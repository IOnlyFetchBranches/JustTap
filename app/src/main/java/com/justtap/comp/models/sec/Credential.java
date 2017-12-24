package com.justtap.comp.models.sec;

import android.util.Base64;
import android.util.Log;

import com.justtap.comp.models.Communicator;
import com.justtap.comp.models.bles.Sendable;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 *
 */

class Credential extends JSONObject implements Serializable, Sendable {
    //ID
    private static long serialVersionUID = 105690L;
    private final String type = "credential";
    //These values dont need to persist;
    private transient String hash, username;
    //These is the only thing that get serialized;
    private String uid; //Also b64 encoded hash;
    private byte[] token; //base 64 encoded

    private Credential() {
    } //private constructor

    Credential(Credential blankCredential) {

    }

    protected static Credential newUnloadedCredential() {
        return new Credential();
    }

    //Credential JSON Structure
    //type, user, hash, email, token
    protected Credential initCredential(String username, String pass) throws JSONException {
        this.username = username;
        this.put("type", type);
        this.put("user", username);

        //Gen hash
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            byte[] hash = digest.digest(pass.getBytes()); //Make byte[] hash
            //Translate back to string
            for (byte b : hash) {
                this.hash += Integer.toHexString(b & 256) + " ";
            }


            return this;
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }

    }

    protected Credential loadSession(String uid, String token) throws JSONException {
        this.uid = uid;
        this.token = token.getBytes();
        this.put("uid", uid);
        this.put("token", token);
        return this;
    }

    //The server will respond with an encrypted base 64 encoded hash
    //this method will expect the base 64 unencrypted uid
    protected void assignUID(String uid) throws JSONException {
        this.uid = uid;
        this.put("uid", uid);
    }

    //Checks for crucial values UID and SessionToken
    protected boolean noNulls() {
        return uid != null && token != null;
    }

    //Assigns and returns the b64 encoded token;
    protected byte[] assignToken(String token) {
        this.token = Base64.encode(token.getBytes(), Base64.DEFAULT);
        try {
            //The token is safe to store in json;
            this.put("token", getEncodedToken());
        } catch (JSONException jse) {
            Log.e("Credential =>", " Error while writing to json. " + jse.getLocalizedMessage());
        }
        return this.token;
    }

    protected String getEncodedToken() {
        return new String(token);
    }

    protected String getSecuredHash() {
        return hash;
    }


    //Only this very safe already encrypted json will ocntain the user's hash!
    @Override
    public byte[] toEncodedJson() {
        try {
            //Store hash into safe JSON
            this.put("hash", hash);
            JSONObject json = this;
            this.remove("hash");

            //encode;
            Communicator mainOut = Communicator.getInstance();
            return mainOut.prepareJsonForSending(json.toString());


        } catch (JSONException jse) {
            Log.e("Credential =>", " Encoding error. " + jse.getLocalizedMessage());
            return null;
        }
    }

    @Override
    public String toJson() {
        return this.toString();
    }
}
