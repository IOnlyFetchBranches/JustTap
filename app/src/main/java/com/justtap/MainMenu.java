package com.justtap;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.widget.TextView;

import com.justtap.act.Gamescreen_Tutorial;
import static com.justtap.utl.Printers.*;

public class MainMenu extends AppCompatActivity {



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_menu);
        logGlobal("Started Main");
        //We want to bypass the main menu for now
        Intent bypassMenu= new Intent(this, Gamescreen_Tutorial.class);
        startActivity(bypassMenu);
        //End of debug block




    }

}
