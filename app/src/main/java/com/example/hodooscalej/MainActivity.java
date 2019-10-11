package com.example.hodooscalej;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;


import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;

import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Message;
import android.util.Base64;
import android.util.Log;

import android.view.View;

import android.widget.Button;

import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.toolbox.ImageLoader;
import com.example.hodooscalej.Facebook.FacebookHelper;
import com.example.hodooscalej.Facebook.FacebookListener;
import com.example.hodooscalej.Google.GoogleHelper;
import com.example.hodooscalej.Google.GoogleListener;
import com.example.hodooscalej.Kakao.KakaoHelper;
import com.example.hodooscalej.Kakao.KakaoHelperListener;
import com.example.hodooscalej.Line.LineLoginHelper;
import com.example.hodooscalej.Line.LineLoginHelperListener;
import com.example.hodooscalej.Line.NetworkSingleton;
import com.facebook.AccessToken;

import com.facebook.appevents.codeless.internal.Constants;
import com.facebook.login.widget.LoginButton;
import com.google.android.gms.auth.api.Auth;

import com.google.android.gms.auth.api.signin.GoogleSignInOptions;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.SignInButton;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import com.kakao.auth.Session;
import com.linecorp.linesdk.LineProfile;
import com.linecorp.linesdk.Scope;
import com.linecorp.linesdk.api.LineApiClient;
import com.linecorp.linesdk.api.LineApiClientBuilder;
import com.linecorp.linesdk.auth.LineAuthenticationParams;
import com.linecorp.linesdk.auth.LineLoginApi;
import com.linecorp.linesdk.auth.LineLoginResult;


import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;


public class MainActivity extends AppCompatActivity implements View.OnClickListener,GoogleApiClient.OnConnectionFailedListener, FacebookListener, GoogleListener, LineLoginHelperListener {



    public FirebaseAuth firebaseAuth;


    // adding for google firebase auth
    private FirebaseAuth.AuthStateListener mAuthListener;
    private static final String TAG = MainActivity.class.getName();





   // GoogleSignInClient mGoogleSignInClient;
    private GoogleApiClient mGoogleApiClient;


    private FacebookHelper mFacebook;
    private GoogleHelper mGoogle;
    private KakaoHelper mKakao;
    private LineLoginHelper mLineLoginHelper;
    //Signin button
    LoginButton loginButton;
    Button faceBtn;
    SignInButton signInButton;
    Button GoogleSignin;


    //Kakao login
    private Button btnLoginKakao;


    // Views Line
    private Button mLineLoginButton;
    // Line Helpers
    private ProgressDialog mLoadingDialog;
    private static final int REQUEST_CODE = 1;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        firebaseAuth = FirebaseAuth.getInstance();

        loginButton = (LoginButton)findViewById(R.id.Login_Button);
        faceBtn = (Button)findViewById(R.id.facebookView);
        // google buttons
        signInButton = (SignInButton) findViewById(R.id.login_with_google);
        signInButton.setSize(SignInButton.SIZE_WIDE);
        GoogleSignin = (Button)findViewById(R.id.google_login);


        //asociamos un oyente al evento clic del botón
        loginButton.setOnClickListener(this);
        faceBtn.setOnClickListener(this);

        signInButton.setOnClickListener(this);
        GoogleSignin.setOnClickListener(this);

        //kakao login
        btnLoginKakao = findViewById(R.id.kakao_login);
        btnLoginKakao.setOnClickListener(this);

        firebaseAuth = com.google.firebase.auth.FirebaseAuth.getInstance();

        mFacebook = new FacebookHelper(this);
        mGoogle = new GoogleHelper(this, this);
        mKakao = new KakaoHelper(this);


        //Line Login
        mLineLoginButton = (Button) findViewById(R.id.line_login_button);
        mLineLoginButton.setOnClickListener(this);
        mLineLoginHelper = new LineLoginHelper( this);

    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.facebookView:
                loginButton.performClick();
                break;

            case R.id.google_login:
                //signInButton.performClick();
                mGoogle.performSignIn();
               //signingoogle();
                break;

            case R.id.kakao_login:
                mKakao.performSignIn();
                break;

