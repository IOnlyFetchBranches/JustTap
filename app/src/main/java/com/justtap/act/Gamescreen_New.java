package com.justtap.act;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.ViewStub;
import android.widget.Button;
import android.widget.ImageButton;

import com.justtap.R;
import com.justtap.comp.LogicEngine;

import java.io.InvalidObjectException;

public class Gamescreen_New extends AppCompatActivity {

    //Logic Engine, This drives the game.
    private static LogicEngine engine;
    //Views
    private View pauseMenu = null;
    //Control Booleans
    private boolean wasPaused = false; // Is there still a viewstub for us to inflate??



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gamescreen__new);

        //Set it fullscreen.
        int UI_OPTIONS = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;
        getWindow().getDecorView().setSystemUiVisibility(UI_OPTIONS);

        //Define UI Buttons
        ImageButton pauseButton = (ImageButton) findViewById(R.id.GAME_PauseButton);
        pauseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showPauseMenu();
            }
        });


        //Onc you attach the engine, the GameLoop starts
        engine = LogicEngine.getInstance(this, LogicEngine.Mode.GAME);


    }

    //Game methods
    private void showPauseMenu() {
        if (!wasPaused) {
            wasPaused = true;
            //Inflate the menu for the first time, this eliminates the view stub
            ViewStub pauseStub = (ViewStub) findViewById(R.id.GAME_PauseStub);
            //Store our new menu in pause menu for quicker access
            pauseMenu = pauseStub.inflate();

            //Button logic
            Button resumeButton = (Button) findViewById(R.id.PAUSE_ResumeButton);
            final Button restartButton = (Button) findViewById(R.id.PAUSE_RestartButton);
            Button exitButton = (Button) findViewById(R.id.PAUSE_ExitButton);

            final Context actContext = this;

            resumeButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showPauseMenu();
                }
            });
            restartButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    try {
                        //Request engine to reset
                        engine.order("Game-Reset", actContext);

                        //Wait for it to reset
                        //noinspection StatementWithEmptyBody

                        pauseMenu.setVisibility(View.GONE); //Hide the menu (DOES NOT GET RID OF THE MEMORY!
                        engine.switchMode(LogicEngine.Mode.RESUMING, actContext);


                    } catch (InvalidObjectException io) {
                        Log.e("GAME =>", io.getLocalizedMessage());
                    }
                }
            });

            exitButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    finish();
                    Intent intent = new Intent(Intent.ACTION_MAIN);
                    intent.addCategory(Intent.CATEGORY_HOME);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    System.exit(0);
                }
            });

            //issue command to logic engine
            engine.switchMode(LogicEngine.Mode.PAUSING, this);
        } else {
            Log.i("DEBUG", "STATE OF ENGINE =>" + LogicEngine.State());
            if (LogicEngine.State() == LogicEngine.Mode.PAUSED || LogicEngine.State() == LogicEngine.Mode.PAUSING) {
                pauseMenu.setVisibility(View.GONE); //Hide the menu (DOES NOT GET RID OF THE MEMORY!
                engine.switchMode(LogicEngine.Mode.RESUMING, this);

            } else {

                pauseMenu.setVisibility(View.VISIBLE);
                engine.switchMode(LogicEngine.Mode.PAUSING, this);


            }

        }

    }


    //These are not game methods! Overridden Android base methods do not TOUCH!
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
