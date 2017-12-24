package com.justtap.comp.models;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * This is all the JSON Files the program uses
 */

public abstract class JSONFactory {

    //The veryFirst request that the App  sends to the backend
    //The type on this request is publicbKeyRequest
    public static JSONObject initialKeyRequest() {
        JSONObject request = new JSONObject();
        try {

            //response is acceptConversation, mess= key
            //request here is beginConversation
            request.put("type", "beginConversation");
            request.put("message", "null");
        } catch (JSONException jse) {
            Log.e("JSONFactory =>", jse.getLocalizedMessage());
        }
        return request;
    }

    static JSONObject testRequest() {
        JSONObject request = new JSONObject();
        try {

            //response is acceptConversation, mess= key
            //request here is beginConversation
            request.put("type", "test");
            request.put("message", "if you have received this everything works properly with RSA exchange");
        } catch (JSONException jse) {
            Log.e("JSONFactory =>", jse.getLocalizedMessage());
        }
        return request;
    }

    //After encrypting the AES key with RSA, you encrypt the message json in aes
    public static JSONObject getTransportJSON() {
        JSONObject request = new JSONObject();

        try {
            request.put("iv", "");
            request.put("payload", "");
            return request;
        } catch (JSONException e) {
            Log.e("JSONFactory =>", e.getLocalizedMessage());
        }
        return null;
    }

}
