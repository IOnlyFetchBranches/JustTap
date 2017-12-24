package com.justtap.comp.models;

import android.content.Context;
import android.util.Base64;
import android.util.Log;

import com.justtap.comp.models.sec.CredentialManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPublicKeySpec;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeoutException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

/**
 * This class will define all methods for communication with the backend API
 * ALL Jsons are to be retrieved from the JSONFactory abstract class!
 */

public class Communicator {


    //Singleton
    private static Communicator instance;

    //Communication properties
    private static String serverAdr = "192.168.1.17"; ///Server address
    //Vars from server
    PublicKey publicKey;
    //Internals
    private Socket connection;
    private Cipher rsaCrypt, aesCrypt;
    private LinkedBlockingQueue<Request> inbox = new LinkedBlockingQueue<>();
    private CredentialManager creds;
    //Controls
    private boolean waitingOnLogin = false;
    private boolean connectionEstablished = false;

    //Private constructor
    @SuppressWarnings("InfiniteLoopStatement")
    private Communicator() {


        //Connect to the server;
        Log.i("DEBUG =>", " Started communicator!");

        //This next section is not finished yet, so as to not break my app when not connected to my computer....
        try {
            //ping the servers test port! not 80 lol.
            if (!isReachable(serverAdr, 7070, 3000)) {
                throw new IOException("Server is unreachable!");
            }

            //Proceed with connection
            connection = new Socket(serverAdr, 80);
            Log.i("DEBUG =>", " Connected! ");
            connectionEstablished = true;

        } catch (IOException ioe) {
            Log.e("Communication =>", "Unable to connect to server! \n" + ioe.getLocalizedMessage());
        }
        //This is the initial response chain that gets the public key.
        if (connection != null) {
            try {
                //Start the initial request to get the public key, used for all communication;
                startInitialRequest();
                Log.i("Communicator =>", "Sent first request.");
                //Wait
                waitResponse(3000);
                //Handler response
                readInitialResponse();
                //After this the public key has been recieved and set on our cipher
                //Already tested encode() method, the public key is properly parsed
                //Now just need to send a response back to server;
                Thread.sleep(100); //always allow time for server to catchup
                sendTestRequest(); //send test request
                //The test request goes through perfecto, From here we start the base communicator loop

                Executors.newSingleThreadExecutor().submit(new Runnable() {
                    @Override
                    public void run() {
                        //Base Loop
                        try {
                            while (true) {

                                checkInbox(); //Check message queue
                                String responseJson = waitForResponse(); //Waits for and decodes response
                                if (responseJson != null) {
                                    handleResponse(responseJson);
                                }
                            }
                        } catch (Exception e) {
                            Log.e("Communicator =>", " Error in  main com loop :\n" + e.getLocalizedMessage());
                        }
                    }
                });


            } catch (Exception te) {
                Log.e("Communicator => ", te.getLocalizedMessage());
            }

            //after this completes we should have the initial key
        }

    }

    //Get the singleton
    public static Communicator getInstance() {
        if (instance == null) {
            Executors.newSingleThreadExecutor().submit(new Runnable() {
                @Override
                public void run() {
                    instance = new Communicator();
                }
            });

            while (instance == null) {
                //wait for the task to complete
            }

            //When finished return
            return instance;
        } else
            return null;
    }

    //End base methods

    //THE BUILT IN METHOD IS BROKEN
    //Thanks to SOV
    private static boolean isReachable(String addr, int openPort, int timeOutMillis) {
        // Any Open port on other machine
        // openPort =  22 - ssh, 80 or 443 - webserver, 25 - mailserver etc.
        try {
            try (Socket soc = new Socket()) {
                soc.connect(new InetSocketAddress(addr, openPort), timeOutMillis);
            }
            return true;
        } catch (IOException ex) {
            return false;
        }
    }

    //Does exactly what you think.
    public boolean isConnectionEstablished() {
        return connectionEstablished;
    }

    //JSONRequest Methods
    private void waitResponse() {
        try {
            InputStreamReader reader = new InputStreamReader(connection.getInputStream());

            while (!reader.ready()) {
                Thread.sleep(100);
                //while there are no bytes wait
            }
            Log.i("Communicator =>", "Incoming Message!");
        } catch (IOException | InterruptedException ioe) {
            Log.e("COMMUNICATOR =>", ioe.getLocalizedMessage());

        }
    }


    //JSON Response handlers

