package com.justtap;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.justtap.act.Gamescreen_New;

import static com.justtap.utl.Printers.logGlobal;

public class MainMenu extends AppCompatActivity {

    public static boolean startInImmersiveMode = true; //This coontrols app startup

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_menu);


        //This activates immersive mode
        if (startInImmersiveMode)
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_IMMERSIVE);

        logGlobal("Started Main");
        //We want to bypass the main menu for now
        Intent bypassMenu = new Intent(this, Gamescreen_New.class);
        startActivity(bypassMenu);
        //End of debug block




    }

}
