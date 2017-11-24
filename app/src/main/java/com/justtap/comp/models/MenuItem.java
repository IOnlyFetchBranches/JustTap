package com.justtap.comp.models;

import android.content.Context;

import com.justtap.R;

/**
 * This is the model for the main menu scroll buttons
 */

public class MenuItem extends android.support.v7.widget.AppCompatImageButton {

    public MenuItem(String whichButton, Context context) {
        super(context);

        switch (whichButton) {
            case "NewGame":
                this.setImageDrawable(getResources().getDrawable(R.mipmap.menu_playbutton));
        }


    }

}
