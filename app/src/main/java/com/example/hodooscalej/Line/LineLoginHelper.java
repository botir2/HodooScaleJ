package com.example.hodooscalej.Line;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.example.hodooscalej.MainActivity;
import com.example.hodooscalej.R;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.linecorp.linesdk.LineProfile;
import com.linecorp.linesdk.Scope;
import com.linecorp.linesdk.api.LineApiClient;
import com.linecorp.linesdk.api.LineApiClientBuilder;
import com.linecorp.linesdk.auth.LineAuthenticationParams;
import com.linecorp.linesdk.auth.LineLoginApi;
import com.linecorp.linesdk.auth.LineLoginResult;

import org.json.JSONObject;
import jp.line.android.sdk.LineSdkContext;
import jp.line.android.sdk.LineSdkContextManager;
import jp.line.android.sdk.exception.LineSdkLoginException;
import jp.line.android.sdk.login.LineAuthManager;
import jp.line.android.sdk.login.LineLoginFuture;
import jp.line.android.sdk.login.LineLoginFutureListener;

import java.util.Arrays;
import java.util.HashMap;
import static android.media.audiofx.Visualizer.SUCCESS;
import static android.telecom.DisconnectCause.CANCELED;

public class LineLoginHelper {

    private String mLineAcessscodeVerificationEndpoint;
    private static final String TAG = LineLoginHelper.class.getSimpleName();
    private MainActivity MainActivityListener;

    private static final int REQUEST_CODE = 1;

    public static final String CHANNEL_ID = "1608554273";

    private static LineApiClient lineApiClient;

    private boolean isClicked = false;

    public LineLoginHelper(MainActivity mainActivity) {

        MainActivityListener = mainActivity;

        mLineAcessscodeVerificationEndpoint = MainActivityListener.getString(R.string.line_validation_server_domain) + "/verifyToken";
    }


    public static LineApiClient getLineApiClient(Context context) {
        if (lineApiClient == null) {
            synchronized (MainActivity.class) {
                if (lineApiClient == null) {
                    LineApiClientBuilder apiClientBuilder = new LineApiClientBuilder(context, CHANNEL_ID);
                    lineApiClient = apiClientBuilder.build();
                }
            }
        }
        return lineApiClient;
    }

