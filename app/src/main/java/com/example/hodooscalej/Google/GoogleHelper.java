package com.example.hodooscalej.Google;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import com.example.hodooscalej.MainActivity;
import com.example.hodooscalej.R;
import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;

import java.io.IOException;

public class GoogleHelper implements GoogleApiClient.OnConnectionFailedListener {

    private static final String TAG = "GoogleHelper";
    private final String SCOPES = "oauth2:profile email";
    private final int RC_SIGN_IN = 100;
    private GoogleListener mListener;
    private GoogleApiClient mGoogleApiClient;
    private String idToken;
    private FragmentActivity mContext;
    FirebaseAuth firebaseAuth;
    MainActivity mainActivity;
   private GoogleSignInClient mGoogleSignInClient;

    public GoogleHelper(@NonNull GoogleListener listener, MainActivity mActivity){
        mainActivity = mActivity;
        mListener = listener;

        firebaseAuth = FirebaseAuth.getInstance();

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(mainActivity.getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(mainActivity, gso);
    }



    public void performSignIn() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        mainActivity.startActivityForResult(signInIntent, RC_SIGN_IN);
      /*  Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
        activity.startActivityForResult(signInIntent, RC_SIGN_IN);*/
    }



    public void onActivityResult(int requestCode, int resultCode, Intent data){
        /// For google login
        // Result returned from launching the Intent from GoogleSignInClient.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);

            if(result.isSuccess()){
                GoogleSignInAccount account = result.getSignInAccount();
                idToken = account.getIdToken();

                Log.e("jalli _________","____ "+idToken);
                // The Task returned from this call is always completed, no need to attach
                // a listener.
                Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
                handleSignInResult(task);

                AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
                firebaseAuthWithGoogle(credential);
            }else {
                mListener.onGoogleAuthSignInFailed(result.getStatus().getStatusMessage());
            }

        }
    }

    // For sucess  google login
    private void handleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
            // Signed in successfully, show authenticated UI.

        } catch (ApiException e) {
            // The ApiException status code indicates the detailed failure reason.
            // Please refer to the GoogleSignInStatusCodes class reference for more information.
            Log.w(TAG, "signInResult:failed code=" + e.getStatusCode());
        }
    }

    // sign for google account
    private void firebaseAuthWithGoogle(AuthCredential credential){
        firebaseAuth.signInWithCredential(credential)
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                       // Toast.makeText((""+e.getMessage(), Toast.LENGTH_LONG).show();
                        Log.e("EROOR Login","____ "+e.getMessage());
                    }

                }).addOnSuccessListener(new OnSuccessListener<AuthResult>() {
            @Override
            public void onSuccess(AuthResult authResult) {
                String email = authResult.getUser().getEmail();
                Log.e("Success login ","Email is "+email);
                mListener.onGoogleAuthSignIn(email);
                //Toast.makeText(MainActivity.this,"You are signed with email: " +email, Toast.LENGTH_LONG).show();
            }});
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        mListener.onGoogleAuthSignInFailed(connectionResult.getErrorMessage());
    }

    public void performSignOut() {
        Auth.GoogleSignInApi.signOut(mGoogleApiClient).setResultCallback(new ResultCallback<Status>() {
            @Override public void onResult(@NonNull Status status) {
                mListener.onGoogleAuthSignOut();
            }
        });
    }

}
