package com.justtap.comp.models.sec;


import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.justtap.comp.models.Communicator;

import org.json.JSONException;
import org.json.JSONObject;

public class CredentialManager extends Credential {


    private static CredentialManager instance;// Singleton
    private static byte[] usrSymKey; // The user's encrypted aes key.
    //Booleans
    private static boolean sessionReady = false; //This triggers when a credential can pass cred.noNulls()
    //Preferences
    SharedPreferences prefs;
    private Credential userSession; //Users credentials


    private CredentialManager(Context context) {
        //Init the Credential manager with a protected inner method;
        super(Credential.newUnloadedCredential());

        //Engage preferences using context
        //Preference name is "sec";
        prefs = context.getSharedPreferences("sec", Context.MODE_PRIVATE);

        //Start a new session by default
        userSession = newUnloadedCredential();

        //Now we can "load" into the session;
        if (prefs.contains("uid")) {
            try {
                userSession = userSession.loadSession(prefs.getString("uid", null), prefs.getString("token", null)); //Load session


                if (userSession.noNulls()) {
                    //Old Crdentials have been loaded
                    sessionReady = true;
                } else {
                    Log.e("Credential Manager=>", " No valid Credentials could be loaded!");
                }
            } catch (JSONException jse) {
                Log.e("Credential Manager=>", "Error:" + jse.getLocalizedMessage());
            }
        }


    }

    public static void saveKeyForTransfer(byte[] encodedKey) {
        usrSymKey = encodedKey;
    }

    public static byte[] loadKeyForTransfer() {
        return usrSymKey;
    }

    public static CredentialManager getInstance(Context callingActivityContext) {
        if (instance == null) {
            instance = new CredentialManager(callingActivityContext);
            return instance;
        } else {
            return instance;
        }

    }

    public boolean saveCredential() throws JSONException {
        if (sessionReady) {
            prefs.edit().putString("uid", (String) userSession.get("uid")).apply();
            prefs.edit().putString("token", (String) userSession.get("token")).apply();
        }

        //Successful only if th default values dont get written...
        return prefs.getString("uid", null) != null &&
                prefs.getString("token", null) != null;
    }

    //The only jsons returned by not JSONFactory deal with credentials
    public byte[] getSecureJSONForNewLogin(String user, String pass, String email) throws JSONException {
        userSession.initCredential(user, pass);
        JSONObject json = userSession;

        json.put("type", "newusercreate");
        json.put("hash", userSession.getSecuredHash());
        json.put("email", email);

        //finally we can return the secured json with all necessary information and send it via the communicator.
        return Communicator.getInstance().prepareJsonForSending(json.toString());
        //Done


    }

    //Only other way to trigger sessionReady()
    //A proper session needs a UID and token
    public void assignToCred(String uid, String token) throws JSONException {
        userSession.loadSession(uid, token);
        sessionReady = userSession.noNulls();
    }

    public boolean credentialsLoaded() {
        return sessionReady;
    }


}
