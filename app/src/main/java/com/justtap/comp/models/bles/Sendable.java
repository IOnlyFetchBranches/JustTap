package com.justtap.comp.models.bles;

/**
 * Defines basic methods common to all sendable objects.
 */

public interface Sendable {

    //This method should return a properly encoded byte[] array
    //All arrays need to be under 512 bytes if encrypted
    //All arrays should be encoded in base64 before sending
    byte[] toEncodedJson();

    String toJson();
}
