package com.justtap;

import com.justtap.comp.LogicEngine;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {
    @Test
    public void correctStateLoaded() throws Exception {
        assertEquals(com.justtap.comp.LogicEngine.State(), LogicEngine.Mode.IDLE);
    }
}