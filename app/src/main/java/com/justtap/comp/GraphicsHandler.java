package com.justtap.comp;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.justtap.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;

import static com.justtap.comp.LogicEngine.Difficulties.EASY;
import static com.justtap.comp.LogicEngine.Difficulties.HARD;
import static com.justtap.comp.LogicEngine.Difficulties.MEDIUM;
import static com.justtap.utl.Numbers.genInt;
import static com.justtap.utl.Printers.logLevel;

/**
 The main thing responsible for handling graphics requests and getting them to the UI thread efficiently
 It's short now, but it does a lot, and will grow a lot as more graphics abilities get added to the game
 Right now it's dwarfed by the logic engine, because of the nature of the task but as the game matures
 Graphics/Animations must be added and such


 */

public class GraphicsHandler {

    //Animations (Defined globally so they can be cancelled across Handler)
    private final static Animation ani = new AlphaAnimation(1f, .5f);
    private final static Animation Ani2 = new AlphaAnimation(.5f, .1f);
    //Control Booleans
    static boolean isGamespaceEmpty = true;
    //Singleton
    private static GraphicsHandler instance;
    private static LogicEngine engine;
    //Components
    private static List<Object> animationQueue = Collections.synchronizedList(new ArrayList<>());
    private static LinkedBlockingQueue<String> messageQueue = new LinkedBlockingQueue<String>();
    //State
    private static LogicEngine.Mode state = LogicEngine.Mode.IDLE;


    //Constants
    private long WARP_BLINK_DELAY_IN = 500; // Amount in ms, before warp fades in
    private long WARP_BLINK_DELAY_OUT = 400; //Amount in ms, before warp fades out;
    private long WARP_PULSE_DELAY_IN = 400;
    private long WARP_PULSE_DELAY_OUT = 400;


    private GraphicsHandler() {
        //Empty constructor to bar default.
    }

    //Singleton
    public static GraphicsHandler getInstance(LogicEngine logicEngine) {
        if (instance == null) {
            engine = logicEngine;
            instance = new GraphicsHandler();
            return instance;
        } else {
            return instance;
        }


    }

    //Get State
    public static LogicEngine.Mode State() {
        return state;
    }

    //Where the magic happens, runs the graphic cycle, does'nt need to be looped, as the logic handles the main Looper
    void update(@NonNull final Context context) {
        //if parent is not busy
        if (LogicEngine.State() != LogicEngine.Mode.BUSY) {
            if (state != LogicEngine.Mode.GAME)
                state = LogicEngine.Mode.GAME;
            //Yup it's message based for the most part so this drives the entire loop
            serve(context); //check queue.0k
            //Why not just call serve? Because that breaks my abstraction and also Update sounds better :P
        } else {
            serve(context);
        }



    }

    //UI Updates
    private void updateTime(Context context) {
        final TextView timeLabel = (TextView) ((Activity) context).findViewById(R.id.GAME_Timeview);

        ((Activity) context).runOnUiThread(new Runnable() {
            @SuppressLint("SetTextI18n")
            @Override
            public void run() {
                timeLabel.setText(LogicEngine.getTime() + "");
            }
        });
    }


    //Message handling

    private void updateScore(Context context) {
        final TextView scoreLabel = (TextView) ((Activity) context).findViewById(R.id.GAME_Scoreview);

        ((Activity) context).runOnUiThread(new Runnable() {
            @SuppressLint("SetTextI18n")
            @Override
            public void run() {

                scoreLabel.setText(LogicEngine.getScore() + "");

            }
        });
    }
    //GRAPHICS OPERATIONS

    @SuppressWarnings("StatementWithEmptyBody")
    private void serve(Context context) {
        if (!messageQueue.isEmpty()) {
            while (!messageQueue.isEmpty()) {
                String message = messageQueue.poll();
                Log.i("Graphic Handler =>", "Handling message " + message);

                if (message == null) {
                    break;
                }

                //EASY Warps are normal, MEDIUM can be Black holes, Hard can be both as well as wormholes
                switch (message) {
                    case "Warp-EASY":
                        genWarp(EASY, context);
                        break;
                    case "Warp-MEDIUM":
                        genWarp(MEDIUM, context);
                        break;
                    case "Warp-HARD":
                        genWarp(HARD, context);
                        break;
                    case "Logic-UpdateTime":
                        updateTime(context);
                        break;
                    case "Logic-UpdateScore":
                        updateScore(context);
                        break;
                    case "Logic-UpdateLevel":
                        //Finish Later;
                        break;
                    case "Logic-Reset":
                        reset(context);
                        break;
                    default:
                        logLevel("Invalid messaage passed to gfx-handle -> " + message, Level.WARNING);
                }
            }
        }
    }


    //END GRAPHICS OPERATIONS

