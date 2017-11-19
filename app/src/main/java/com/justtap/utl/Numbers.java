package com.justtap.utl;

import java.util.Random;

/**
 * Generic Number methods, New so not implemented everywhere!
 */

public abstract class Numbers {
    private static Random generator = new Random();
    private static int count = 0;

    public static int genInt(int base, int limit) {
        if (count == 100) {
            generator.setSeed(System.nanoTime());
            count = 0;
        }
        count++;
        return generator.nextInt((limit - base) + 1) + base;
    }
}
