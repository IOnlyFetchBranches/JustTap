package com.justtap.act;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.shapes.OvalShape;
import android.graphics.drawable.shapes.Shape;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.justtap.R;
import com.justtap.comp.LogicEngine;

import java.util.Timer;
import java.util.TimerTask;


import static com.justtap.utl.Printers.*;

public class Gamescreen_Tutorial extends AppCompatActivity{

    //Importantly we need a tick
    private static long frames=0;
    //Timer for Fps
    private static Timer fpsTimer=new Timer();
    private static TimerTask framecounter;


    //Logic Engine
    private static LogicEngine engine;






    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gamescreen__tutorial);


        //Start GameLoop
        engine=LogicEngine.getInstance(this);


    }


    private synchronized static void tick(){
        frames++;
    }
}
