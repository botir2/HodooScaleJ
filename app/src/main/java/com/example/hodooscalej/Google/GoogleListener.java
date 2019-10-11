package com.example.hodooscalej.Google;

public interface GoogleListener {

    void onGoogleAuthSignInFailed(String statusMessage);

    void onGoogleAuthSignOut();

   // void onGoogleAuthSignIn(String token, String userId);

    void onGoogleAuthSignIn(String email);
}