            case R.id.line_login_button:
               mLineLoginHelper.login();
                break;
        }
    }


    // can remuve
    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Dismiss loading dialog to avoid

    }

    public void AddPetACtivity() {
        startActivity(new Intent( this, AddPet.class));
        finish();//<- IMPORTANT

      /*  Intent intencion = new Intent(getApplication(), AddPet.class);
        startActivity(intencion);*/
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        mFacebook.onActivityResult(requestCode,resultCode,data);
        mGoogle.onActivityResult(requestCode, resultCode, data);
        mKakao.onActivityResult(requestCode, resultCode, data);
        mLineLoginHelper.onActivityResult(requestCode, resultCode, data);

        Log.d(TAG,"RequestCode Massege___" +requestCode);

        if (Session.getCurrentSession().handleActivityResult(requestCode, resultCode, data)) {
            Log.d(TAG, "requestCode: " + requestCode + ", resultCode: " + resultCode);
            return;
        }

        // For Line Login
        if (requestCode != REQUEST_CODE) {
            return;
        }
        LineLoginResult result = LineLoginApi.getLoginResultFromIntent(data);
        switch (result.getResponseCode()) {
            case SUCCESS:
                try {
                    String accessToken = result.getLineCredential().getAccessToken().getTokenString();
                    String userId = result.getLineProfile().getUserId();
                    String displayName = result.getLineProfile().getDisplayName();
                    //TODO 获取token和userId，执行自己的登录业务逻辑
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            case CANCEL:
                // Login CANCEL!
                break;
            default:
                // Login FAILED!
        }

    }

    private void printkeyhash() {
        try {
            PackageInfo info  =  getPackageManager().getPackageInfo("com.example.hodooscalej", PackageManager.GET_SIGNATURES);
            for(Signature signature: info.signatures){
                MessageDigest messageDigest = MessageDigest.getInstance("SHA");
                messageDigest.update(signature.toByteArray());
                Log.e("KEYHASH", Base64.encodeToString(messageDigest.digest(), Base64.DEFAULT));
            }

        }catch (PackageManager.NameNotFoundException e){
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    /************************************************************************FACEBOOK LOGIN*************************************************************************************************/
    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {  }

    @Override
    public void onFbSignInFail(String errorMessage) {  Toast.makeText(MainActivity.this,"Error message: " +errorMessage, Toast.LENGTH_LONG).show(); }

    // this function created inside of the profile for google login
    @Override
    public void onFBSignOut() {
        Auth.GoogleSignInApi.signOut(mGoogleApiClient).setResultCallback(new ResultCallback<Status>() {
        @Override
        public void onResult(@NonNull Status status) {
            Toast.makeText(MainActivity.this,"LOG OUT",Toast.LENGTH_SHORT).show();
            }
    }); }

    @Override
    public void onFbSignInSuccess(AccessToken accessToken) {
        AuthCredential credential = FacebookAuthProvider.getCredential(accessToken.getToken());
        firebaseAuth.signInWithCredential(credential)
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(MainActivity.this,""+e.getMessage(), Toast.LENGTH_LONG).show();
                        Log.e("EROOR LOgin",""+e.getMessage());
                    }

                }).addOnSuccessListener(new OnSuccessListener<AuthResult>() {
            @Override
            public void onSuccess(AuthResult authResult) {
                String email = authResult.getUser().getEmail();
                Toast.makeText(MainActivity.this,"You are signed with email: " +email, Toast.LENGTH_LONG).show();
                AddPetACtivity();
            }
        });
    }


    /************************************************************************GOOGLE LOGIN*************************************************************************************************/
    @Override
    public void onGoogleAuthSignInFailed(String statusMessage) { Toast.makeText(MainActivity.this,"Failed Login: " +statusMessage, Toast.LENGTH_LONG).show(); }

    @Override
    public void onGoogleAuthSignOut() { Toast.makeText(MainActivity.this,"Sing Out: ",  Toast.LENGTH_LONG).show(); }

    @Override
    public void onGoogleAuthSignIn(String email) {
        if (email != null){ AddPetACtivity(); Toast.makeText(MainActivity.this,"You are signed with email: " +email, Toast.LENGTH_LONG).show(); }
        else { Toast.makeText(MainActivity.this,"Failed reginstarion: " +email, Toast.LENGTH_LONG).show();}
    }

   /* @Override
    public void onGoogleAuthSignIn(String token, String userId) {
        if (userId != null){ AddPetACtivity();}
        else { Toast.makeText(MainActivity.this,"Failed reginstarion: " +userId, Toast.LENGTH_LONG).show();}
    }*/

    /************************************************************************Kakao LOGIN*************************************************************************************************/

    /**
     * Update UI based on Firebase's current user. Show Login Button if not logged in. in the Kakao package
     */

    /************************************************************************Line LOGIN*************************************************************************************************/

    @Override
    public void lineLoginSuccess(String token, String userId) {
        Toast.makeText(MainActivity.this,"You are signed with LINE email: " +userId, Toast.LENGTH_LONG).show();
    }



}