    //Warp function
    private void genWarp(LogicEngine.Difficulties difficulty, final Context context) {
        if (GraphicsHandler.isGamespaceEmpty) {
            GraphicsHandler.isGamespaceEmpty = false;
            //Declare our warp;


            final Warp warp;
            //Create the warp at a specified difficulty
            if (difficulty == EASY)
                warp = new Warp(EASY.ID(), context, this);
            else if (difficulty == MEDIUM) {
                warp = new Warp(MEDIUM.ID(), context, this);
            } else {
                warp = new Warp(HARD.ID(), context, this);
            }


            //style warp according to type (if there is no defaults)
            //Id rather focus on getting core to work right now, come to later
            //add to UI

            final RelativeLayout gamescreen = (RelativeLayout) ((Activity) context).getWindow().getDecorView().findViewById(R.id.GAME__Area);
            if (gamescreen == null) {
                Log.e("Graphic Engine =>", "Gamescreen cannot be found!");
            }


            //Next we use Layout params to randomize the position of our new young warp

            final RelativeLayout.LayoutParams position = new RelativeLayout.LayoutParams(250, 250);
            warp.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            //250*250 should be general size
            warp.setMinimumHeight(250);
            warp.setMinimumWidth(250);
            boolean valid = false;
            while (!valid) {


                assert gamescreen != null;

                //ALWAYS USE %'s it SCALES!!!!
                //We want to add a random value between ten percent of the screens width for left margin
                //Current best is between 30-40% for left and always minus 10 percent for right
                position.leftMargin = genInt(0, gamescreen.getWidth() / 2) + genInt(0, Double.valueOf((gamescreen.getWidth() * .35)).intValue());
                position.rightMargin = genInt(0, gamescreen.getWidth() / 2) - genInt(0, Double.valueOf((gamescreen.getWidth() * .1)).intValue());
                position.topMargin = genInt(0, gamescreen.getHeight());

                warp.measure(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);

                Log.i("Graphic Handler =>", "Margins " + position.topMargin + " " + position.bottomMargin
                        + " " + position.leftMargin + " " + position.rightMargin + "Dims : " +
                        warp.getMeasuredHeight() + " " + warp.getMeasuredWidth());


                if (gamescreen.getHeight() - position.topMargin < 200) {
                    continue;
                } else {
                    valid = true;
                }

            }
            //What happens when user taps warp;
            warp.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.e("WARP =>", "Warp tapped!");


                    gamescreen.removeView(warp);
                    animationQueue.remove((warp));
                    warp.animate().alpha(0f).start();
                    //Initiate scoring, initiate warp pop()
                    //This also spawns a new warp via a graphics handler req.
                    LogicEngine.CalculateScore(warp.pop(), warp.getType(), position, context);


                    Log.i("Graphic Handler =>", "Removed Warp!");

                    isGamespaceEmpty = true; //Warp is popped

                    //Add warp to animation queue


                }
            });


            //ADD and TWEAK Animation
            ((Activity) context).runOnUiThread(
                    //Define the animation and apply it
                    //Also loops back to itself
                    new Runnable() {
                        @Override
                        public void run() {


                            DecelerateInterpolator inter = new DecelerateInterpolator();
                            ani.setDuration(WARP_BLINK_DELAY_OUT);
                            ani.setInterpolator(inter);
                            ani.setAnimationListener(new Animation.AnimationListener() {
                                @Override
                                public void onAnimationStart(Animation animation) {
                                    //Async Animations

                                }

                                @Override
                                public void onAnimationEnd(Animation animation) {

                                    warp.setVisibility(View.VISIBLE);
                                    Ani2.setDuration(WARP_BLINK_DELAY_IN);
                                    AccelerateInterpolator inter = new AccelerateInterpolator();
                                    Ani2.setInterpolator(inter);
                                    Ani2.setAnimationListener(new Animation.AnimationListener() {
                                        @Override
                                        public void onAnimationStart(Animation animation) {

                                        }

                                        @Override
                                        public void onAnimationEnd(Animation animation) {
                                            warp.startAnimation(ani);
                                        }

                                        @Override
                                        public void onAnimationRepeat(Animation animation) {

                                        }
                                    });
                                    warp.startAnimation(Ani2);
                                }

                                @Override
                                public void onAnimationRepeat(Animation animation) {


                                }
                            });

                            warp.startAnimation(ani);
                        }
                    }
            );


            //Add to animation queue;
            animationQueue.add(warp);

            //Finally add to UI
            final RelativeLayout.LayoutParams finalPosition = position;
            ((Activity) context).runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    gamescreen.addView(warp, finalPosition);


                }
            });


        }
    }

    //Reset state
    private void reset(Context context) {
        state = LogicEngine.Mode.BUSY;
        final RelativeLayout gameArea = (RelativeLayout) ((Activity) context).findViewById(R.id.GAME__Area);

        ((Activity) context).runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //Remove all warps
                for (int x = 0; x < gameArea.getChildCount(); x++) {
                    if (gameArea.getChildAt(x) instanceof Warp) {
                        gameArea.removeView(gameArea.getChildAt(x));
                        isGamespaceEmpty = true;
                    }
                }
            }

        });

        while (LogicEngine.State() == LogicEngine.Mode.BUSY) {
            //Do nothing but wait
            state = LogicEngine.Mode.IDLE;
        }
        //When the logic engine is finished run initial UI updates
        state = LogicEngine.Mode.BUSY;
        updateTime(context);
        updateScore(context);
        animationQueue.clear();
        state = LogicEngine.Mode.DONE;

        while (LogicEngine.State() == LogicEngine.Mode.BUSY) {
            //Wait for Logic to finish;
        }
    }

    //This is how outside entities add to the engine's workload
    //Try not to directly call animation, this method is the safest
    public void order(String message) {
        messageQueue.add(message);
        Log.i("Graphic Handler =>", message);
    }


    //End Class

}