    private void waitResponse(long timeoutinmillis) throws TimeoutException {
        try {
            InputStreamReader reader = new InputStreamReader(connection.getInputStream());
            long time = 0;
            while (!reader.ready()) {
                Thread.sleep(100);
                time += 100;
                //while there are no bytes wait
                if (time > timeoutinmillis) {
                    throw new TimeoutException("Response Wait Time Exceeded!");
                }
            }
            Log.i("Communicator =>", "Incoming Message!");
        } catch (IOException | InterruptedException ioe) {
            Log.e("COMMUNICATOR =>", ioe.getLocalizedMessage());

        }
    }

    //Will wait for and return a readable response.
    private String waitForResponse() throws TimeoutException {
        try {
            InputStreamReader reader = new InputStreamReader(connection.getInputStream());
            long time = 0;
            while (!reader.ready()) {
                Thread.sleep(100);
                if (!inbox.isEmpty()) {
                    return null; //break on inbox recieved
                }
                //Else
                time += 100;//Just wait
                if (time % 10000 == 0)
                    Log.i("Communicator =>", "Waiting response for " + time / 1000 + " secs.");

                if (!inbox.isEmpty()) {
                    return null; //break on inbox recieved
                }
            }

            byte[] containerJson = new byte[1500];
            connection.getInputStream().read(containerJson);

            Log.e("DEBUG =>", "INCOMING MESSAGE SIZE " + new String(containerJson));


            //decrypt it...
            try {
                return unboxContainerJson(containerJson).toString();
            } catch (Exception e) {
                Log.e("Communicator =>", "Error unpacing container! \n" + e.getLocalizedMessage());
                return null;
            }
            //Done
        } catch (IOException | InterruptedException ioe) {
            Log.e("COMMUNICATOR =>", ioe.getLocalizedMessage());
            return null;
        }


    }

    //MAIN HANDLER;
    private void handleResponse(String response) throws JSONException {
        if (response != null) {
            try {
                JSONObject json = new JSONObject(response);

                String type = jGet("type", json);
                //Define handlers for ALL POSSIBLE TYPES HERE
                switch (type) {
                    case "requestAuthentication":
                        //This triggers main logon process if not already logged in!
                        Log.e("Communicator =>", "Server requested authentication!");
                        waitingOnLogin = true;
                        break;
                    case "credentialsAccepted":
                        //Here you would need to parse the uid + token fields, they will be used to reconnect
                        break;
                    case "credentialsRejected":
                        //Here you would need to deal with a bad token/etcetc
                        break;
                    case "sessionTokenAccepted":
                        //here you would do nothing really, the server will continue communicating
                        break;
                    case "userStats":
                        //here you could expect a json with fields that correspond to user stats
                        //again just parse and deliver to the appropriate component
                        break;
                    default:
                        Log.e("Communicator =>", "Invalid Response recieved of type: " + type);
                }


            } catch (JSONException jse) {
                throw new JSONException(jse.getLocalizedMessage());
            }
        }
    }

    //This method starts the send/respond chain for logging in a NEW user

    private void startInitialRequest() {
        JSONObject json = JSONFactory.initialKeyRequest();
        try {
            connection.getOutputStream().write(json.toString().getBytes());
        } catch (IOException ioe) {
            Log.e("Communicator =>", ioe.getLocalizedMessage());
        }
    }

    private void readInitialResponse() throws Exception {
        try {
            //Always use an input stream reader!
            InputStreamReader reader = new InputStreamReader(connection.getInputStream());
            //Read to a char buffer
            char[] buffer = new char[1000];
            if (reader.ready()) {
                int read = reader.read(buffer);
            }
            //retrieve encoded message
            String responseEncoded = new String(buffer);


            //decode message finally
            String response = new String(Base64.decode(responseEncoded.getBytes(), Base64.DEFAULT));


            //Parse into json
            JSONObject json = wrapJSON(response);
            String type = jGet("type", json);
            String message = jGet("message", json);

            //Our response should be a proper json
            Log.e("DEBUG =>", "Server Response Type=  " + type);

            //Assign the public key to the crypto
            if (type != null && type.contains("acceptConversation")) {
                if (message != null) {

                    String mod, exp;
                    mod = message.substring(0, message.lastIndexOf("=") + 1);
                    exp = message.substring(message.lastIndexOf("=") + 1, message.length());
                    try {
                        //Log.e("DEBUG =>","Key response  =>"+mod+" \nexp "+exp);
                        InitCrypt(mod, exp);

                    } catch (Exception e) {
                        Log.e("Communicator =>", "Crypt Error " + e.getLocalizedMessage());
                    }

                    Log.i("Communicator =>", "Crypt Init'd");
                }
            } else {
                throw new Exception("Was Expecting different server response type! \nneeded:acceptConversation" +
                        "\ngot:" + type);
            }


        } catch (IOException ioe) {
            Log.e("Communicator =>", ioe.getLocalizedMessage());
        }
    }

