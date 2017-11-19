package com.justtap.act;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.RelativeLayout;

import com.justtap.R;
import com.justtap.comp.LogicEngine;

import java.util.Timer;
import java.util.TimerTask;

public class Gamescreen_New extends AppCompatActivity {

    //Importantly we need a tick
    private static long frames=0;
    //Timer for Fps
    private static Timer fpsTimer=new Timer();
    private static TimerTask framecounter;


    //Logic Engine
    private static LogicEngine engine;

    private synchronized static void tick() {
        frames++;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gamescreen__new);

        //Set it fullscreen.
        int UI_OPTIONS = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;
        getWindow().getDecorView().setSystemUiVisibility(UI_OPTIONS);

        RelativeLayout gamearea = (RelativeLayout) findViewById(R.id.GAME__Area);
        Log.i("GAME =>", gamearea.isShown() + " ");



        //Start GameLoop
        engine=LogicEngine.getInstance(this);


    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d("Act:Gamescreen", "User has left the app!");
    }

    @Override
    protected void onResume() {
        //Set it fullscreen everytime the app is put back on the screen!
        int UI_OPTIONS = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;
        getWindow().getDecorView().setSystemUiVisibility(UI_OPTIONS);
        super.onResume();
    }
}
