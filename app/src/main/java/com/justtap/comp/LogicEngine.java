package com.justtap.comp;


import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Rect;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.justtap.R;

import java.io.InvalidObjectException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

import static android.graphics.Color.BLACK;
import static android.graphics.Color.BLUE;
import static com.justtap.comp.LogicEngine.Level.ADVANCED;
import static com.justtap.comp.LogicEngine.Level.BEGINNER;
import static com.justtap.comp.LogicEngine.Level.INTERMEDIATE;
import static com.justtap.comp.LogicEngine.Level.INTRO;
import static com.justtap.comp.LogicEngine.Level.MASTER;
import static com.justtap.comp.LogicEngine.Mode.AFTERTIME;
import static com.justtap.comp.LogicEngine.Mode.BUSY;
import static com.justtap.comp.LogicEngine.Mode.DONE;
import static com.justtap.comp.LogicEngine.Mode.GAME;
import static com.justtap.comp.LogicEngine.Mode.GAMEOVER;
import static com.justtap.comp.LogicEngine.Mode.IDLE;
import static com.justtap.comp.LogicEngine.Mode.PAUSED;
import static com.justtap.comp.LogicEngine.Mode.PAUSING;
import static com.justtap.comp.LogicEngine.Mode.RESUMING;
import static com.justtap.comp.LogicEngine.Type.BLACKHOLE;
import static com.justtap.comp.LogicEngine.Type.NORMAL;
import static com.justtap.utl.Numbers.genInt;
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
    private static LogicEngine instance; //The singleton
    private static GraphicsHandler graphics; //The graphics handler
    private static SoundHandler sound; //The sound handler

    //Our Loop components
    private static  ExecutorService corePool = Executors.newFixedThreadPool(3); //3 is a good number.
    private static Timer fpsTimer;
    private static Mode state = Mode.IDLE; //This is the current states of the Logic engine, and therefore the game.
    //Variables
    private static long frames = 0; //fps
    private static long cycles=0; //How many seconds has the program been running
    //Is the game paused?
    private static boolean isPaused = false;
    //Game settings (Game constants are capitalized for better distinction
    //Will eventually control how quickly you advance through levels
    //For now its challenging enough for it to dictate punishment for missing and such
    private static Difficulties DIFF_LEVEL = Difficulties.INTER; //All diff level {EASY,INTER,HARD}
    //NOT DIFF LEVEL! This sets the current level of PROGRESSION in a Level
    private static Level level; // The current level
    //If true, the user will get punished for missed taps as well
    private static boolean perfectMode = true; //True by default for now
    //Define the Global game colors;
    private static int[] colorScheme = {BLACK, BLUE}; //{TEXT_COLOR,WARP_COLOR)
    //The amount of time in seconds that the player has left
    private static long roundTime = 30;
    //The current Time
    private static Long time;
    //Hold the timer to prevent race conditions when directly modifying the time
    private static boolean timeModding = false;
    //The amount of warps popped
    private static long popCount = 0; //If surpasses set numbers, difficulty will change
    //Average pop time of the user, will also fluctuate difficulty
    private static double avgPopTime = 0.0;
    //total time user has been popping warps
    private static double totalPopTime = 0.0;
    //Fastest pop time
    private static double minPopTime = 99999.0;
    //Slowest pop time
    private static double maxPopTime = 0;
    //Of course the score, keep this static
    private static long score = 0;
    /**
     * @param time
     * @param type
     */

    private static double oldAvgPopTime = 0;

    static {
        fpsTimer = new Timer(true);
    }

    private Timer gameTimer = new Timer(true);
    //Tracking for future use, for graphics likely
    private long userTouchCount = 0;
    private float userTouchX = 0f;
    private float userTouchY = 0f;
    //Control
    private boolean halt = false; //This serves as an interrupt.
    private boolean isGameOver = false; //Is the game ended
    //End Control
    /**
     * Break LogicEngine Global Parameter debug Mode before final prod release
     * This is to prevent memory tampering by some clever reverse engineering; At least make it a bti harder?!
     */
    private boolean debugMode = true; //Triggers special debug state.
    private boolean finishedReset = true;

    /**
     * @param callingActivityContext
     */
    //Our Base constructor, initiates the game loop, called with the activity screen's context (Don't worry it's trashed when the loop is done!)
    private LogicEngine(final Context callingActivityContext, final Mode mode) {
        //Link Graphics
        linkGraphics(GraphicsHandler.getInstance(this));

        if (mode == Mode.GAME) {
            //Start game loop
            newContextGameLoop(callingActivityContext);
        }


    }

    //ONLY way we work with the Logic Core
    public static LogicEngine getInstance(Context context, Mode mode) {
        if (instance == null) {
            instance = new LogicEngine(context, mode);

            return instance;
        } else {
            return instance;
        }
    }

    //Calculates score and requests score update
    //Reminder that current Types are NORM,BLKHOLE,WRMHOLE
    @SuppressWarnings("UnnecessaryLocalVariable")
    @SuppressLint("SetTextI18n")
    static void CalculateScore(long popTime, Warp warp, RelativeLayout.LayoutParams popLocation, Context context) {
        /*This method will calculate the score based on how quickly the popTime is (in ms)
         * It Must also be able to track the average and adjust its sensitivity based updon the
         * current difficulty level
         *
         *
        */


        //If first move, start game
        if (popCount == 0) {
            state = Mode.GAME;
            Log.i("Logic Engine=>", " User tapped a warp!");
            unpauseTimer();

        }

        //Update pops
        popCount++;

        //Update the total pop popTime
        totalPopTime += popTime;
        //Update average pop popTime
        oldAvgPopTime = avgPopTime;
        avgPopTime = totalPopTime / popCount;

        //Calculate maxima (Can be used as padding)
        //The faster your fastest the harder the game can be
        if (popTime < minPopTime) minPopTime = popTime;
        if (popTime > maxPopTime) maxPopTime = popTime;


        //Keep the bias in-between .01 and .02 percent, the higher that is the more likely the player is to be within average!
        boolean AROUND_AVERAGE = avgPopTime > (oldAvgPopTime - (oldAvgPopTime * .01)) && avgPopTime < (oldAvgPopTime + (oldAvgPopTime * .01));

        //Adjustment
        if (AROUND_AVERAGE) {
            //you can lower avg to speed up game to make it
            //super hard
            Log.i("LOGIC =>", "User is doing average");
        } else {
            Log.i("LOGIC =>", "User is above/below average");
        }


        //Label That pops up for user, saying how good they did
        final TextView qualityLabel = new TextView(context);


        //Fade In animation for Float Text
        //Label Animation
        AlphaAnimation fadeInDissolve = new AlphaAnimation(0, 1);
        fadeInDissolve.setDuration(400);

        //Tweak the animation here by fading it out onEnd() and translating it onStart()
        fadeInDissolve.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                qualityLabel.animate().translationXBy(40).setDuration(500).setInterpolator(new AccelerateDecelerateInterpolator()).start();
                qualityLabel.animate().translationYBy(-40).setDuration(500).setInterpolator(new AccelerateDecelerateInterpolator()).start();
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                qualityLabel.animate().alpha(0f).setDuration(400).setInterpolator(new DecelerateInterpolator()).start();
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });

        //We must reference our gamescreen here;
        RelativeLayout gameScreen = (RelativeLayout) ((Activity) context).findViewById(R.id.GAME__Area);



        /*
         * For score updating we need to take into account how LONG the user ahs been tapping
         * lower averages at high popcounts need to be awarded more than lower averages at lower pop counts
         * and so forth....
         *
         * For now until i can determine the fastest and slowest each one with give ten points
         */
        //Define Ranges
        boolean INTRO_LEVEL = popCount <= 5; //User is warming up
        boolean BEGINNER_LEVEL = popCount > 5 && popCount <= 20; //Early Stages
        boolean INTERMEDIATE_LEVEL = popCount > 20 && popCount <= 45; //Next range, 100 may be a little too high
        boolean ADVANCED_LEVEL = popCount > 45 && popCount <= 100; // This is where you start getting popTime for excellents
        boolean MASTER_LEVEL = popCount > 100 && popCount <= 200; // User is a master, this is quite a feat!

        //Define conditions, Percentages in relation to avgpoptime that define what you have to beat.
        boolean EXCEL_CONDITION = popTime <= avgPopTime - (avgPopTime * .20); //player is 20% better than average
        boolean GREAT_CONDITION = popTime <= avgPopTime - (avgPopTime * .10); //player is 10% better than average
        boolean GOOD_CONDITION = popTime <= avgPopTime + (avgPopTime * .10); //player is within 10% of average popTime


        //NAME SCHEME -> LEVEL_TIME_ADJUSTMENT_CONDITION;
        //Define Bias to be added as the player beats their average popTime. in Percentages;
        final double BEG_TIME_BIAS_ADJ_EXCEL = .0050;
        final double BEGR_TIME_BIAS_ADJ_GREAT = .0030;
        final double INTER_TIME_BIAS_ADJ_EXCEL = .0015;
        final double INTER_TIME_BIAS_ADJ_GREAT = .0015;
        final double ADV_TIME_BIAS_ADJ_EXCEL = .0010;
        final double ADV_TIME_BIAS_ADJ_GREAT = .0010;
        final double MAST_TIME_BIAS_ADJ_EXCEL = .0010;
        final double MAST_TIME_BIAS_ADJ_GREAT = .0005;


        //RNG
        int chance = genInt(0, 100);

        //Check warp type;
        Type type = warp.getType();


        //BEGIN SCORING LOGIC HERE
        //The labels are also styled here, if that's what you;re looking for!

        //If type is normal. [Long]
        if (type == NORMAL) {
            if (INTRO_LEVEL) {
                if (level != INTRO) {
                    level = INTRO;
                    //graphics.order("Logic-UpdateLevel");
                }
                //Of course we would also take into consideration popTime
                //Label
                qualityLabel.setText(context.getResources().getString(R.string.label_score_getready) + " +" + 5);
                qualityLabel.setTextColor(Color.BLACK);

                popLocation.topMargin = popLocation.topMargin + 100;
                popLocation.leftMargin = popLocation.leftMargin + 50;
                popLocation.width = ViewGroup.LayoutParams.WRAP_CONTENT;
                popLocation.height = ViewGroup.LayoutParams.WRAP_CONTENT;

                gameScreen.addView(qualityLabel, popLocation);
                qualityLabel.startAnimation(fadeInDissolve);

                score += 10;
            } else if (BEGINNER_LEVEL) {
                if (level != BEGINNER) {
                    level = BEGINNER;
                    graphics.order("Logic-UpdateLevel");
                }

                if (EXCEL_CONDITION) {
                    score += 10;
                    //Also add popTime ?

                    //We only want this to happen 2-4 times randomly
                    if (chance % genInt(2, 4) == 0) {
                        //add popTime
                        setTime(getTime() + 1, false);
                        qualityLabel.setText(context.getResources().getString(R.string.bonustimegreat) + " +" + 1 + " Sec!");
                        avgPopTime -= avgPopTime * BEG_TIME_BIAS_ADJ_EXCEL;
                    } else {
                        qualityLabel.setText(context.getResources().getString(R.string.label_score_excellent) + " +" + 10);
                    }

                    //Label
                    qualityLabel.setTextColor(Color.GREEN);

                    popLocation.topMargin = popLocation.topMargin + 100;
                    popLocation.leftMargin = popLocation.leftMargin + 50;
                    popLocation.width = ViewGroup.LayoutParams.WRAP_CONTENT;
                    popLocation.height = ViewGroup.LayoutParams.WRAP_CONTENT;

                    gameScreen.addView(qualityLabel, popLocation);
                    qualityLabel.startAnimation(fadeInDissolve);


                } else if (GREAT_CONDITION) {
                    score += 7;

                    //We only want this to happen 2-4 times randomly
                    if (chance % genInt(2, 4) == 0) {
                        //add popTime
                        setTime(getTime() + 1, false);
                        qualityLabel.setText(context.getResources().getString(R.string.bonustimegreat) + " +" + 1 + " Sec!");
                        avgPopTime -= avgPopTime * BEGR_TIME_BIAS_ADJ_GREAT;
                    } else {
                        qualityLabel.setText(context.getResources().getString(R.string.label_score_great) + " +" + 7);
                    }



                    //Label

                    qualityLabel.setTextColor(Color.BLUE);

                    popLocation.topMargin = popLocation.topMargin + 100;
                    popLocation.leftMargin = popLocation.leftMargin + 50;
                    popLocation.width = ViewGroup.LayoutParams.WRAP_CONTENT;
                    popLocation.height = ViewGroup.LayoutParams.WRAP_CONTENT;


                    gameScreen.addView(qualityLabel, popLocation);
                    qualityLabel.startAnimation(fadeInDissolve);

                } else if (GOOD_CONDITION) {
                    //Label
                    qualityLabel.setText(context.getResources().getString(R.string.label_score_good) + " +" + 5);
                    qualityLabel.setTextColor(Color.BLACK);

                    popLocation.topMargin = popLocation.topMargin + 100;
                    popLocation.leftMargin = popLocation.leftMargin + 50;
                    popLocation.width = ViewGroup.LayoutParams.WRAP_CONTENT;
                    popLocation.height = ViewGroup.LayoutParams.WRAP_CONTENT;


                    gameScreen.addView(qualityLabel, popLocation);
                    qualityLabel.startAnimation(fadeInDissolve);
                    score += 5;
                } else {
                    //Label
                    qualityLabel.setText(context.getResources().getString(R.string.label_score_okay) + " +" + 3);
                    qualityLabel.setTextColor(Color.RED);

                    popLocation.topMargin = popLocation.topMargin + 100;
                    popLocation.leftMargin = popLocation.leftMargin + 50;
                    popLocation.width = ViewGroup.LayoutParams.WRAP_CONTENT;
                    popLocation.height = ViewGroup.LayoutParams.WRAP_CONTENT;

                    gameScreen.addView(qualityLabel, popLocation);
                    qualityLabel.startAnimation(fadeInDissolve);
                    score += 3;
                }


            } else if (INTERMEDIATE_LEVEL) {

                if (level != INTERMEDIATE) {
                    level = INTERMEDIATE;
                    graphics.order("Logic-UpdateLevel");
                }

                if (EXCEL_CONDITION) {
                    score += 15;
                    //Also add popTime ?

                    //We only want this to happen around third the popTime
                    if (chance % 3 == 0) {
                        //add popTime
                        setTime(getTime() + 2, false);
                        qualityLabel.setText(context.getResources().getString(R.string.bonustimegreat) + " +" + 2 + " Secs!");
                        //make it slightly harder
                        avgPopTime -= avgPopTime * INTER_TIME_BIAS_ADJ_EXCEL;

                    } else {
                        qualityLabel.setText(context.getResources().getString(R.string.label_score_excellent) + " +" + 15);
                    }

                    //Label
                    qualityLabel.setTextColor(Color.GREEN);

                    RelativeLayout.LayoutParams topCorner = popLocation;
                    topCorner.topMargin = topCorner.topMargin + 100;
                    topCorner.leftMargin = topCorner.leftMargin + 50;
                    topCorner.width = ViewGroup.LayoutParams.WRAP_CONTENT;
                    topCorner.height = ViewGroup.LayoutParams.WRAP_CONTENT;

                    gameScreen.addView(qualityLabel, topCorner);
                    qualityLabel.startAnimation(fadeInDissolve);


                } else if (GREAT_CONDITION) {
                    //Also add popTime ?

                    //We only want this to happen around a third the popTime
                    if (chance % 3 == 0) {
                        //add popTime
                        setTime(getTime() + 1, false);
                        qualityLabel.setText(context.getResources().getString(R.string.bonustimegreat) + " +" + 1 + " Sec!");
                        //make it slightly harder
                        avgPopTime -= avgPopTime * INTER_TIME_BIAS_ADJ_GREAT;

                    } else {
                        qualityLabel.setText(context.getResources().getString(R.string.label_score_great) + " +" + 12);
                    }



                    //Label
                    qualityLabel.setTextColor(Color.BLUE);

                    RelativeLayout.LayoutParams topCorner = popLocation;
                    topCorner.topMargin = topCorner.topMargin + 100;
                    topCorner.leftMargin = topCorner.leftMargin + 50;
                    topCorner.width = ViewGroup.LayoutParams.WRAP_CONTENT;
                    topCorner.height = ViewGroup.LayoutParams.WRAP_CONTENT;

                    gameScreen.addView(qualityLabel, topCorner);
                    qualityLabel.startAnimation(fadeInDissolve);
                    score += 12;
                } else if (GOOD_CONDITION) {
                    //Label
                    qualityLabel.setText(context.getResources().getString(R.string.label_score_good) + " +" + 10);
                    qualityLabel.setTextColor(Color.BLACK);

                    RelativeLayout.LayoutParams topCorner = popLocation;
                    topCorner.topMargin = topCorner.topMargin + 100;
                    topCorner.leftMargin = topCorner.leftMargin + 50;
                    topCorner.width = ViewGroup.LayoutParams.WRAP_CONTENT;
                    topCorner.height = ViewGroup.LayoutParams.WRAP_CONTENT;

                    gameScreen.addView(qualityLabel, topCorner);
                    qualityLabel.startAnimation(fadeInDissolve);
                    score += 10;

                } else {
                    //Label
                    qualityLabel.setText(context.getResources().getString(R.string.label_score_okay) + " +" + 5);
                    qualityLabel.setTextColor(Color.RED);

                    RelativeLayout.LayoutParams topCorner = popLocation;
                    topCorner.topMargin = topCorner.topMargin + 100;
                    topCorner.leftMargin = topCorner.leftMargin + 50;
                    topCorner.width = ViewGroup.LayoutParams.WRAP_CONTENT;
                    topCorner.height = ViewGroup.LayoutParams.WRAP_CONTENT;

                    gameScreen.addView(qualityLabel, topCorner);
                    qualityLabel.startAnimation(fadeInDissolve);
                    score += 5;
                }
            } else if (ADVANCED_LEVEL) {

                if (level != ADVANCED) {
                    level = ADVANCED;
                    graphics.order("Logic-UpdateLevel");
                }

                if (EXCEL_CONDITION) {

                    //Also add popTime ?

                    //We only want this to happen around half the popTime
                    if (chance % 2 == 0) {
                        //add popTime
                        setTime(getTime() + 2, false);
                        qualityLabel.setText(context.getResources().getString(R.string.bonustimegreat) + " +" + 2 + " Secs!");
                        //make it slightly harder
                        avgPopTime -= avgPopTime * ADV_TIME_BIAS_ADJ_EXCEL;

                    } else {
                        qualityLabel.setText(context.getResources().getString(R.string.label_score_excellent) + " +" + 20);
                    }

                    //Label
                    qualityLabel.setTextColor(Color.GREEN);

                    popLocation.topMargin = popLocation.topMargin + 100;
                    popLocation.leftMargin = popLocation.leftMargin + 50;
                    popLocation.width = ViewGroup.LayoutParams.WRAP_CONTENT;
                    popLocation.height = ViewGroup.LayoutParams.WRAP_CONTENT;

                    gameScreen.addView(qualityLabel, popLocation);
                    qualityLabel.startAnimation(fadeInDissolve);

                    score += 20;

                } else if (GREAT_CONDITION) {

                    //We only want this to happen around a third of the popTime
                    if (chance % 3 == 0) {
                        //add popTime
                        setTime(getTime() + 2, false);
                        qualityLabel.setText(context.getResources().getString(R.string.bonustimegreat) + " +" + 2 + " Secs!");

                        //make it slightly harder
                        avgPopTime -= avgPopTime * ADV_TIME_BIAS_ADJ_GREAT;

                    } else {
                        qualityLabel.setText(context.getResources().getString(R.string.label_score_great) + " +" + 15);
                    }

                    //Label

                    qualityLabel.setTextColor(Color.BLUE);

                    RelativeLayout.LayoutParams topCorner = popLocation;
                    topCorner.topMargin = topCorner.topMargin + 100;
                    topCorner.leftMargin = topCorner.leftMargin + 50;
                    topCorner.width = ViewGroup.LayoutParams.WRAP_CONTENT;
                    topCorner.height = ViewGroup.LayoutParams.WRAP_CONTENT;

                    gameScreen.addView(qualityLabel, topCorner);
                    qualityLabel.startAnimation(fadeInDissolve);

                    score += 15;
                } else if (GOOD_CONDITION) {

                    //Label
                    qualityLabel.setText(context.getResources().getString(R.string.label_score_good) + " +" + 10);
                    qualityLabel.setTextColor(Color.BLACK);

                    RelativeLayout.LayoutParams topCorner = popLocation;
                    topCorner.topMargin = topCorner.topMargin + 100;
                    topCorner.leftMargin = topCorner.leftMargin + 50;
                    topCorner.width = ViewGroup.LayoutParams.WRAP_CONTENT;
                    topCorner.height = ViewGroup.LayoutParams.WRAP_CONTENT;

                    gameScreen.addView(qualityLabel, topCorner);
                    qualityLabel.startAnimation(fadeInDissolve);
                    score += 10;
                } else {

                    //Dynamic loss popTime
                    int loss = DIFF_LEVEL == Difficulties.EASY ? 3 :
                            DIFF_LEVEL == Difficulties.INTER ? 4 :
                                    DIFF_LEVEL == Difficulties.HARD ? 5 : 7;

                    //Label
                    qualityLabel.setText(context.getResources().getString(R.string.label_minustime) + " -" + loss + " Secs!");
                    setTime(getTime() - loss, false);
                    qualityLabel.setTextColor(Color.RED);

                    RelativeLayout.LayoutParams topCorner = popLocation;
                    topCorner.topMargin = topCorner.topMargin + 100;
                    topCorner.leftMargin = topCorner.leftMargin + 50;

                    topCorner.width = ViewGroup.LayoutParams.WRAP_CONTENT;
                    topCorner.height = ViewGroup.LayoutParams.WRAP_CONTENT;

                    gameScreen.addView(qualityLabel, topCorner);
                    qualityLabel.startAnimation(fadeInDissolve);
                    score += 7;
                }
            } else if (MASTER_LEVEL) {
                if (level != MASTER) {
                    level = MASTER;
                    graphics.order("Logic-UpdateLevel");
                }


                if (EXCEL_CONDITION) {


                    //We only want this to happen around half the popTime
                    if (chance % 2 == 0) {
                        //add popTime
                        setTime(getTime() + 4, false);
                        qualityLabel.setText(context.getResources().getString(R.string.bonustimegreat) + " +" + 4 + " Secs!");
                        //make it slightly harder
                        avgPopTime -= avgPopTime * MAST_TIME_BIAS_ADJ_EXCEL;

                    } else {
                        qualityLabel.setText(context.getResources().getString(R.string.label_score_excellent) + " +" + 30);
                        //make it slightly harder
                        avgPopTime -= avgPopTime * MAST_TIME_BIAS_ADJ_GREAT;

                    }

                    //Label
                    qualityLabel.setText(context.getResources().getString(R.string.label_score_excellent) + " +" + 30);
                    qualityLabel.setTextColor(Color.GREEN);

                    RelativeLayout.LayoutParams topCorner = popLocation;
                    topCorner.topMargin = topCorner.topMargin + 100;
                    topCorner.leftMargin = topCorner.leftMargin + 50;
                    topCorner.width = ViewGroup.LayoutParams.WRAP_CONTENT;
                    topCorner.height = ViewGroup.LayoutParams.WRAP_CONTENT;

                    gameScreen.addView(qualityLabel, topCorner);
                    qualityLabel.startAnimation(fadeInDissolve);

                    score += 30;

                } else if (GREAT_CONDITION) {

                    //We only want this to happen around half the popTime
                    if (chance % 2 == 0) {
                        //add popTime
                        setTime(getTime() + 2, false);
                        qualityLabel.setText(context.getResources().getString(R.string.bonustimegreat) + " +" + 2 + " Secs!");
                        //make it slightly harder
                        avgPopTime -= avgPopTime * MAST_TIME_BIAS_ADJ_GREAT;

                    } else {
                        qualityLabel.setText(context.getResources().getString(R.string.label_score_great) + " +" + 20);
                        //make it slightly harder
                        avgPopTime -= avgPopTime * MAST_TIME_BIAS_ADJ_GREAT;

                    }

                    //Label

                    qualityLabel.setTextColor(Color.BLUE);

                    RelativeLayout.LayoutParams topCorner = popLocation;
                    topCorner.topMargin = topCorner.topMargin + 100;
                    topCorner.leftMargin = topCorner.leftMargin + 50;
                    topCorner.width = ViewGroup.LayoutParams.WRAP_CONTENT;
                    topCorner.height = ViewGroup.LayoutParams.WRAP_CONTENT;

                    gameScreen.addView(qualityLabel, topCorner);
                    qualityLabel.startAnimation(fadeInDissolve);

                    score += 20;
                } else if (GOOD_CONDITION) {

                    //Label
                    qualityLabel.setText(context.getResources().getString(R.string.label_score_good) + " +" + 15);
                    qualityLabel.setTextColor(Color.BLACK);

                    RelativeLayout.LayoutParams topCorner = popLocation;
                    topCorner.topMargin = topCorner.topMargin + 100;
                    topCorner.leftMargin = topCorner.leftMargin + 50;
                    topCorner.width = ViewGroup.LayoutParams.WRAP_CONTENT;
                    topCorner.height = ViewGroup.LayoutParams.WRAP_CONTENT;

                    gameScreen.addView(qualityLabel, topCorner);
                    qualityLabel.startAnimation(fadeInDissolve);
                    score += 15;
                } else {

                    //Dynamic loss popTime
                    int loss = DIFF_LEVEL == Difficulties.EASY ? 5 :
                            DIFF_LEVEL == Difficulties.INTER ? 6 :
                                    DIFF_LEVEL == Difficulties.HARD ? 7 : 10;

                    //Label
                    qualityLabel.setText(context.getResources().getString(R.string.label_minustime) + " -" + loss + " Secs!");
                    setTime(getTime() - loss, false);
                    qualityLabel.setTextColor(Color.RED);

                    RelativeLayout.LayoutParams topCorner = popLocation;
                    topCorner.topMargin = topCorner.topMargin + 100;
                    topCorner.leftMargin = topCorner.leftMargin + 50;

                    topCorner.width = ViewGroup.LayoutParams.WRAP_CONTENT;
                    topCorner.height = ViewGroup.LayoutParams.WRAP_CONTENT;

                    gameScreen.addView(qualityLabel, topCorner);
                    qualityLabel.startAnimation(fadeInDissolve);
                    score += 10;
                }
            }

            Log.i("Logic Engine =>", " Current Fastest/Slowest s + average " +
                    minPopTime + "/" + maxPopTime + " " + avgPopTime);

            //Graphics requests
            graphics.order("Logic-UpdateScore");
            graphics.order("Logic-UpdateStats");
        }


        //What happens for blackholes?
        if (type == Type.BLACKHOLE) {
            if (!warp.isMissed()) {
                switch (getDiffLevel()) {
                    case EASY:
                        setTime(getTime() - Punishment.EASY.value, false);
                        break;
                    case INTER:
                        setTime(getTime() - Punishment.INTER.value, false);
                        break;
                    case HARD:
                        setTime(getTime() - Punishment.HARD.value, false);
                        break;
                    default:
                        //Should hopefully never reach here but just in case
                        throw new IllegalArgumentException("Invalid diff level provided!");
                }
            } else {
                //50 points rewarded for getting rid of black hole;
                score += 50;
            }
        }


    }

    static long getTime() {
        return time;
    }


    //Time getters/setters
    //Note: we do not HAVE to call order("Logic-UpdateTime") as the timer thread does this automatically)
    private static void setTime(long newTime, boolean freezeTime) {
        pauseTimer(); //Pause timer
        time = newTime; //Update Timer
        graphics.order("Logic-UpdateTime");
        if (time < 0) {
            time = 0L; //We don't want negative time lol
        }
        if (!freezeTime)
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

    private static Difficulties getDiffLevel() {
        return DIFF_LEVEL;
    }

    //When calling this, use all caps for level
    static void setDiffLevel(String LEVEL){
        switch(LEVEL){
            case "EASY":
                DIFF_LEVEL = Difficulties.EASY;
                break;
            case "INTER":
                DIFF_LEVEL = Difficulties.INTER;
                break;
            case "HARD":
                DIFF_LEVEL = Difficulties.HARD;
                break;
            default:
                throw new IllegalArgumentException("Choices EASY/INTER/HARD <= SET_DIFF");


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
    private static void pauseTimer() {
        timeModding = true;
    }

    private static void unpauseTimer() {
        timeModding = false;
    }

    static boolean isTimePause() {
        return timeModding;
    }

    //Return current state;
    public static Mode State() {
        return state;
    }

    //Return ser progression
    public static Level getUserProgression() {
        return level;
    }
    //End Game methods

    //Internal Methods

    private static void updateState(Mode newState) {
        //This MAY cause a slight hold on the engine while it readjusts
        state = newState;
    }

    //Check queue
    private void serve(Context context) {


        while (!messageQueue.isEmpty()) {

            for (String m : messageQueue) {
                if (m == null) {
                    messageQueue.remove(m);
                }
            }

            String message = messageQueue.poll();
            {
                if (message == null) {
                    break;
                }
            }


            switch (message) {
                case "Game-Reset":
                    if (state != Mode.BUSY) {
                        Log.i("Logic Engine =>", " Resetting...");
                        reset(context);
                        //noinspection StatementWithEmptyBody
                        while (state != DONE) {
                            //do nothing until it finishs
                        }
                        for (String m : messageQueue) {
                            if (m.contains("Reset")) {
                                messageQueue.remove(m);
                            }
                        }
                        break;
                    }

                default:
                    logLevel("Invalid messaage passed to Logic-Engine -> " + message, java.util.logging.Level.WARNING);
            }
        }

    }


    //LOOP SECTION

    //IMPORTANT <----GAMELOOP IS HERE---->
    private void newContextGameLoop(final Context context) {

        level = INTRO;


        //Connect to the game screen
        final RelativeLayout gameArea = (RelativeLayout) ((Activity) context).findViewById(R.id.GAME__Area);

        if (gameArea == null) {
            throw new IllegalArgumentException("Game Loop cannot use provided context!");
        }


        gameArea.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                //Touch logic
                if (!timeModding) {
                    userTouchCount++;
                    userTouchX = event.getX();
                    userTouchY = event.getY();

                    //Vars
                    Warp warp = null;
                    AlphaAnimation fadeInDissolve = null;


                    //Find the warp
                    for (int x = 0; x < gameArea.getChildCount(); x++) {
                        if (gameArea.getChildAt(x) instanceof Warp) {
                            warp = (Warp) gameArea.getChildAt(x);
                        }
                    }
                    //Safeguard
                    if (warp != null) {
                        Rect hitBox = new Rect();
                        warp.getHitRect(hitBox);
                        //It's a miss then!
                        if (!hitBox.contains((int) userTouchX, (int) userTouchY)) {

                            //Are they playing in the right mode?
                            if (perfectMode && warp.getType() != BLACKHOLE) {
                                //How much are they punished?
                                int loss =
                                        DIFF_LEVEL == Difficulties.EASY ? Punishment.MISS_EASY.value :
                                                DIFF_LEVEL == Difficulties.INTER ? Punishment.MISS_INTER.value :
                                                        DIFF_LEVEL == Difficulties.HARD ? Punishment.MISS_HARD.value : 3;

                                //Declare Label
                                final TextView qualityLabel = new TextView(context);


                                //Fade In animation for Float Text

                                //Where do we gen the animation?
                                final RelativeLayout.LayoutParams missLocation = warp.getPosition();
                                //Label Animation
                                fadeInDissolve = new AlphaAnimation(0, 1);
                                fadeInDissolve.setDuration(400);

                                //Tweak the animation here by fading it out onEnd() and translating it onStart()
                                fadeInDissolve.setAnimationListener(new Animation.AnimationListener() {
                                    @Override
                                    public void onAnimationStart(Animation animation) {
                                        qualityLabel.animate().translationXBy(40).setDuration(500).setInterpolator(new AccelerateDecelerateInterpolator()).start();
                                        qualityLabel.animate().translationYBy(-40).setDuration(500).setInterpolator(new AccelerateDecelerateInterpolator()).start();
                                    }

                                    @Override
                                    public void onAnimationEnd(Animation animation) {
                                        qualityLabel.animate().alpha(0f).setDuration(400).setInterpolator(new DecelerateInterpolator()).start();
                                    }

                                    @Override
                                    public void onAnimationRepeat(Animation animation) {

                                    }
                                });


                                //Design Label
                                qualityLabel.setText(context.getResources().getString(R.string.label_score_miss) + " -" + loss);
                                qualityLabel.setTextColor(Color.RED);


                                missLocation.topMargin = missLocation.topMargin + 100;
                                missLocation.leftMargin = missLocation.leftMargin + 50;
                                missLocation.width = ViewGroup.LayoutParams.WRAP_CONTENT;
                                missLocation.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                                qualityLabel.startAnimation(fadeInDissolve);

                                //Subtract time;
                                setTime(getTime() - loss, false);


                                //"Click the warp"
                                //Two critical sections!
                                warp.markMissed();
                                warp.callOnClick();

                                //Finally add label
                                gameArea.addView(qualityLabel, missLocation);
                            } else {
                                if (warp.getType() == BLACKHOLE) {


                                    //Declare Label
                                    final TextView qualityLabel = new TextView(context);

                                    //Where do we gen the animation?
                                    final RelativeLayout.LayoutParams missLocation = warp.getPosition();
                                    //Label Animation
                                    fadeInDissolve = new AlphaAnimation(0, 1);
                                    fadeInDissolve.setDuration(400);

                                    //Tweak the animation here by fading it out onEnd() and translating it onStart()
                                    fadeInDissolve.setAnimationListener(new Animation.AnimationListener() {
                                        @Override
                                        public void onAnimationStart(Animation animation) {
                                            qualityLabel.animate().translationXBy(40).setDuration(500).setInterpolator(new AccelerateDecelerateInterpolator()).start();
                                            qualityLabel.animate().translationYBy(-40).setDuration(500).setInterpolator(new AccelerateDecelerateInterpolator()).start();
                                        }

                                        @Override
                                        public void onAnimationEnd(Animation animation) {
                                            qualityLabel.animate().alpha(0f).setDuration(400).setInterpolator(new DecelerateInterpolator()).start();
                                        }

                                        @Override
                                        public void onAnimationRepeat(Animation animation) {

                                        }
                                    });


                                    //Design Label
                                    qualityLabel.setText(context.getResources().getString(R.string.label_blackhole_good));
                                    qualityLabel.setTextColor(Color.GREEN);


                                    missLocation.topMargin = missLocation.topMargin + 100;
                                    missLocation.leftMargin = missLocation.leftMargin + 50;
                                    missLocation.width = ViewGroup.LayoutParams.WRAP_CONTENT;
                                    missLocation.height = ViewGroup.LayoutParams.WRAP_CONTENT;

                                    gameArea.addView(qualityLabel, missLocation);
                                    qualityLabel.startAnimation(fadeInDissolve);


                                    //Label is created


                                    // Again, two crucial steps
                                    warp.markMissed(); //mark missed, so user doesn't lose points;
                                    warp.callOnClick(); //Run onclick, ADJUSTS with type.


                                } else {
                                    Log.i("Logic Engine=>", " User Missed warp!");
                                }
                            }
                        }
                    }
                }

                return false;

            }
        });

        //init our loop
        corePool.execute(new Runnable() {
            @Override
                    public void run() {

                if (frames == 0) {
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
                    //End fps timer definition (May convert to more modular runnable later
                }


                //Initial UI Updates (So the user wont see the clock at zero)
                graphics.order("Logic-UpdateTime");

                startTimer();
                pauseTimer(); //Pause the timer


                //Timer will unpause on first warp touch

                //This is the basis of our Logic Loop
                while (!halt) {

                    if (!isPaused) {
                        serve(context);
                    }

                    while (isPaused) {
                        //Serve Only
                        serve(context); //Check messages
                        tick();
                    }


                    //If the game screen is empty, order a warp()
                    if (GraphicsHandler.isGamespaceEmpty) {
                        graphics.order("Warp-" + getDiffLevel());
                        Log.i("Logic Engine=>", "Order placed");
                    }


                    //Push graphics engine cycle
                    graphics.update(context); //Makes calls to the UI thread depending on the current frame state

                    //End of game loop
                    tick(); //Push frame

                    //Always Update this at the end.
                    //Initial state
                    if (state == DONE) {
                        //Give the rest of the app a room to catch it
                        try {
                            Thread.sleep(100);
                            //resume
                            switchMode(Mode.RESUMING, context);
                        } catch (InterruptedException ie) {
                            Log.e("Logic Engine =>", " Severe Error, Program Will Now Exit " + ie.getLocalizedMessage());
                            System.exit(1);
                        }
                    } else {
                        state = IDLE;

                    }


                }


            }
        });
    }

    //MENU LOOP
    private void newContextMenuLoop(Context context) {
        /*
        This loop will need to handle displaying the menus and handling interaction logic for them
         */
    }

    //PAUSE LOOP
    @SuppressWarnings("unused")
    private void pauseLoop(Context context) {
        //Empty for now, But will reveal the opacity and reenable control fo the menu
    }


    //Switches modes and returns a reference to the engine
    public LogicEngine switchMode(Mode mode, Context callingActivityContext) {

        //Cannot switch mode while reset is happening
        //noinspection StatementWithEmptyBody
        while (!finishedReset) {
            //Wait
        }


        switch (mode) {
            case MENU:
                //Transition from Game.
                if (state == Mode.GAME) {
                    halt = true;
                    state = Mode.IDLE;
                }
                if (state != mode) {
                    if (halt) {
                        halt = false;
                    }
                    state = mode;
                    newContextMenuLoop(callingActivityContext);
                }
                break;
            case GAME:
                //Transition from menu
                if (state == Mode.MENU) {
                    halt = true;
                    state = Mode.IDLE;
                }
                if (state != mode) {
                    state = mode;
                    newContextGameLoop(callingActivityContext);
                }
                break;
            case PAUSED: //Nothing as it leads to the same outcome
            case PAUSING:
                state = PAUSING;
                //if Engine is not already Paused
                if (!isPaused) {
                    //Disable all warps
                    final RelativeLayout GameArea = (RelativeLayout) ((Activity) callingActivityContext).findViewById(R.id.GAME__Area);
                    for (int x = 0; x < GameArea.getChildCount(); x++) {
                        if (GameArea.getChildAt(x) instanceof Warp) {
                            GameArea.getChildAt(x).setClickable(false);
                        }
                    }


                    isPaused = true; //Flip the control boolean
                    pauseTimer(); //Double lock just in case some legacy components still check for timer activity
                    //Update State
                    state = PAUSED;
                }
                break;
            case RESUMING:
                //if Engine is Paused
                if (isPaused) {
                    state = RESUMING;
                    isPaused = false; //Flip the control boolean
                    unpauseTimer(); //unlock timer fully
                    //unfreeze warps, if it isn't gameover or if debugmode
                    if (!isGameOver || debugMode) {
                        final RelativeLayout gamescreen = (RelativeLayout) ((Activity) callingActivityContext).findViewById(R.id.GAME__Area);
                        for (int x = 0; x < gamescreen.getChildCount(); x++) {
                            if (gamescreen.getChildAt(x) instanceof Warp) {
                                gamescreen.getChildAt(x).setClickable(true);
                            }
                        }
                    }

                    //Update State back to GAME if it can
                    if (time != 0)
                        state = GAME;
                    else {
                        //end the game prematurely;
                        if (!isGameOver) {
                            switchMode(GAMEOVER, callingActivityContext);
                        }
                    }


                }
                break;
            case GAMEOVER:
                halt = true; //Break the game loop
                //Show game over splash and disable UI input;
                pauseTimer();
                state = mode;
                graphics.order("Logic-GameOver");
            default:
                return this;

        }
        return this;
    }

    //Reset orders NEED effective mode management, read the comment
    private void reset(final Context context) {
        //First set the state as BUSY and Halt
        state = Mode.BUSY;
        //Flip the halt switch
        halt = true;
        try {
            //Wait a bit for everything to end
            Thread.sleep(100);
        } catch (InterruptedException ie) {
            state = Mode.ERROR;
            Log.e("Logic Engine =>", "Critical Error, Program Will Now Exit!  \n" + ie.getLocalizedMessage());
        }

        corePool.execute(new Runnable() {
            @Override
            public void run() {
                //Alert the Graphics

                graphics.order("Logic-Reset"); //THIS BLOCKS AND WILL NOT UPDATE UI UNTIL LOGIC HAS FINISHED, ELSE WE GET CONTINUITY ISSUES!!!!!!
                //There are no looper threads running currently, so we need to manually call update()

                graphics.update(context);
                //Will not proceed until engine has left busy state!;

            }
        });

        //noinspection StatementWithEmptyBody
        while (GraphicsHandler.State() != Mode.BUSY) {
            //Set state as IDLE
            state = IDLE;
            //Wait for handler to process
            if (GraphicsHandler.State() == Mode.BUSY) {
                break;
            }
        }

        state = BUSY;

        //Reset all vars
        popCount = 0L;
        avgPopTime = 0.0;
        totalPopTime = 0.0;
        oldAvgPopTime = 0.0;
        userTouchCount = 0L;
        time = 0L;
        userTouchX = 0;
        userTouchY = 0;
        maxPopTime = 0.0;
        minPopTime = 5.0;
        level = INTRO;
        score = 0;

        gameTimer.cancel();


        //Don't edit this.
        state = Mode.IDLE; //Lets graphics handler proceed

        //Wait for it to finish
        //noinspection StatementWithEmptyBody
        while (GraphicsHandler.State() == Mode.BUSY) {
            //Leave the busy state;
        }

        try {
            Thread.sleep(50);
        } catch (InterruptedException ie) {
            state = Mode.ERROR;
            Log.e("Logic Engine =>", "Critical Error, Program Will Now Exit!  \n" + ie.getLocalizedMessage());
        }


        //unflip the halt switch
        halt = false;

        try {
            Thread.sleep(100);
        } catch (InterruptedException ie) {
            state = Mode.ERROR;
            Log.e("Logic Engine =>", "Critical Error, Program Will Now Exit!  \n" + ie.getLocalizedMessage());
        }

        //WAIT FOR HANDLER TO BE DONE
        //noinspection StatementWithEmptyBody
        while (GraphicsHandler.State() != DONE) {
            state = IDLE;
            //Do nothing but wait!
        }
        state = Mode.DONE;

        Log.i("Logic Engine =>", " Done with reset!");
        //start new game
        newContextGameLoop(context);


    }

    //End loops
    //Start the game timer;
    private void startTimer() {
        gameTimer = new Timer(true);
        time = roundTime;


        gameTimer.schedule(new TimerTask() {
            @Override
            public void run() {


                //Update the timer, believe it or not in the extreme you can catch the timer mid cycle
                //Thats why it checks conditions again.
                //noinspection ConstantConditions
                if (!timeModding && state != PAUSED) {
                    //This is to fix continuity issues

                    //noinspection SynchronizeOnNonFinalField
                        synchronized (time) {
                            time = time - 1;
                            //Update ui
                            graphics.order("Logic-UpdateTime");
                        }
                        Log.i("TIME =>", "New time:" + time);

                }


                if (time <= 0) {
                    boolean saved = false;//Did the user save themself?
                    time = 0L;
                    //Update the UI
                    graphics.order("LOGIC-UpdateTime");

                    //wait a bit for any cahnges
                    //Fixing desync issues
                    try {
                        Thread.sleep(500);

                        if (time <= 0) {
                            //Update the UI
                            graphics.order("LOGIC-UpdateTime");
                            this.cancel();
                        } else {
                            //Update the UI
                            graphics.order("LOGIC-UpdateTime");
                            saved = true;
                        }

                    } catch (InterruptedException ie) {
                        //do nothing
                    }

                    if (!saved) {
                        //This is what happens when the timer hits zero
                        if (!debugMode) {
                            //End game
                            //UI End
                            graphics.order("Logic-GameOver");
                            //Logic End
                            isGameOver = true;
                            state = GAMEOVER;
                        } else {
                            //Logic End only
                            state = AFTERTIME; //After game is a debug only state;
                        }
                    }
                }
                //Secondary time catch. [Again we disable Empty Body check]
                //noinspection StatementWithEmptyBody
                while (timeModding || state == PAUSED) {
                    //Halt while paused
                }
            }
        }, 500, 1000);
    }


    //Logic engine really only listens to Activities, as context is required for many of it's ops
    public void order(@NonNull String message, @NonNull Context callingActivityContext) throws InvalidObjectException {
        //Test if valid activity context
        View test = ((Activity) callingActivityContext).findViewById(R.id.GAME__Area);
        if (test == null) {
            throw new InvalidObjectException("Context must be activity level!");
        } else {
            messageQueue.add(message);
        }
        //That was easy, all the fun stuff happens in serve()
    }

    //Push frames
    private void tick(){
       frames++;
    }

    //Link Graphics engine (Should only be done by Engine ONCE on startup. Try to avoid manually calling this
    private void linkGraphics(GraphicsHandler handler ){
        if(graphics ==null){
            graphics=handler;
        }
    }


    //ENUMS

    //Core Control (Mode set, Loop control)
    public enum Mode {
        GAME,
        MENU,
        IDLE,
        PAUSING,
        PAUSED,
        RESUMING,
        GAMEOVER,
        AFTERTIME, //This can only be called when app is in debug mode!
        BUSY, // Mainly called when engine is doing something that should'nt be spammed (LIKE RESET)
        DONE,
        ERROR


    }

    //Time punishment constants
    // Why Here? Well because It needs to be easy for me to tweak lol
    private enum Punishment {
        EASY(5),
        INTER(7),
        HARD(10),
        MISS_EASY(1),
        MISS_INTER(2),
        MISS_HARD(3);

        private int value;

        Punishment(int value) {
            this.value = value;
        }

        public int value() {
            return value;
        }
    }

    //Enums that store game difficulties
    enum Difficulties {
        EASY(0),
        INTER(1),
        HARD(2);

        private int level;

        Difficulties(int i) {
            level = i;
        }

        public int ID() {
            return this.level;
        }

    }

    //This is much easier way of handling all the types
    enum Type {
        NORMAL,
        BLACKHOLE,
        SPLITTER,
        WORMHOLE


    }


    //used to represent the current progression of the player.
    enum Level {
        INTRO(0),
        BEGINNER(1),
        INTERMEDIATE(2),
        ADVANCED(3),
        MASTER(4),
        LEGENDARY(5); // Not used yet.

        private int value = 0;

        Level(int val) {
            value = val;
        }

        public int value() {
            return value;
        }


    }
}




