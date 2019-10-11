package com.example.hodooscalej.Facebook;

import com.facebook.AccessToken;

public interface FacebookListener {
    void onFbSignInFail(String errorMessage);

    void onFBSignOut();

    void onFbSignInSuccess(AccessToken accessToken);
}
