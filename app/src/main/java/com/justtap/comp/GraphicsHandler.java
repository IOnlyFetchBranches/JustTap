package com.justtap.comp;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.CycleInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.RotateAnimation;
import android.view.animation.TranslateAnimation;
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
import static com.justtap.comp.LogicEngine.Difficulties.INTER;
import static com.justtap.comp.LogicEngine.getUserProgression;
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


    //Animation Constants
    private long WARP_CYCLE_SPEED = 250;
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

    //Animations
    public static void floatView(final long timeinmillis, final float distanceY, final float distanceX,
                                 final boolean floatUpFirst, final View view, final Context context) {
        ((Activity) context).runOnUiThread(new Runnable() {
            @Override
            public void run() {
                final TranslateAnimation floatDown = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF, 0.0f,
                        Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF, distanceY);
                floatDown.setInterpolator(new AccelerateDecelerateInterpolator());
                floatDown.setDuration(timeinmillis);


                final TranslateAnimation floatUp = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF, 0.0f,
                        Animation.RELATIVE_TO_SELF, distanceY, Animation.RELATIVE_TO_SELF, 0.0f);
                floatUp.setInterpolator(new AccelerateDecelerateInterpolator());
                floatUp.setDuration(timeinmillis);

                //Our animation loop begins here
                //Float Down Triggers Float Up
                floatDown.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {
                        view.animate().scaleXBy(-distanceX).setInterpolator(new AccelerateDecelerateInterpolator()).setDuration(timeinmillis);
                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        view.startAnimation(floatUp);
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {

                    }
                });
                //Float Up Triggers Float Up
                floatUp.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {
                        view.animate().scaleXBy(distanceX).setInterpolator(new AccelerateDecelerateInterpolator()).setDuration(timeinmillis);
                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        view.startAnimation(floatDown);
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {

                    }
                });

                //Finally trigger the animation cycle
                if (floatUpFirst)
                    view.startAnimation(floatUp);
                else
                    view.startAnimation(floatDown);
            }
        });
    }

    public static void shakeView(final long timeinmillis, final float distance,
                                 final View view, final Context context) {
        final RotateAnimation rotateRightFull = new RotateAnimation(0, distance);
        final RotateAnimation rotateZero = new RotateAnimation(distance, 0);
        final RotateAnimation rotateLeftFull = new RotateAnimation(0, -distance);
        final RotateAnimation rotateZeroFinal = new RotateAnimation(-distance, 0);

        //Set interpolators

        rotateRightFull.setInterpolator(new AccelerateDecelerateInterpolator());
        rotateZero.setInterpolator(new AccelerateDecelerateInterpolator());
        rotateLeftFull.setInterpolator(new AccelerateDecelerateInterpolator());
        rotateZeroFinal.setInterpolator(new AccelerateDecelerateInterpolator());

        //set Duration

        rotateRightFull.setDuration(timeinmillis);
        rotateZero.setDuration(timeinmillis);
        rotateLeftFull.setDuration(timeinmillis);
        rotateZeroFinal.setDuration(timeinmillis);

        //Loop animations here
        rotateRightFull.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                view.startAnimation(rotateZero);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });

        rotateZero.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                view.startAnimation(rotateLeftFull);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });

        rotateLeftFull.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                view.startAnimation(rotateZeroFinal);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });

        rotateZeroFinal.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                view.startAnimation(rotateRightFull);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });

        view.startAnimation(rotateRightFull);

    }


    //Message handling

    public static void fadeView(final long timeinmillis, final float to, final float from, final boolean fadeOutFirst
            , final View view, final Context context) {

        final AlphaAnimation fadeOut = new AlphaAnimation(view.getAlpha(), to);
        final AlphaAnimation fadeIn = new AlphaAnimation(view.getAlpha(), from);

        fadeOut.setInterpolator(new AccelerateDecelerateInterpolator());
        fadeIn.setInterpolator(new AccelerateDecelerateInterpolator());

        fadeOut.setDuration(timeinmillis);
        fadeIn.setDuration(timeinmillis);

        fadeOut.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                view.startAnimation(fadeIn);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        fadeIn.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                view.startAnimation(fadeOut);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });

        view.startAnimation(fadeOut);

    }
    //GRAPHICS OPERATIONS

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

    @SuppressWarnings("StatementWithEmptyBody")
    private void serve(Context context) {
        if (!messageQueue.isEmpty()) {
            while (!messageQueue.isEmpty()) {
                String message = messageQueue.poll();
                Log.i("Graphic Handler =>", "Handling message " + message);

                if (message == null) {
                    break;
                }

                //EASY Warps are normal, INTER can be Black holes, Hard can be both as well as wormholes
                switch (message) {
                    case "Warp-EASY":
                        genWarp(EASY, context);
                        break;
                    case "Warp-INTER":
                        genWarp(INTER, context);
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
                    //Clears screen and resets timer
                    case "Logic-EndGame":
                        end(context);
                        break;
                    default:
                        logLevel("Invalid message passed to gfx-handler -> " + message, Level.WARNING);
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

            // #Define
            final Warp warp;
            //Create the warp
            warp = new Warp(getUserProgression(), context, this);



            //style warp according to type (if there is no defaults)
            //Id rather focus on getting core to work right now, come to later
            //add to UI

            final RelativeLayout gameArea = (RelativeLayout) ((Activity) context).getWindow().getDecorView().findViewById(R.id.GAME__Area);
            if (gameArea == null) {
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


                assert gameArea != null;

                //ALWAYS USE %'s it SCALES!!!!
                //We want to add a random value between ten percent of the screens width for left margin
                //Current best is between 30-40% for left and always minus 10 percent for right
                position.leftMargin = genInt(0, gameArea.getWidth() / 2) + genInt(0, Double.valueOf((gameArea.getWidth() * .35)).intValue());
                position.rightMargin = genInt(0, gameArea.getWidth() / 2) - genInt(0, Double.valueOf((gameArea.getWidth() * .1)).intValue());
                position.topMargin = genInt(0, gameArea.getHeight());

                warp.setPosition(position);

                warp.measure(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);

                Log.i("Graphic Handler =>", "Margins " + position.topMargin + " " + position.bottomMargin
                        + " " + position.leftMargin + " " + position.rightMargin + "Dims : " +
                        warp.getMeasuredHeight() + " " + warp.getMeasuredWidth());


                if (gameArea.getHeight() - position.topMargin < 200) {
                    continue;
                } else {
                    valid = true;
                }

            }
            //What happens when user taps warp;
            warp.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //Only works when game is not paused
                    if (LogicEngine.State() != LogicEngine.Mode.PAUSING && LogicEngine.State() != LogicEngine.Mode.PAUSED) {
                        Log.e("WARP =>", "Warp tapped!");

                        //Disable onclick
                        warp.setOnClickListener(new View.OnClickListener() {

                            @Override
                            public void onClick(View v) {
                                //Do nothing
                            }
                        });

                        //Fade it out fast
                        warp.setAnimation(new AlphaAnimation(1, 0));


                        animationQueue.remove((warp));

                        //Initiate scoring, initiate warp pop()
                        //This also spawns a new warp via a graphics handler req.

                        //Score, if not just a missed warp.
                        if (!warp.isMissed())
                            LogicEngine.CalculateScore(warp.pop(), warp, position, context);
                        else if (warp.isMissed() && warp.getType() == LogicEngine.Type.BLACKHOLE)
                            LogicEngine.CalculateScore(warp.pop(), warp, position, context);


                        Log.i("Graphic Handler =>", "Removed Warp!");

                        //Remove the warp
                        for (int i = 0; i < gameArea.getChildCount(); i++) {
                            if (gameArea.getChildAt(i) instanceof Warp) {
                                gameArea.removeViewAt(i);
                            }
                        }

                        isGamespaceEmpty = true; //Warp is popped

                        //Add warp to animation queue

                    } else {
                        logLevel("Game is paused, Cannot tap warp!", Level.WARNING);
                    }
                }
            });


            //ADD and TWEAK Animation
            ((Activity) context).runOnUiThread(
                    //Define the animation and apply it
                    //Also loops back to itself
                    new Runnable() {
                        @Override
                        public void run() {
                            //Rotation Animation
                            warp.animate().rotationBy(360).setInterpolator(new CycleInterpolator(WARP_CYCLE_SPEED)).setDuration(600000).start();

                            //Warp animation
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
                    gameArea.addView(warp, finalPosition);


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

    private void end(Context context) {

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

        //When the logic engine is finished run initial UI updates
        updateTime(context);
        updateScore(context);
        animationQueue.clear();
        state = LogicEngine.Mode.DONE;
    }

    //This is how outside entities add to the engine's workload
    //Try not to directly call animation, this method is the safest
    public void order(String message) {
        messageQueue.add(message);
        Log.i("Graphic Handler =>", message);
    }


    //End Class

}








