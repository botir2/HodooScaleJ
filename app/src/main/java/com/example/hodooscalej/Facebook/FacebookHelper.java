package com.example.hodooscalej.Facebook;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import androidx.annotation.CheckResult;
import androidx.annotation.NonNull;

import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;


public class FacebookHelper {
    private FacebookListener mListener;
    private CallbackManager mCallBackManager;

    public FacebookHelper(@NonNull FacebookListener facebookListener){

        mListener = facebookListener;
        mCallBackManager = CallbackManager.Factory.create();

        FacebookCallback<LoginResult> mCallBack = new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                mListener.onFbSignInSuccess(loginResult.getAccessToken());
            }

            @Override
            public void onCancel() {
                mListener.onFbSignInFail("User cancelled operation");
            }

            @Override
            public void onError(FacebookException error) {
                mListener.onFbSignInFail(error.getMessage());
            }
        }; LoginManager.getInstance().registerCallback(mCallBackManager, mCallBack);

    }

    @NonNull
    @CheckResult
    public CallbackManager getCallbackManager() {
        return mCallBackManager;
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        mCallBackManager.onActivityResult(requestCode, resultCode, data);
    }

}
