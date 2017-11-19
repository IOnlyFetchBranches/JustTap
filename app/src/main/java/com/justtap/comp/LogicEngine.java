package com.justtap.comp;


import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;

import static android.graphics.Color.BLACK;
import static android.graphics.Color.BLUE;
import static com.justtap.utl.Printers.logGlobal;
import static com.justtap.utl.Printers.logLevel;

/*
 * This is the Logic Background for the Game it runs in two modes
 * GameLoop and MenuLoop
 * GameLoop needs to be called with the context of the activity that it belongs to.
 * There are also several predefined STATIC methods to be used for preferences that can be cahnged without calling the getInstance()
 * This helps free up resources and prevent having deadlocks off of commonly used functions
 * Right now the core runs on a Thread pool of 3 threads, should only need two at a time, eventually get instance will be able to be called with
 * a MODE handle, to tell it which loop to spin up, again context matters!
 */

public class LogicEngine {




    //MessageQueue
    private static LinkedBlockingQueue<String> messageQueue=new LinkedBlockingQueue<String>();


    //Singleton + core components
    private  static LogicEngine instance;
    private static GraphicsHandler graphics;
    private static SoundHandler player;

    //Our Loop components
    private static  ExecutorService corePool = Executors.newFixedThreadPool(3); //3 is a good number.
    private static Timer fpsTimer;
    //Variables
    private static long frames = 0; //fps
    private static long cycles=0; //How many seconds has the program been running
    //Is the game paused?
    private static boolean isPaused = false;
    //Game settings (Game constants are capitalized for better distinction
    private static String DIFF_LEVEL = "EASY"; //All diff level {EASY,MEDIUM,HARD}
    //Define the Global game colors;
    private static int[] colorScheme = {BLACK, BLUE}; //{TEXT_COLOR,WARP_COLOR)
    //The amount of time in seconds that the player has left
    private static long roundTime = 30;
    //The current Time
    private static long time;
    //Hold the timer to prevent race conditions when directly modifying the time
    private static boolean timeModding = false;
    //The amount of warps popped
    private static long popCount = 0; //If surpasses set numbers, difficulty will change

    //END GLOBALS
    //Average pop time of the user, will also fluctuate difficulty
    private static double avgPopTime = 0.0;
    //total time user has been popping warps
    private static double totalPopTime = 0.0;
    //Fastest pop time
    private static double minPopTime = 99999.0;
    //Slowest pop time
    private static double maxPopTime = 0;
    //Of course the score
    private static long score = 0;

    static {
        fpsTimer = new Timer(true);
    }

    private boolean halt = false; //This serves as an interrupt.
    private Mode state = Mode.IDLE; //This is the current states of the Logic engine, and therefore the game.

    //Our Base constructor, initiates the game loop, called with the activity screen's context (Don't worry it's trashed when the loop is done!)
    private LogicEngine(final Context callingActivityContext) {
        //Link Graphics
        linkGraphics(GraphicsHandler.getInstance(this));

        //Start game loop
        newContextGameLoop(callingActivityContext);


    }

    //ONLY way we work with the Logic Core
    public static LogicEngine getInstance(Context context) {
        if (instance == null) {
            instance = new LogicEngine(context);

            return instance;
        } else {
            return instance;
        }
    }

    //Calculates score and requests score update
    //Reminder that current Types are NORM,BLKHOLE,WRMHOLE
    static void CalculateScore(long time, String type) {
        /*This method will calculate the score based on how quickly the time is (in ms)
         * It Must also be able to track the average and adjust its sensitivity based updon the
         * current difficulty level
         *
         * For now it simply only needs to announce that a warp was popped
         *
        */

        //Update pops
        popCount++;

        //Update the total pop time
        totalPopTime += time;
        //Update average pop time
        avgPopTime = totalPopTime / popCount;

        //Calculate maxima
        if (time < minPopTime) minPopTime = time;
        if (time > maxPopTime) maxPopTime = time;



        /*
         * For score updating we need to take into account how LONG the user ahs been tapping
         * lower averages at high popcounts need to be awarded more than lower averages at lower pop counts
         * and so forth....
         *
         * For now until i can determine the fastest and slowest each one with give ten points
         */

        if (popCount <= 10) {
            //Of course we would also take into consideration time
            //Again that comes later for now get a working concept
            //Remove this when maxes are collected;
            score += 10;
        } else if (popCount > 10 && popCount <= 30) {
            score += 10;
        } else if (popCount > 30 && popCount <= 50) {
            score += 10;
        } else {
            score += 10;
        }

        Log.i("Logic Engine =>", " Current Fastest/Slowest Times + average " +
                minPopTime + "/" + maxPopTime + " " + avgPopTime);

        //Graphics requests
        graphics.order("Logic-UpdateScore");
        graphics.order("Logic-UpdateStats");


        //If a bad type is popped, process the consequences here
        if (type.equals("BLKHOLE")) {
            switch (getDiffLevel()) {
                case "EASY":
                    setTime(time - Punishment.EASY.value);
                    break;
                case "MEDIUM":
                    setTime(time - Punishment.MEDIUM.value);
                    break;
                case "HARD":
                    setTime(time - Punishment.HARD.value);
                    break;
                default:
                    //Should hopefully never reach here but just incase
                    throw new IllegalArgumentException("Invalid diff level provided!");
            }
        }


    }

