package com.justtap.utl;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class contains abstracted Printing/Logging methods
 */

public abstract class Printers {

    public static void logGlobal(String text){
        Logger.getGlobal().log(Level.INFO,text);
    }
    public static void logLevel(String text,Level level){
        Logger.getGlobal().log(level,text);
    }

    public static void logAnon(String text){
        Logger.getAnonymousLogger().log(Level.ALL,text);
    }

    public static void print(String text){
        System.out.println(text);
    }
}
