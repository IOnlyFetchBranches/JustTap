package com.justtap.comp.models;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.support.annotation.NonNull;
import android.util.TypedValue;

import java.io.Serializable;

/**
 * Our settings object
 * Will need to start migrating changable values over to this, so it can be easily changed
 * By using a global static variable.
 */

public class Settings implements Serializable {


    public static final int dpUnit = TypedValue.COMPLEX_UNIT_DIP;
    public static final int spUnit = TypedValue.COMPLEX_UNIT_SP;
    public static int titleLabelFontSize = 70; //dp
    public static int primaryLabelFontSize = 32; //Measured in dp
    public static int secondaryLabelFontSize = 20;//sp
    public static int primaryHeaderFontSizeSP = 24; // measured in sp
    public static int secondaryHeaderFontSizeSP = 20; //sp
    //Our singleton;
    private static Settings instance = null;
    //Start global prefs
    private static Typeface titleTypeFace, primaryLabelTypeFace, headerTypeFace, secondaryLabelTypeFace;
    //Color preferences
    public int primaryLabelColor = Color.BLACK;
    public int secondaryLabelColor = Color.WHITE;
    public int okColor = Color.parseColor("#D32F2F");
    public int goodColor = Color.parseColor("#212121");
    public int greatColor = Color.parseColor("#448AFF");
    public int excellentColor = Color.parseColor("#388E3C");


    private Settings(Context context) {
        //Settings constructor, here we define all of our globals.
        //A lot of them need context to reference resources so that is why it must be done here;
        //Define fonts;
        titleTypeFace = Typeface.createFromAsset(context.getAssets(), "fonts/freeish/BrainfishRush.ttf");
        headerTypeFace = Typeface.createFromAsset(context.getAssets(), "fonts/freeish/BrainfishRush.ttf");
        primaryLabelTypeFace = Typeface.createFromAsset(context.getAssets(), "fonts/free/Slabo-Smaller.ttf");
        secondaryLabelTypeFace = Typeface.createFromAsset(context.getAssets(), "fonts/free/Slabo-Smaller.ttf");

        if (titleTypeFace == null || headerTypeFace == null || primaryLabelTypeFace == null) {
            System.exit(1);
        }


    }

    static Settings getInstance(Context callingContext) {
        if (instance == null) {
            instance = new Settings(callingContext);
            return instance;
        } else {
            return instance;
        }

    }


    //Easy method to get values using properties
    public Typeface getFont(@NonNull Property property) {

        switch (property) {
            case Font_Title:
                return titleTypeFace;

            case Font_Header:
                return headerTypeFace;

            case Font_PrimaryLabel:
                return primaryLabelTypeFace;
            case Font_SecondaryLabel:
                return secondaryLabelTypeFace;

            default:
                throw new IllegalArgumentException("Invalid property given, please refer to documentation/comments!");
        }
    }


    public enum Property {
        Font_Title,
        Font_Header,
        Font_PrimaryLabel,
        Font_SecondaryLabel
    }


}
