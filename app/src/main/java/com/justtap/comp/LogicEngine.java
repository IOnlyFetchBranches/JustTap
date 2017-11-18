package com.justtap.comp;


import android.app.Activity;
import android.content.Context;
import android.renderscript.RSInvalidStateException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;

import static com.justtap.utl.Printers.logGlobal;
import static com.justtap.utl.Printers.logLevel;

public class LogicEngine {




    //MessageQueue
    private static LinkedBlockingQueue<String> messageQueue=new LinkedBlockingQueue<String>();


    //Singleton + core components
    private  static LogicEngine instance;
    private static GraphicsHandler graphics;

    //Our Loop components
    private static  ExecutorService corePool = Executors.newFixedThreadPool(3); //3 is a good number.
    private static Timer fpsTimer;
    static{
        fpsTimer=new Timer(true);
    }




    //Variables
    private static long frames = 0; //fps
    private static long cycles=0; //How many seconds has the program been running


    //Control
    private static boolean isPaused=false;


    //Game settings (Game constants are capitaized for better distiction
    private static String DIFF_LEVEL="EASY"; //All diff level {Easy, Medium, High}

    //END GLOBALS


    //ONLY way we work with the Logic Core
    public static LogicEngine getInstance(Context context) {
        if (instance == null) {
            instance = new LogicEngine(context);

            return instance;
        } else {
            return instance;
        }
    }

    //IMPORTANT <----GAMELOOP IS HERE---->
    public void newContextGameLoop(Context context){
        final Context callingActivityContext=context;

        //init our loop
        corePool.execute(new Runnable() {
            @Override
            public void run() {

                //Start the fps reset timer
                fpsTimer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        logGlobal("FPS => " + frames +" CYCLE => "+cycles);

                        cycles++;
                        if(cycles %10 == 0){
                            logGlobal("10-SEC AVG => "+ frames/10);
                        }

                        frames=0;
                    }
                },0,1000);

                logGlobal("Init'd Timer");

                //End fps timer loop (May convert to more modular runnable later!


                //This is the basis of our Logic Loop
                while (true ) {

                    serve(); //Check messages

                    //If the game screen is empty, order a warp()
                    if(GraphicsHandler.isGamespaceEmpty){
                        graphics.order("Warp-Easy");
                    }


                    //Push graphics engine cycle
                    graphics.update(callingActivityContext); //Makes calls to the UI thread depending on the current frame state

                    //End of game loop
                    tick(); //Push frame
                }



            }
        });
    }
//Our Base constructor, initiates the game loop, called with the activity screen's context (Don't worry it's trashed when the loop is done!)
    private LogicEngine(final Context callingActivityContext) {
        //Link Graphics
        linkGraphics(GraphicsHandler.getInstance(this));

        //Start game loop
        newContextGameLoop(callingActivityContext);


    }


    //Game Methods

    static void CalculateScore(long time){
        /*This method will calculate the score based on how quickly the time is (in ms)
         * It Must also be able to track the average and adjust its sensitivity based updon the
         * current difficulty level
         *
         * For now it simply only needs to announce that a warp was popped
         *
        */

        logGlobal("WARP POP: TIME => " + time);
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




    //End Game methods







    //Flow methods, mainly unused
    public void order(String s) {

    }

    //Check queue
    public static void serve(){
        if(!messageQueue.isEmpty()){
            while(!messageQueue.isEmpty()){
                String message= messageQueue.poll();

                switch(message){

                    default:
                        logLevel("Invalid messaage passed to logic-engine -> "+ message, Level.WARNING);
                }
            }
        }
    }

    //Push frames
    private void tick(){
       frames++;
    }


    //

    //Getters/Setters
    private void linkGraphics(GraphicsHandler handler ){
        if(graphics ==null){
            graphics=handler;
        }
    }


}