    public void login() {
        AsyncTask.THREAD_POOL_EXECUTOR.execute(() -> {
            try {
                // 检测token有效性
                if (getLineApiClient(MainActivityListener).verifyToken().isSuccess()) {
                    String accessToken = getLineApiClient(MainActivityListener).getCurrentAccessToken().getResponseData().getTokenString();

                    LineProfile profile = getLineApiClient(MainActivityListener).getProfile().getResponseData();
                    String userId = profile.getUserId();
                    //loginSuccess(userId, accessToken);
                    MainActivityListener.lineLoginSuccess(userId, accessToken);
                    Log.d(TAG,"AccessToken Massege___" +accessToken);
                    return;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            try{
                // 调起line app授权登录
                LineAuthenticationParams params = new LineAuthenticationParams.Builder().scopes(Arrays.asList(Scope.PROFILE)).build();
                Intent loginIntent = LineLoginApi.getLoginIntent(MainActivityListener.getApplication(), CHANNEL_ID, params);
                MainActivityListener.startActivityForResult(loginIntent, REQUEST_CODE);

            } catch(Exception e) {
                e.printStackTrace();
            }
        });
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data){

        if (requestCode  ==  REQUEST_CODE) {
            Log.d(TAG,"requestCode Massege__________________________" +requestCode);
            return;
        }

        LineLoginResult result = LineLoginApi.getLoginResultFromIntent(data);
        switch (result.getResponseCode()) {

            case SUCCESS:
                try {
                    String accessToken = result.getLineCredential().getAccessToken().getTokenString();
                    String userId = result.getLineProfile().getUserId();
                    String displayName = result.getLineProfile().getDisplayName();
                    Log.d(TAG,"AccessToken Massege___" +accessToken);
                    Log.d(TAG,"userId Massege___" +userId);
                    Log.d(TAG,"displayName Massege___" +displayName);
                    GetFirebaseAuthToken(MainActivityListener, accessToken);

                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;

            case CANCEL:
                isClicked = false;
                Log.d(TAG,"displayName Massege_____"+ "Login failed");
                break;
            default:
                // Login FAILED!
        }
    }


    public Task<AuthResult> startLineLogin() {

        /**
         * Use Tasks API to chain 3 login steps together
         * Refer to this blog post for more details about Tasks API:
         *   https://firebase.googleblog.com/2016/09/become-a-firebase-taskmaster-part-1.html
         **/

        Task<AuthResult> combinedTask =
                // STEP 1: User logins with LINE and get their LINE access token
                getLineAccessCode(MainActivityListener)
                        .continueWithTask(new Continuation<String, Task<String>>() {
                            @Override
                            public Task<String> then(@NonNull Task<String> task) throws Exception {
                                // STEP 2: Exchange LINE access token for Firebase Custom Auth token
                                String lineAccessCode = task.getResult();
                                //return getFirebaseAuthToken(MainActivityListener, lineAccessCode);
                                return null;
                            }
                        })
                        .continueWithTask(new Continuation<String, Task<AuthResult>>() {
                            @Override
                            public Task<AuthResult> then(@NonNull Task<String> task) throws Exception {
                                // STEP 3: Use Firebase Custom Auth token to login Firebase
                                String firebaseToken = task.getResult();
                                FirebaseAuth auth = FirebaseAuth.getInstance();
                                return auth.signInWithCustomToken(firebaseToken);
                            }
                        });

        return combinedTask;
    }

    private Task<String> getLineAccessCode(final Activity activity) {
        final TaskCompletionSource<String> source = new TaskCompletionSource<>();

        // STEP 1: User logins with LINE and get their LINE access token
        LineSdkContext sdkContext = LineSdkContextManager.getSdkContext();
        LineAuthManager authManager = sdkContext.getAuthManager();
        LineLoginFuture loginFuture = authManager.login(activity);
        loginFuture.addFutureListener(new LineLoginFutureListener() {
            @Override
            public void loginComplete(LineLoginFuture future) {
                switch(future.getProgress()) {
                    case SUCCESS: //Login successfully
                        String lineAccessToken = future.getAccessToken().accessToken;
                        Log.d(TAG, "LINE Access token = " + lineAccessToken);
                        source.setResult(lineAccessToken);
                        break;
                    case CANCELED: // Login canceled by user
                        Exception e = new Exception("User cancelled LINE Login.");
                        source.setException(e);
                        break;
                    default: // Error
                        Throwable cause = future.getCause();
                        if (cause instanceof LineSdkLoginException) {
                            LineSdkLoginException loginException = (LineSdkLoginException) cause;
                            Log.e(TAG, loginException.getMessage());
                            source.setException(loginException);
                        } else {
                            source.setException(new Exception("Unknown error occurred in LINE SDK."));
                        }
                        break;
                }
            }
        });

        return source.getTask();
    }

    private Task<String> GetFirebaseAuthToken(Context context, final String lineAccessToken) {
        final TaskCompletionSource<String> source = new TaskCompletionSource<>();

        // STEP 2: Exchange LINE access token for Firebase Custom Auth token
        HashMap<String, String> validationObject = new HashMap<>();
        validationObject.put("token", lineAccessToken);

        // Exchange LINE Access Token for Firebase Auth Token
        Response.Listener<JSONObject> responseListener = new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    String firebaseToken = response.getString("firebase_token");
                    Log.d(TAG, "Firebase Token = " + firebaseToken);
                    source.setResult(firebaseToken);
                } catch (Exception e) {
                    source.setException(e);
                }
            }
        };

        Response.ErrorListener errorListener = new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e(TAG, error.toString());
                source.setException(error);
            }
        };

        JsonObjectRequest fbTokenRequest = new JsonObjectRequest(
                Request.Method.POST, mLineAcessscodeVerificationEndpoint,
                new JSONObject(validationObject),
                responseListener, errorListener);

        NetworkSingleton.getInstance(context).addToRequestQueue(fbTokenRequest);

        return source.getTask();
    }

    public void signOut() {
        FirebaseAuth.getInstance().signOut();
        LineSdkContextManager.getSdkContext().getAuthManager().logout();
    }

}