    public static long getTime() {
        return time;
    }

    //Time getters/setters
    //Note: we do not HAVE to call order("Logic-UpdateTime") as the timer thread does this automatically)
    static void setTime(@NonNull long newTime) {
        pauseTimer(); //Pause timer
        time = newTime; //Update Timer
        graphics.order("Logic-UpdateTime");
        if (time < 0) {
            time = 0; //We don't want negative time lol
        }
        unpauseTimer(); //Un-Pause Timer

    }

    //Get popCount
    public static long getPopCount() {
        return popCount;
    }

    //Returns the user's current average pop time
    public static double getAvgPopTime() {
        return avgPopTime;
    }

    static long getScore() {
        return score;
    }

    public static String getDiffLevel(){
        return DIFF_LEVEL;
    }

    //When calling this, use all caps for level
    static void setDiffLevel(String LEVEL){
        switch(LEVEL){
            case "EASY":
                DIFF_LEVEL="EASY";
                break;
            case "MEDIUM":
                DIFF_LEVEL="MEDIUM";
                break;
            case "HARD":
                DIFF_LEVEL="HARD";
                break;
            default:
                throw new IllegalArgumentException("Choices EASY/MEDIUM/HARD <= SET_DIFF");


        }
    }

    //Check queue
    public static void serve() {
        if (!messageQueue.isEmpty()) {
            while (!messageQueue.isEmpty()) {
                String message = messageQueue.poll();

                switch (message) {

                    default:
                        logLevel("Invalid messaage passed to logic-engine -> " + message, Level.WARNING);
                }
            }
        }
    }

    //Returns an ARRAY of Color enum int values {TEXT COLOR, WARP COLOR}
    static int[] getColorScheme(boolean getDefault) {
        if (getDefault) {
            return new int[]{BLACK, BLUE};
        } else {
            return colorScheme;
        }
    }

    //Set the color scheme, Should ONLY BE USED IN OPTIONS MENU!
    //Format for proper input is x[] {TEXT COLOR, WARP COLOR}
    public static void setColorScheme(@NonNull int[] newScheme) {
        if (newScheme.length == 2)
            colorScheme = newScheme;
        else {
            throw new IllegalArgumentException("Invalid Color Scheme, Input => {Color.color, Color.color}");
        }
    }

    //Package private, aux methods used for time coordination with other components
    static void pauseTimer() {
        timeModding = true;
    }

    static void unpauseTimer() {
        timeModding = false;
    }






    //End Game methods

    //IMPORTANT <----GAMELOOP IS HERE---->
    public void newContextGameLoop(Context context) {
        final Context callingActivityContext = context;


        //init our loop
        corePool.execute(new Runnable() {
            @Override
            public void run() {


                //Start the fps reset timer
                fpsTimer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        if (!halt) {
                            logGlobal("FPS => " + frames + " CYCLE => " + cycles);

                            cycles++;
                            if (cycles % 10 == 0) {
                                logGlobal("10-SEC AVG => " + frames / 10);
                            }

                            frames = 0;
                        }
                    }
                }, 0, 1000);
                //End fps timer loop (May convert to more modular runnable later


                //This is the basis of our Logic Loop
                while (!halt) {

                    serve(); //Check messages

                    //If the game screen is empty, order a warp()
                    if (GraphicsHandler.isGamespaceEmpty) {
                        graphics.order("Warp-" + getDiffLevel());
                        Log.i("Logic Engine=>", "Order placed");
                    }


                    //Push graphics engine cycle
                    graphics.update(callingActivityContext); //Makes calls to the UI thread depending on the current frame state

                    //End of game loop
                    tick(); //Push frame

                    if (isPaused) {
                        //Go to pause loop
                    }


                }


            }
        });
    }

    //PAUSE LOOP
    public void pauseLoop(Context context) {
        //Empty for now, But will reveal the opacity and reenable control fo the menu
    }

    //Start the game timer;
    private void startTimer() {
        time = roundTime;

        Timer gameTimer = new Timer(true);
        gameTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                while (isPaused || timeModding) {
                    //Halt while paused
                }
                //Update the timer
                time--;
                //Update ui
                graphics.order("Logic-UpdateTime");
                if (time == 0) {
                    this.cancel();
                    //gameover
                }
            }
        }, 500, 1000);
    }

    //Flow methods, mainly unused
    public void order(String s) {

    }

    //Push frames
    private void tick(){
       frames++;
    }

    //Link Graphics engine (Should only be done by Engine. Try to avoid manually calling this
    private void linkGraphics(GraphicsHandler handler ){
        if(graphics ==null){
            graphics=handler;
        }
    }

    //Don't need an unpause game, the pause loop will have an out.
    //Preferably triggered by the resume game button;
    public void pauseGame(Context callingActivityContext) {
        isPaused = true;
        pauseLoop(callingActivityContext);

    }

    //Core Control (Mode set, Loop control)
    public enum Mode {
        GAME,
        MENU,
        IDLE,
        ERROR

    }

    //Time punishment constants
    // Why Here? Well because It needs to be easy for me to tweak lol
    private enum Punishment {
        EASY(5),
        MEDIUM(7),
        HARD(10);

        private int value;

        Punishment(int value) {
            this.value = value;
        }

        public int value() {
            return value;
        }
    }




}




