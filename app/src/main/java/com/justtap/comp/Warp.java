package com.justtap.comp;


import android.content.Context;
import android.media.Image;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;

public class Warp extends android.support.v7.widget.AppCompatImageView{

    //Time of creation (Called to see how long it took to hit
    private final long createTime=System.currentTimeMillis();

    private GraphicsHandler parent;


    //Controls
    public Warp(Context context, GraphicsHandler parent){
        super(context);

        this.parent=parent; //link to the graphics handler

        //Set the ontouch listener
        setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                LogicEngine.CalculateScore(pop());
            }
        });



    }

    //This function is called when the user taps the created drawable, Returns the amount of milliseconds it took them.
    private long pop(){
        this.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                //Do nothing again (Disable)
            }
        });

        //disappear *poof*
        this.setAlpha(0f);

        parent.order("Warp-Pop");
        while(!GraphicsHandler.isGamespaceEmpty){
            //wait for pop to occur
        }

        //gen a new one based on the current difficulty level
        parent.order("Warp-"+LogicEngine.getDiffLevel());

        return System.currentTimeMillis() - createTime;
    }





}
