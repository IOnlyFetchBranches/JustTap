package com.justtap.comp;

import android.content.Context;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;

import static com.justtap.utl.Printers.*;

/**
    The main thing responsible for handling graphics
    Animate(reference to object, runnable) Will put a task on the animation queue

    Any object added to the queue should specify a min/max for the value it wants animated unless predefined

 */

public class GraphicsHandler {

    //Singleton
    private static GraphicsHandler instance;
    private  static  LogicEngine engine;



    //Components
    private static List<Object> animationQueue= Collections.synchronizedList(new ArrayList<>());
    private static LinkedBlockingQueue<String> messageQueue=new LinkedBlockingQueue<String>();

    //Control Booleans
    static boolean isGamespaceEmpty=true;


    private GraphicsHandler(){
        //Empty constructor to bar default.
    }




    //Where the magic happens;
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





    //Message handling
    public void serve(){

    }
    public void serve(Context context){
        if(!messageQueue.isEmpty()){
            while(!messageQueue.isEmpty()){
                String message=messageQueue.poll();

                switch(message){
                    case "Warp-EASY":
                        genWarp(1,context);
                        break;
                    case "Warp-MEDIUM":
                        genWarp(2,context);
                        break;
                    case "Warp-HARD":
                        genWarp(3,context);
                        break;
                    case "Warp-Pop":
                        popWarp(1,context);
                    default:
                        logLevel("Invalid messaage passed to gfx-handle -> "+ message, Level.WARNING);
                }
            }
        }
    }

    //Warp function
    private void genWarp(int difficulty, Context context){
        if(isGamespaceEmpty) {
            isGamespaceEmpty = false;


            switch (difficulty) {

                case 1:
                    //Create the warp
                    Warp warp = new Warp(context, this);

                    //style warp according to type


                    //add to UI



                    //add to animation queue




                    break;


                default:
                    throw new IllegalArgumentException("Input for genWarp != 0<x<=3");
            }


        }



    }
    private void popWarp(int amount, Context context) {
        if(amount==1) {
            Object tmp=null;
            for (Object obj : animationQueue) {
                if (obj instanceof Warp) {
                    tmp = obj;
                    break;
                }
            }

            if(tmp != null)
                animationQueue.remove(tmp);
            else
                logLevel("Unable to pop warp as there is none!", Level.WARNING);
        }

        isGamespaceEmpty=true;

    }


    public void order(String message){



        }


    //Singleton
    public static  GraphicsHandler getInstance(LogicEngine logicEngine){
        if(instance == null) {
            engine = logicEngine;
            instance = new GraphicsHandler();
            return instance;
        }
        else{
            return instance;
        }



    }





}
