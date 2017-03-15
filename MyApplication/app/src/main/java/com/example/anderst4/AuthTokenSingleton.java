package com.rosehulman.realbotics;

import android.os.AsyncTask;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by efronbs on 2/4/2017.
 */

public class AuthTokenSingleton {

    private static AuthTokenSingleton _instance = null;
    private String accessToken = "";
    private String refreshToken = "";
    private String timeToLive = "";
    private String username = "";
    private String password = "";

    // not thread safe
    public static AuthTokenSingleton get_instance() {
        if (_instance == null) {
            _instance = new AuthTokenSingleton();
        }

        return _instance;
    }

    private AuthTokenSingleton() {
    }

    /**
     * ALL - this is the only setter because your really should never get one piece of an auth token without the rest, so we just set
     * them all at once.
     *
     * @param accessToken
     * @param expiresIn
     * @param refreshToken
     */
    public void setToken(String accessToken, String expiresIn, String refreshToken) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.timeToLive = expiresIn;

//			System.out.println("Set token data is: ");
//			System.out.println("\naccessToken " + this.accessToken);
//			System.out.println("\nrefreshToken " + this.refreshToken);
//			System.out.println("\ntimeToLive " + this.timeToLive);

        // Todo implement result fetching and checking if another thread already exists
        RefreshTokenThread refreshTokenRunnable = new RefreshTokenThread(
                this.refreshToken,
                this.username,
                this.password,
                this.timeToLive);
        Thread t = new Thread(refreshTokenRunnable);
        t.start();
    }

    /**
     * Getter for access token
     *
     * @return
     */
    public String getAccessToken() {
        return this.accessToken;
    }


    public void setUnameAndPassword(String username, String password) {
        this.username = username;
        this.password = password;
    }


    private class RefreshTokenThread implements Runnable {
        String refreshToken;
        String username;
        String password;
        String timeToLive;

        public RefreshTokenThread(String refreshToken, String username, String password, String timeToLive) {
            this.refreshToken = refreshToken;
            this.username = username;
            this.password = password;
            this.timeToLive = timeToLive;
        }

        public void run() {
            try {
                Thread.sleep(900 * Integer.parseInt(this.timeToLive));
                RefreshTokenTask refreshTask = new RefreshTokenTask(this.refreshToken, this.username, this.password);
                refreshTask.execute();
                return;
            } catch (InterruptedException e) {
                System.out.println("Sleep thread interrupted before it woke up");
            }
        }
    }

    /**
     * Represents an asynchronous login/registration task used to authenticate
     * the user. This one in particular is used to refresh the user's auth token
     */
    private class RefreshTokenTask extends AsyncTask<Void, Void, LoginActivity.LoginStatus> {

        String refreshToken;
        String username;
        String password;

        String newAccessToken;
        String newRefreshToken;
        String newExpiresIn;

        RefreshTokenTask(String refreshToken, String username, String password) {
            this.refreshToken = refreshToken;
            this.username = username;
            this.password = password;

        }

        @Override
        protected void onPreExecute() {
        }

        //This function attempts a login with the credentials currently in the credentials fields
        @Override
        protected LoginActivity.LoginStatus doInBackground(Void... params) {
            //System.out.println("GOT HeRe!.!");
            DefaultHttpClient httpclient = new DefaultHttpClient();
            String path = ServerSingleton.AUTH_URL;//SERVER_URL + "/auth/authenticate/";

            //url with the post data
            HttpResponse response;
            HttpPost httpost = new HttpPost(path);

            StringEntity se;

            //convert parameters into JSON object
            List<NameValuePair> urlParameters = new ArrayList<NameValuePair>();
            urlParameters.add(new BasicNameValuePair("grant_type", "refresh"));
            //			urlParameters.add(new BasicNameValuePair("Authentication" , "Bearer " + refreshToken));
            urlParameters.add(new BasicNameValuePair("username", this.username));
            urlParameters.add(new BasicNameValuePair("password", this.password));

            System.out.println("refreshing with data: ");
            System.out.println("\nusername " + this.username);
            System.out.println("\npassword " + this.password);
            System.out.println("\nrefreshToken " + this.refreshToken);

            try {
                //passes the results to a string builder/entity
                se = new StringEntity(URLEncodedUtils.format(urlParameters, HTTP.UTF_8));
                //System.out.println("se = " + se);
            } catch (UnsupportedEncodingException e1) {
                e1.printStackTrace();
                return LoginActivity.LoginStatus.ERRORED;
            }
            //sets the post request as the resulting string
            httpost.setEntity(se);

            //sets a request header so the page receving the request
            httpost.setHeader("Content-Type", "application/x-www-form-urlencoded");
            httpost.setHeader("Authentication", "Bearer " + refreshToken);

            //Handles what is returned from the page
            try {
                System.out.println("Sending off refresh request");
                response = httpclient.execute(httpost);
            } catch (ClientProtocolException e) {
                e.printStackTrace();
                return LoginActivity.LoginStatus.ERRORED;
            } catch (IOException e) {
                e.printStackTrace();
                return LoginActivity.LoginStatus.ERRORED;
            }

            if (response.getStatusLine().getStatusCode() == 200) {

                String newParsedHTMLEntity;
                HttpEntity entity = response.getEntity();
                InputStream is = null;

                try {
                    is = entity.getContent();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(is, "iso-8859-1"), 8);
                    StringBuilder sb = new StringBuilder();
                    String line = null;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line + "\n");
                    }
                    is.close();

                    newParsedHTMLEntity = sb.toString();
                    JSONObject jObj = new JSONObject(newParsedHTMLEntity);
                    this.newAccessToken = jObj.getString("access_token");
                    this.newRefreshToken = jObj.getString("refresh_token");
                    this.newExpiresIn = jObj.getString("expires_in");

                    //					System.out.println("refreshing tokens with:");
                    //					System.out.println("\tnew access token: " + this.newAccessToken);
                    //					System.out.println("\tnew refresh token: " + this.newRefreshToken);
                    //					System.out.println("\tnew expires in: " + this.newExpiresIn);


                } catch (IOException e) {
                    e.printStackTrace();
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                return LoginActivity.LoginStatus.SUCCESSFUL;
            }
            if (response.getStatusLine().getStatusCode() == 401) {
                return LoginActivity.LoginStatus.TWOFACTOR;
            }

            System.out.println("refresh authentication failed");
            return LoginActivity.LoginStatus.FAILED;
        }

        //TODO implement the error cases. Two factor is something separate.
        @Override
        protected void onPostExecute(final LoginActivity.LoginStatus success) {

            if (success == LoginActivity.LoginStatus.SUCCESSFUL) {
                setToken(this.newAccessToken, this.newExpiresIn, this.newRefreshToken);
            } else if (success == LoginActivity.LoginStatus.FAILED) {
                // probably force logout
            } else if (success == LoginActivity.LoginStatus.TWOFACTOR) {
                // implement later
            } else {
                // something's gone really wrong.
            }
        }

        @Override
        protected void onCancelled() {
            // something's gone kind of wrong
        }
    }
}
