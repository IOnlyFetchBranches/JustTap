package com.justtap.comp.models;

import android.content.Context;

/**
 * This class handles the save/load/call of settings
 */

public abstract class SettingsManager {

    //Returns an instance of a settings object;
    public static Settings get(Context callingActivityContext) {
        return Settings.getInstance(callingActivityContext);
    }


}