    /**
     * More clarification about the Logon processes
     * The jsons you need to send in the Communicator process methods
     * ARE ALWAYS OBTAINED From a credential, which is itself a json object*
     * You get the proper json from the credential manager and respond as needed
     * <p>
     * RU ALL NETWORK COMMANDS ON NOT THE MAIN THREAD!!!!
     */
    public void startInitialLogonProcess(Context mainMenuContext, String user, String pass, String email) throws JSONException {
        creds = CredentialManager.getInstance(mainMenuContext); //Grab instance of Credential Manager
        final byte[] logonRequest = creds.getSecureJSONForNewLogin(user, pass, email); //returns user credentials securely.

        //Do this OFF THE MAIN THREAD!!!!!
        //As per the documentation all network ops happen off main thread!
        Executors.newSingleThreadExecutor().submit(new Runnable() {
            @Override
            public void run() {
                try {
                    connection.getOutputStream().write(logonRequest); //write the logon request
                    Log.i("Communicator =>", "Sent initial logon!");
                } catch (IOException e) {
                    Log.e("Communicator =>", "Cannot write initial logon request.\n" + e.getLocalizedMessage());
                }
            }
        });

        //at this point the ma

    }

    private void sendTestRequest() throws IOException {
        JSONObject testJson = JSONFactory.testRequest();
        send(testJson);
    }

    //Returns true when the server has responded
    public boolean readyForLogin() {
        return waitingOnLogin;
    }

    //Default send message...
    private void send(JSONObject json) {
        try {
            connection.getOutputStream().write(prepareJsonForSending(json.toString()));
            Log.i("Communicator=> ", "Sent Message!");
        } catch (IOException | JSONException ioe) {
            Log.e("Communicator =>", " Error: " + ioe.getLocalizedMessage());
        }
    }

    //Internal Methods

    //Called only ONCE from the initial response handler!
    private void InitCrypt(String mod, String exp) throws NoSuchAlgorithmException,
            InvalidKeySpecException, InvalidKeyException, NoSuchPaddingException, NoSuchProviderException {
        if (rsaCrypt == null) {
            //Get our RSA instance with OAEP
            rsaCrypt = Cipher.getInstance("RSA/ECB/OAEPWithSHA-1AndMGF1Padding");
            //Get our AES Instance CBC PKCS5 padding (Compatible with pkc57 block size defs!!!
            aesCrypt = Cipher.getInstance("AES/CBC/PKCS5Padding");


            //Decode base 64 to hex, the mod is really a base64 encoded hex byte array!
            String hexMod = String.format("%040x", new BigInteger(1, Base64.decode(mod, Base64.DEFAULT)));
            String hexExp = String.format("%040x", new BigInteger(1, Base64.decode(exp, Base64.DEFAULT)));

            //Convert to bigInt
            BigInteger decMod = new BigInteger(hexMod, 16);
            BigInteger decExp = new BigInteger(hexExp, 16);

            //Log.e("DEBUG =>",decExp+ "");
            //Create the key model
            RSAPublicKeySpec publicKeySpec = new RSAPublicKeySpec(decMod, decExp);
            //Convert key model class to usable PublicKey
            KeyFactory keygen = KeyFactory.getInstance("RSA");
            publicKey = keygen.generatePublic(publicKeySpec);
            //Init the rsaCrypt
            rsaCrypt.init(Cipher.ENCRYPT_MODE, publicKey);


        }
    }

    //Check messages
    private void checkInbox() {
        if (inbox.size() != 0) {
            Log.i("Communicator =>", "Handling Request...");
            for (Request request : inbox) {
                switch (request) {
                    case CloseConnection:
                        try {
                            close();
                        } catch (IOException e) {
                            Log.e("Communicator =>", "Error handling close request: " + e.getLocalizedMessage());
                        }
                        break;

                    case Reconnect:
                        //Nothing yet.
                        //This will reconnect to the server with credentials
                        //Special type for this.
                        break;

                    default:
                        Log.i("Communicator =>", "Cannot handle request at the moment! ");

                }
                inbox.remove(request);
            }
        }
    }

