package com.justtap.act;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.ViewStub;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import com.justtap.R;
import com.justtap.comp.GraphicsHandler;
import com.justtap.comp.LogicEngine;
import com.justtap.comp.models.Settings;
import com.justtap.comp.models.SettingsManager;

import java.io.InvalidObjectException;

public class Gamescreen_New extends AppCompatActivity {

    //Logic Engine, This drives the game.
    private static LogicEngine engine;
    //Views
    private View pauseMenu = null;
    //Control Booleans
    private boolean wasPaused = false; // Is there still a viewstub for us to inflate??
    private boolean pauseMenuShowing = false;



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

        //style ui
        final TextView timeLabel, timeView, scoreLabel, scoreView;
        Settings settings = SettingsManager.get(this);

        //For Time Label
        timeLabel = (TextView) findViewById(R.id.GAME_Timelabel);

        timeLabel.setTextSize(Settings.spUnit, Settings.primaryHeaderFontSizeSP);
        timeLabel.setTypeface(settings.getFont(Settings.Property.Font_Header));


        //animations
        final TranslateAnimation floatDown = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF, 0.0f,
                Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF, .15f);
        floatDown.setInterpolator(new AccelerateDecelerateInterpolator());
        floatDown.setDuration(1000);


        final TranslateAnimation floatUp = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF, 0.0f,
                Animation.RELATIVE_TO_SELF, .15f, Animation.RELATIVE_TO_SELF, 0.0f);
        floatUp.setInterpolator(new AccelerateDecelerateInterpolator());
        floatUp.setDuration(1000);

        //Our animation loop begins here

        floatDown.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                timeLabel.animate().alpha(.7f).setInterpolator(new AccelerateDecelerateInterpolator()).setDuration(1000);
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                timeLabel.startAnimation(floatUp);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        floatUp.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                timeLabel.animate().alpha(1f).setInterpolator(new AccelerateDecelerateInterpolator()).setDuration(1000);
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                timeLabel.startAnimation(floatDown);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });


        //Start animation
        timeLabel.startAnimation(floatUp);


        //More animations, done using the newer way
        GraphicsHandler.fadeView(1000, .7f, 1f, true, pauseButton, this);


        //For Time View
        timeView = (TextView) findViewById((R.id.GAME_Timeview));
        timeView.setTextSize(Settings.spUnit, 30);
        timeView.setTypeface(settings.getFont(Settings.Property.Font_PrimaryLabel));


        //for scoreLabel
        scoreLabel = (TextView) findViewById(R.id.GAME_Scorelabel);
        scoreLabel.setTextSize(Settings.spUnit, Settings.secondaryHeaderFontSizeSP);
        scoreLabel.setTypeface(settings.getFont(Settings.Property.Font_Title));

        //for scoreView
        scoreView = (TextView) findViewById(R.id.GAME_Scoreview);
        scoreView.setTextSize(Settings.spUnit, Settings.secondaryHeaderFontSizeSP);
        scoreView.setTypeface(settings.getFont(Settings.Property.Font_PrimaryLabel));


        //animation


        final AlphaAnimation fadeOut = new AlphaAnimation(1f, .01f);
        fadeOut.setDuration(500);
        fadeOut.setInterpolator(new AccelerateDecelerateInterpolator());

        fadeOut.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                timeView.animate().alpha(1f).setDuration(500).setInterpolator(new AccelerateDecelerateInterpolator()).setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        timeView.startAnimation(fadeOut);
                    }
                });
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });


        //Once you attach the engine, the GameLoop starts
        engine = LogicEngine.getInstance(this, false);

        engine.switchMode(LogicEngine.Mode.GAME, this);



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
            //Context for ease
            final Context actContext = this;


            //Style it BABYY

            Settings settings = SettingsManager.get(actContext);
            assert settings != null;
            if (settings.getFont((Settings.Property.Font_PrimaryLabel)) != null) {
                resumeButton.setTypeface(settings.getFont(Settings.Property.Font_PrimaryLabel));
                restartButton.setTypeface(settings.getFont(Settings.Property.Font_PrimaryLabel));
                exitButton.setTypeface(settings.getFont(Settings.Property.Font_PrimaryLabel));
            }
            resumeButton.setTextColor(settings.primaryLabelColor);
            restartButton.setTextColor(settings.primaryLabelColor);
            exitButton.setTextColor(settings.primaryLabelColor);

            resumeButton.setTextSize(Settings.spUnit, Settings.secondaryLabelFontSize);
            restartButton.setTextSize(Settings.spUnit, Settings.secondaryLabelFontSize);
            exitButton.setTextSize(Settings.spUnit, Settings.secondaryLabelFontSize);


            resumeButton.setTypeface(settings.getFont(Settings.Property.Font_PrimaryLabel));
            restartButton.setTypeface(settings.getFont(Settings.Property.Font_PrimaryLabel));
            exitButton.setTypeface(settings.getFont(Settings.Property.Font_PrimaryLabel));

            //done?

            //Animations
            GraphicsHandler.floatView(1100, .05f, 0, true, resumeButton, actContext);
            GraphicsHandler.floatView(1000, .05f, 0, true, restartButton, actContext);
            GraphicsHandler.floatView(1200, .05f, 0, true, exitButton, actContext);


            //Listeners
            resumeButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showPauseMenu(); //call this again to hide the menu
                }
            });
            restartButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    try {
                        //Hide menu
                        pauseMenu.setVisibility(View.GONE); //Hide the menu (DOES NOT GET RID OF THE MEMORY!
                        //Request engine to reset
                        engine.order("Game-Reset", actContext);
                        //Switch the mode
                        engine.switchMode(LogicEngine.Mode.RESUMING, actContext);

                        //Reset state
                        pauseMenuShowing = false;




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
            pauseMenuShowing = true;
            Log.i("DEBUG => ", "Requested initial mode switch ");

            //If all this is already defined
        } else {
            Log.i("DEBUG", "STATE OF ENGINE =>" + LogicEngine.State());
            if (pauseMenuShowing) {
                pauseMenu.setVisibility(View.GONE); //Hide the menu (DOES NOT GET RID OF THE MEMORY!
                engine.switchMode(LogicEngine.Mode.RESUMING, this);
                pauseMenuShowing = false;

            } else {

                pauseMenu.setVisibility(View.VISIBLE);
                engine.switchMode(LogicEngine.Mode.PAUSING, this);
                pauseMenuShowing = true;


            }

        }

    }



    //These are not game methods! Overridden Android base methods do not TOUCH!
    @Override
    protected void onPause() {
        if (LogicEngine.State() != LogicEngine.Mode.GAMEOVER) {
            showPauseMenu();
            Log.d("Act:Gamescreen", "User has left the app!");
        }

        super.onPause();

    }

    @Override
    public void onBackPressed() {
        if (LogicEngine.State() != LogicEngine.Mode.GAMEOVER)
            showPauseMenu();
    }


    @Override
    protected void onResume() {
        //Set it fullscreen everytime the app is put back on the screen!
        int UI_OPTIONS = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;
        getWindow().getDecorView().setSystemUiVisibility(UI_OPTIONS);

        super.onResume();
    }
}
