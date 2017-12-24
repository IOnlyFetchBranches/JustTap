package com.justtap;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewStub;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.justtap.comp.GraphicsHandler;
import com.justtap.comp.models.Communicator;
import com.justtap.comp.models.Settings;
import com.justtap.comp.models.SettingsManager;
import com.justtap.comp.models.adapts.MainMenuAdapter;
import com.justtap.comp.models.sec.CredentialManager;

import java.util.concurrent.Executors;

import static com.justtap.utl.Printers.logGlobal;

public class MainMenu extends AppCompatActivity {
    static Settings settings;
    //Debug booleans
    private static boolean testingDone = false;
    private static boolean firstTime = true;
    //Control Booleans for standard anon class logic
    private static boolean isNotLeft, isNotRight, leftGone, rightGone;
    private static View welcomeScreen;
    private Communicator com;
    private Thread tester;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main_menu);

        //initialize our settings for the entire program
        if (firstTime) {
            settings = SettingsManager.get(this);
        }

        //Now settings should be loaded for the entire class;


        //Handle Menu Text Fonts here
        final TextView titleLabel = (TextView) this.findViewById(R.id.MENU_Titleview);
        //Set the title font, Button Fonts are set by their respective fragments!
        titleLabel.setTypeface(settings.getFont(Settings.Property.Font_Title));
        titleLabel.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 72);


        //Here we create our menu view
        final ViewPager menu = (ViewPager) findViewById(R.id.MENU_menupager);
        //Now we create and attach our adapter and set the default option
        PagerAdapter adapter = new MainMenuAdapter(getSupportFragmentManager(), 3); //Uses default fragment manager /w 3 options
        menu.setAdapter(adapter);
        //Starts it at 1, which is our MIDDLE option. [Play]
        menu.setCurrentItem(1);
        //Disable until infinite scroll works properly
        menu.setOverScrollMode(View.OVER_SCROLL_NEVER);

        //Define all our buttons

        ImageButton settingsButton = (ImageButton) findViewById(R.id.MENU_settings_button);
        ImageButton leaderboardsButton = (ImageButton) findViewById(R.id.MENU_leaderboards_button);
        ImageButton playButton = (ImageButton) findViewById(R.id.MENU_play_button);
        //Grab the context in case it needs use;
        final Context context = this;


        //Define Any animations that lie outside of fragments here
        final TranslateAnimation floatDown = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF, 0.0f,
                Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF, .50f);
        floatDown.setInterpolator(new AccelerateDecelerateInterpolator());
        floatDown.setDuration(1500);


        final TranslateAnimation floatUp = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF, 0.0f,
                Animation.RELATIVE_TO_SELF, .50f, Animation.RELATIVE_TO_SELF, 0.0f);
        floatUp.setInterpolator(new AccelerateDecelerateInterpolator());
        floatUp.setDuration(1500);

        //Our animation loop begins here

        floatDown.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                titleLabel.animate().scaleXBy(-.1f).setInterpolator(new AccelerateDecelerateInterpolator()).setDuration(1500);
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                titleLabel.startAnimation(floatUp);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        floatUp.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                titleLabel.animate().scaleXBy(.1f).setInterpolator(new AccelerateDecelerateInterpolator()).setDuration(1500);
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                titleLabel.startAnimation(floatDown);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });

        //End of title animations
        //Animate arrows

        final ImageView leftArrow, rightArrow;
        leftArrow = (ImageView) findViewById(R.id.MENU_LeftArrow);
        rightArrow = (ImageView) findViewById(R.id.MENU_RightArrow);

        final TranslateAnimation leftArrowOut, leftArrowIn, rightArrowOut, rightArrowIn;

        leftArrowOut = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0f, Animation.RELATIVE_TO_SELF, .25f, Animation.RELATIVE_TO_SELF, 0f, Animation.RELATIVE_TO_SELF, 0f);
        leftArrowIn = new TranslateAnimation(Animation.RELATIVE_TO_SELF, .25f, Animation.RELATIVE_TO_SELF, 0f, Animation.RELATIVE_TO_SELF, 0f, Animation.RELATIVE_TO_SELF, 0f);
        rightArrowOut = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0f, Animation.RELATIVE_TO_SELF, -.25f, Animation.RELATIVE_TO_SELF, 0f, Animation.RELATIVE_TO_SELF, 0f);
        rightArrowIn = new TranslateAnimation(Animation.RELATIVE_TO_SELF, -.25f, Animation.RELATIVE_TO_SELF, 0f, Animation.RELATIVE_TO_SELF, 0f, Animation.RELATIVE_TO_SELF, 0f);

        leftArrowOut.setInterpolator(new AccelerateDecelerateInterpolator());
        leftArrowOut.setDuration(750);
        leftArrowIn.setInterpolator(new AccelerateDecelerateInterpolator());
        leftArrowIn.setDuration(750);
        rightArrowOut.setInterpolator(new AccelerateDecelerateInterpolator());
        rightArrowOut.setDuration(750);
        rightArrowIn.setInterpolator(new AccelerateDecelerateInterpolator());
        rightArrowIn.setDuration(750);

        //End of Arrow animations


        //Initialize our control booleans first!, These track the logical state of our arrows
        isNotLeft = true;
        isNotRight = true;
        leftGone = false;
        rightGone = false;

        //Then we need some fade animations for our arrows
        final AlphaAnimation leftFadeOutAnimation = new AlphaAnimation(leftArrow.getAlpha(), 0f);
        final AlphaAnimation rightFadeOutAnimation = new AlphaAnimation(rightArrow.getAlpha(), 0f);
        final AlphaAnimation leftFadeInAnimation = new AlphaAnimation(leftArrow.getAlpha(), 1f);
        final AlphaAnimation rightFadeInAnimation = new AlphaAnimation(rightArrow.getAlpha(), 1f);

        //Of course we want interpolator and proper durations!
        leftFadeOutAnimation.setInterpolator(new AccelerateInterpolator());
        rightFadeOutAnimation.setInterpolator(new AccelerateInterpolator());
        leftFadeInAnimation.setInterpolator(new AccelerateInterpolator());
        rightFadeInAnimation.setInterpolator(new AccelerateInterpolator());

        leftFadeOutAnimation.setDuration(500);
        rightFadeOutAnimation.setDuration(500);
        leftFadeInAnimation.setDuration(500);
        rightFadeInAnimation.setDuration(500);


        //Next up we Add the logic to our page Viewer menu to hide the corresponding navigation arrow, Left/Right
        menu.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            //Bug here, mae sure you check the booleans are in the proper state before you change them
            //This is really the only method we are concerned about!
            //Note we hide the arrows by a fadeout!
            @Override
            public void onPageSelected(int position) {
                if (position == 0) {
                    isNotRight = true;

                    leftArrow.startAnimation(leftFadeOutAnimation);
                    leftArrow.setVisibility(View.INVISIBLE);
                    isNotLeft = false;
                    leftGone = true;
                } else if (position == 1) {
                    isNotLeft = true;
                    isNotRight = true;

                } else {
                    isNotLeft = true;
                    rightArrow.startAnimation(rightFadeOutAnimation);
                    rightArrow.setVisibility(View.INVISIBLE);
                    isNotRight = false;
                    rightGone = true;
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {
                if (state == ViewPager.SCROLL_STATE_IDLE) {
                    if (rightGone && isNotRight && rightArrow.getVisibility() != View.VISIBLE) {
                        rightArrow.setVisibility(View.VISIBLE);
                        rightArrow.startAnimation(rightFadeInAnimation);
                        rightArrow.startAnimation(rightArrowOut);
                        rightGone = false;
                    }
                    if (leftGone && isNotLeft && leftArrow.getVisibility() != View.VISIBLE) {
                        leftArrow.setVisibility(View.VISIBLE);
                        leftArrow.startAnimation(leftFadeInAnimation);
                        leftArrow.startAnimation(leftArrowOut);
                        leftGone = false;
                    }
                }
            }
        });


        //Loop animations here
        leftArrowOut.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                leftArrow.animate().alpha(.15f).setDuration(750).setInterpolator(new AccelerateInterpolator()).start();
            }

            @Override
            public void onAnimationEnd(Animation animation) {

                if (rightGone && isNotRight) {
                    rightArrow.animate().translationXBy(.25f);
                    rightArrow.setVisibility(View.VISIBLE);
                    rightArrow.startAnimation(rightFadeInAnimation);
                    rightArrow.startAnimation(rightArrowIn);
                }


                leftArrow.startAnimation(leftArrowIn);

            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        leftArrowIn.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                leftArrow.animate().alpha(1f).setDuration(750).setInterpolator(new AccelerateInterpolator()).start();
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                if (rightGone && isNotRight) {
                    rightArrow.setVisibility(View.VISIBLE);
                    rightArrow.startAnimation(rightFadeInAnimation);
                    rightArrow.startAnimation(rightArrowOut);
                    rightGone = false;
                }
                leftArrow.startAnimation(leftArrowOut);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        rightArrowOut.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                rightArrow.animate().alpha(0.15f).setDuration(750).setInterpolator(new AccelerateInterpolator()).start();
            }

            @Override
            public void onAnimationEnd(Animation animation) {

                if (leftGone && isNotLeft) {
                    leftArrow.animate().translationXBy(.25f);
                    leftArrow.setVisibility(View.VISIBLE);
                    leftArrow.startAnimation(leftFadeInAnimation);
                    leftArrow.startAnimation(leftArrowIn);
                }


                rightArrow.startAnimation(rightArrowIn);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        rightArrowIn.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                rightArrow.animate().alpha(1f).setDuration(750).setInterpolator(new AccelerateInterpolator()).start();
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                if (leftGone && isNotLeft) {
                    leftArrow.setVisibility(View.VISIBLE);
                    leftArrow.startAnimation(leftFadeInAnimation);
                    leftArrow.startAnimation(leftArrowOut);
                    leftGone = false;
                }

                rightArrow.startAnimation(rightArrowOut);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });


        //Start initial animations
        titleLabel.setAnimation(floatDown);
        leftArrow.startAnimation(leftArrowOut);
        rightArrow.startAnimation(rightArrowOut);


        //Trigger testing by logo tap DEBUG

        titleLabel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!tester.isAlive())
                    tester.start();
                else if (tester.isAlive() && !testingDone) {
                    testingDone = true;
                } else {
                    testingDone = true;
                }

            }
        });

        //Test thread for ui testing
        tester = new Thread(new Runnable() {
            @Override
            public void run() {
                //Unit test for the menu view
                try {
                    while (true) {
                        Thread.sleep(500);

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                menu.setCurrentItem(2);
                            }
                        });

                        Thread.sleep(250);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                menu.setCurrentItem(1);
                            }
                        });
                        Thread.sleep(100);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                menu.setCurrentItem(0);
                            }
                        });
                        Thread.sleep(500);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                menu.setCurrentItem(1);
                            }
                        });

                        while (testingDone) {

                        }
                    }
                } catch (InterruptedException ie) {
                    //do nothing
                }
            }
        });


        // Start communication protocols
        Log.i("MAIN =>", "Loading Network Components");
        //Get a communicator and initialize it
        com = Communicator.getInstance();


        if (firstTime) {
            //Start async wait task, to monitor for connection, and show login!
            if (!CredentialManager.getInstance(this).credentialsLoaded()) {
                Log.e("MAIN =>", "Waiting for auth request");
                StartLogon(this);
            }
        }







        logGlobal("Started Main");
        //Mark that the menu has been started before
        if (firstTime) {
            firstTime = false;
        }


    }


    //The init of the logon process
    private void StartLogon(final Context context) {


        Executors.newSingleThreadExecutor().submit(new Runnable() {
            @Override
            public void run() {
                //Execute on another thread for async
                try {
                    while (!com.readyForLogin()) {
                        Thread.sleep(100);
                        //Wait for server to send back login request
                    }


                    Log.e("DEBUG =>", " Attempting inflate!");

                    //once it receives, throw up the login screen!
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            final ViewStub welcomeStub = (ViewStub) findViewById(R.id.MENU_WelcomeStub);
                            welcomeStub.setLayoutResource(R.layout.menu_welcomelogin);

                            welcomeScreen = welcomeStub.inflate();
                            //Welcome screen is inflated now

                            //Associate with objects and style appropriately

                            //Welcome Text Styling...
                            final TextView welcomeHeader = (TextView) findViewById(R.id.MENU_Welcome_Header);
                            welcomeHeader.setClickable(false);
                            welcomeHeader.setTypeface(settings.getFont(Settings.Property.Font_PrimaryLabel));
                            welcomeHeader.setTextSize(Settings.spUnit, Settings.primaryHeaderFontSizeSP);

                            //Animate this like the title
                            GraphicsHandler.floatView(1500, .15f, .15f, true, welcomeHeader, context);

                            //End animations


                            TextView welcomeText = (TextView) findViewById(R.id.MENU_Welcome_Message);
                            welcomeText.setClickable(false);
                            welcomeText.setTypeface(settings.getFont(Settings.Property.Font_PrimaryLabel));

                            //Define Login Button
                            Button loginButton = (Button) findViewById(R.id.MENU_Welcome_LoginButton);
                            loginButton.setBackground(getDrawable(R.drawable.linerec));
                            loginButton.setTypeface(settings.getFont(Settings.Property.Font_PrimaryLabel));
                            loginButton.setTextSize(Settings.spUnit, Settings.secondaryHeaderFontSizeSP);

                            //Animations
                            GraphicsHandler.floatView(1500, .05f, 0, false, loginButton, context);


                            loginButton.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    //For now nothing...
                                }
                            });
                            //Define Create Account Button
                            Button createAccountButton = (Button) findViewById(R.id.MENU_Welcome_NewButton);
                            createAccountButton.setBackground(getDrawable(R.drawable.linerec));
                            createAccountButton.setTypeface(settings.getFont(Settings.Property.Font_PrimaryLabel));
                            createAccountButton.setTextSize(Settings.spUnit, Settings.secondaryHeaderFontSizeSP);

                            //Animations
                            GraphicsHandler.floatView(1500, .05f, 0, false, createAccountButton, context);

                            createAccountButton.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    //Trigger account creation  screen!
                                }
                            });


                            Button skipButton = (Button) findViewById(R.id.MENU_Welcome_SkipButton);
                            skipButton.setBackground(getDrawable(R.drawable.linerec));
                            skipButton.setTypeface(settings.getFont(Settings.Property.Font_PrimaryLabel));
                            skipButton.setTextSize(Settings.spUnit, Settings.secondaryHeaderFontSizeSP);

                            GraphicsHandler.floatView(1500, .05f, 0, false, skipButton, context);

                            skipButton.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    //Trigger exit prompt
                                    welcomeScreen.setVisibility(View.GONE);
                                }
                            });

                        }
                    });


                } catch (Exception e) {
                    Log.e("MAIN =>", "Logon Error!\n" + e.getLocalizedMessage());
                }


            }
        });




    }


    //Override the back button to prevent breaking

    @Override
    public void onBackPressed() {
        System.exit(0);
        super.onBackPressed();
    }
}