    //Place a message
    public void order(Request request) {
        inbox.add(request);
        //Done
    }

    //Encode the key to be attached to the cipher.
    private byte[] encodeKey(byte[] keyBytes) {
        if (rsaCrypt != null) {


            try {
                //encrypt the message.
                byte[] cryptedKey = rsaCrypt.doFinal(keyBytes);
                //encode
                Log.i("Communicator =>", "Encypted successfully! " + cryptedKey.length);
                return cryptedKey;

            } catch (Exception e) {
                Log.e("Communicator =>", " Error Encoding: " + e.getLocalizedMessage());
                return null;
            }
        } else {
            return null;
        }
    }

    private IvParameterSpec genIV() throws NoSuchAlgorithmException {
        SecureRandom randomSecureRandom = SecureRandom.getInstance("SHA1PRNG");
        byte[] iv = new byte[aesCrypt.getBlockSize()];
        randomSecureRandom.nextBytes(iv);

        return new IvParameterSpec(iv);
    }

    //Returns AES ciphertext
    private byte[] encodePayloadWithAES(String json, IvParameterSpec iv)
            throws NoSuchAlgorithmException, InvalidKeyException, BadPaddingException,
            IllegalBlockSizeException, InvalidAlgorithmParameterException {
        KeyGenerator keygen = KeyGenerator.getInstance("AES");
        keygen.init(256);
        SecretKey key = keygen.generateKey();
        //Encode the key for safe keeping
        CredentialManager.saveKeyForTransfer(encodeKey(key.getEncoded()));

        //Init cipher with our key
        aesCrypt.init(Cipher.ENCRYPT_MODE, key, iv);
        return aesCrypt.doFinal(json.getBytes());
    }

    //Fully Wraps a Json for secure transfer;
    public byte[] prepareJsonForSending(String json) throws JSONException {
        JSONObject container = JSONFactory.getTransportJSON();
        try {
            //generate IV FOr extra sec;
            IvParameterSpec IV = genIV();
            byte[] payload = encodePayloadWithAES(json, IV);
            byte[] key = CredentialManager.loadKeyForTransfer();
            String encodedPayload = Base64.encodeToString(payload, Base64.DEFAULT);
            String encodedKey = Base64.encodeToString(key, Base64.DEFAULT);

            //After converting to B64, store in container json
            container.put("key", encodedKey);
            container.put("payload", encodedPayload);
            container.put("iv", Base64.encodeToString(IV.getIV(), Base64.DEFAULT));

            Log.e("DEBUG =>", container.toString());

            return container.toString().getBytes();
        } catch (NoSuchAlgorithmException | InvalidKeyException | BadPaddingException
                | IllegalBlockSizeException | InvalidAlgorithmParameterException e) {
            Log.e("Communicator =>", "Error wraping JSON \n" + e.getLocalizedMessage());
            return null;
        }

    }

    private JSONObject unboxContainerJson(byte[] containerBytes) throws JSONException, BadPaddingException, IllegalBlockSizeException, InvalidKeyException, InvalidAlgorithmParameterException {

        JSONObject jsonContainer = new JSONObject(new String(containerBytes));
        String payload = new String(Base64.decode(jsonContainer.getString("payload"), Base64.DEFAULT));
        //Convert to usable object
        JSONObject json = new JSONObject(payload);
        Log.e("DEBUG =>", " Unpacked Container " + json.toString());


        return json;


    }

    public boolean close() throws IOException {
        connection.close();
        return connection.isClosed();
    }

    //Send this method a string and it returns a JSON Object
    private JSONObject wrapJSON(String json) {
        try {
            JSONObject jsonObj = new JSONObject(json);
            return jsonObj;
        } catch (JSONException jse) {
            Log.e("Communicator => ", jse.getLocalizedMessage());
            return null;
        }
    }

    //Get from JSON
    private String jGet(String field, JSONObject json) {
        try {

            //Attempt to get the string
            //Return the string value
            return json.getString(field);
        } catch (JSONException jse) {
            Log.e("Communicator => ", jse.getLocalizedMessage());
            return null;
        }
    }

    //
    //Maybe needed in the future???
    public enum Request {
        Reconnect,
        CloseConnection,
        Login(),
        PlayerData,
        UpdatePlayerData


    }

}
