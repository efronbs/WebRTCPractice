package com.example.anderst4;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

public class AuthenticationActivity extends Activity {

    public enum LoginStatus {
        SUCCESSFUL, FAILED, ERRORED
    }

    private String uEmail = "";
    private String uPass = "";

    public static boolean taskRunning = false;

    //This method is called when the activity is created. It pairs the variables in this file with the widgets on the login screen
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        attemptLogin();
    }

    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    public void attemptLogin() {

        if (taskRunning) {
            return;
        }

        taskRunning = true;

        // Kick off a background task to perform the user login attempt.
        UserLoginTask loginTask = new UserLoginTask(uEmail, uPass);
        loginTask.execute();
    }

    //If the login attempt was successful this method is called and it saves your credentials if requested and displays the logout message
    private void loginSuccessful(String email, String password){
        //This saves your credentials if requested
        //Which activity do we want for this to go to?
        /*
        Intent i = new Intent(this, DeviceListActivity.class);
        startActivity(i);
        */
        System.out.println("FINISH!");
        finish();
    }

    /**
     * Represents an asynchronous login/registration task used to authenticate
     * the user.
     */
    public class UserLoginTask extends AsyncTask<Void, Void, LoginStatus> {

        private final String mEmail;
        private final String mPassword;

        UserLoginTask(String email, String password) {
            mEmail = email;
            mPassword = password;

        }

        @Override
        protected void onPreExecute() {/*This method intentionally left blank*/}

        //This function attempts a login with the credentials currently in the credentials fields
        @Override
        protected LoginStatus doInBackground(Void... params) {
            //System.out.println("GOT HeRe!.!");
            DefaultHttpClient httpclient = new DefaultHttpClient();
            String path = "https://test.realbotics.com/";
            //System.out.println(path);
            //url with the post data
            HttpResponse response;

            HttpPost httpost = new HttpPost(path);
            //System.out.println("The End Is Nigh!");
            StringEntity se;
            //convert parameters into JSON object
            //System.out.println("got herE!");
            List<NameValuePair> urlParameters = new ArrayList<NameValuePair>();
            urlParameters.add(new BasicNameValuePair("grant_type", "password"));
            urlParameters.add(new BasicNameValuePair("username", mEmail));
            urlParameters.add(new BasicNameValuePair("password", mPassword));

            try {
                //passes the results to a string builder/entity
                se = new StringEntity(URLEncodedUtils.format(urlParameters, HTTP.UTF_8));
                //System.out.println("se = " + se);
            } catch (UnsupportedEncodingException e1) {
                e1.printStackTrace();
                return LoginStatus.ERRORED;
            }
            //sets the post request as the resulting string
            httpost.setEntity(se);

            //sets a request header so the page receiving the request
            httpost.setHeader("Content-Type", "application/x-www-form-urlencoded");

            //Handles what is returned from the page. Parse it into JSON format
            try {

                // code from http://stackoverflow.com/questions/12026317/extracting-data-from-a-html-response-android
                response = httpclient.execute(httpost);

            } catch (ClientProtocolException e) {
                e.printStackTrace();
                return LoginStatus.ERRORED;
            } catch (IOException e) {
                e.printStackTrace();
                return LoginStatus.ERRORED;
            }

            if (response.getStatusLine().getStatusCode() == 200) {

                //Extract data from response
                ServerSingleton server = ServerSingleton.getInstance(LoginActivity.this);
                try {
                    String accessToken; String refreshToken; String expiresIn; String parsedHTMLEntity;
                    HttpEntity entity = response.getEntity();
                    InputStream is = entity.getContent();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(is,"iso-8859-1"),8);
                    StringBuilder sb = new StringBuilder();
                    String line = null;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line + "\n");
                    }
                    is.close();
                    parsedHTMLEntity = sb.toString();

                    JSONObject jObj= new JSONObject(parsedHTMLEntity);
                    accessToken = jObj.getString("access_token");
                    refreshToken = jObj.getString("refresh_token");
                    expiresIn = jObj.getString("expires_in");

                    AuthTokenSingleton.get_instance().setUnameAndPassword(mEmail, mPassword);
                    AuthTokenSingleton.get_instance().setToken(accessToken, expiresIn,  refreshToken);

                } catch (IOException e) {
                    e.printStackTrace();
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                return LoginStatus.SUCCESSFUL;
            }
            return LoginStatus.FAILED;
        }

        //This function is called after the login attempts and figures out what the next action is to be and makes it happen
        @Override
        protected void onPostExecute(final LoginStatus success) {
            taskRunning = false;
            //System.out.println(success);
            if (success == LoginStatus.SUCCESSFUL) {
                loginSuccessful(mEmail, mPassword);
                //                finish();
            } else {
                System.out.println("ERROR LOGGING IN");
            }
        }

        @Override
        protected void onCancelled() {
            taskRunning = false;
        }
    }
}
