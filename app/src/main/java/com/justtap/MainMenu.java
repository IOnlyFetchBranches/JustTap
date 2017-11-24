package com.justtap;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import static com.justtap.utl.Printers.logGlobal;

public class MainMenu extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_menu);


        logGlobal("Started Main");




    }

}
