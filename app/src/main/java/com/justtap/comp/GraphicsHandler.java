package com.justtap.comp;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.justtap.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;

import static com.justtap.comp.GraphicsHandler.Difficulties.EASY;
import static com.justtap.comp.GraphicsHandler.Difficulties.HARD;
import static com.justtap.comp.GraphicsHandler.Difficulties.MEDIUM;
import static com.justtap.utl.Numbers.genInt;
import static com.justtap.utl.Printers.logLevel;

/**
    The main thing responsible for handling graphics
    Animate(reference to object, runnable) Will put a task on the animation queue

    Any object added to the queue should specify a min/max for the value it wants animated unless predefined

 */

public class GraphicsHandler {

    //Control Booleans
    static boolean isGamespaceEmpty = true;
    //Singleton
    private static GraphicsHandler instance;
    private  static  LogicEngine engine;
    //Components
    private static List<Object> animationQueue= Collections.synchronizedList(new ArrayList<>());
    private static LinkedBlockingQueue<String> messageQueue=new LinkedBlockingQueue<String>();

    private GraphicsHandler(){
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

    //Where the magic happens, runs the graphic cycle, doesnt need to be looped, as the logic handles the main Looper
    void update(Context context) {

        //We may need have a message to add or remove a node to be animated so we need to call serve()
        serve(context); //check queue

        //Now handle existing nodes
        if(!animationQueue.isEmpty()){
            for (Object obj : animationQueue) {
                if (obj instanceof Warp) {
                    //What to do with our warps

                }
            }
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


    //Message handling

    private void serve(Context context) {
        if(!messageQueue.isEmpty()){
            while(!messageQueue.isEmpty()){
                String message=messageQueue.poll();
                Log.i("Graphic Handler =>", "Handling message " + message);

                switch(message){
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
                    default:
                        logLevel("Invalid messaage passed to gfx-handle -> "+ message, Level.WARNING);
                }
            }
        }
    }

    //Warp function
    private void genWarp(Difficulties difficulty, final Context context) {
        if (GraphicsHandler.isGamespaceEmpty) {
            GraphicsHandler.isGamespaceEmpty = false;
            //Declare our warp;


            switch (difficulty) {

                case EASY:
                    //Create the warp
                    final Warp warp = new Warp(EASY.ID(), context, this);

                    //style warp according to type (if there is no defaults)
                    //Id rather focus on getting core to work right now, come to later
                    //add to UI

                    final RelativeLayout gamescreen = (RelativeLayout) ((Activity) context).getWindow().getDecorView().findViewById(R.id.GAME__Area);
                    if (gamescreen == null) {
                        Log.e("Graphic Engine =>", "Gamescreen cannot be found!");
                    }

                    warp.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Log.e("WARP =>", "Warp tapped!");

                            gamescreen.removeView(warp);
                            //Initiate scoring
                            LogicEngine.CalculateScore(warp.pop(), warp.getType());
                            // animationQueue.remove(warp);
                            Log.i("Graphic Handler =>", "Removed Warp!");

                            isGamespaceEmpty = animationQueue.size() == 0;


                        }
                    });


                    RelativeLayout.LayoutParams position = null;
                    warp.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                    warp.setMinimumHeight(250);
                    warp.setMinimumWidth(250);
                    boolean valid = false;
                    while (!valid) {
                        position = new RelativeLayout.LayoutParams
                                (250, 250);

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

                    final RelativeLayout.LayoutParams finalPosition = position;


                    ((Activity) context).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            gamescreen.addView(warp, finalPosition);


                        }
                    });


                    //add to animation queue


                    break;


                default:
                    throw new IllegalArgumentException("Input for genWarp != 0<x<=3");
            }


        }



    }


    //This is how outside entities add to the engine's workload
    //Try not to directly call animation, this method is the safest
    public void order(String message) {
        messageQueue.add(message);
        Log.i("Graphic Handler =>", message);
    }


    //Enums
    public enum Difficulties {
        EASY(0),
        MEDIUM(1),
        HARD(2);

        private int level;

        Difficulties(int i) {
            level = i;
        }

        public int ID() {
            return this.level;
        }

    }





}
