package com.example.hodooscalej;

import android.app.Application;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;

import jp.line.android.sdk.LineSdkContextManager;

public class LineLoginGlobalApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        verifyConfiguration();
        LineSdkContextManager.initialize(this);
    }

    private void verifyConfiguration() {
        // Verify if LINE SDK has been setup
        try {
            ApplicationInfo applicationInfo = getPackageManager().getApplicationInfo(getPackageName(), PackageManager.GET_META_DATA);
            Bundle bundle = applicationInfo.metaData;
            Integer lineChannelIdInt = bundle.getInt("jp.line.sdk.ChannelId");
            String lineChannelIdStr = bundle.getString("jp.line.sdk.ChannelId");
            String lineAuthScheme = bundle.getString("jp.line.sdk.AuthScheme");

            if (lineChannelIdInt.intValue() == 0)
                if (lineAuthScheme.contains("<your_channel_id>") ||
                        lineChannelIdStr.contains("<your_channel_id>")) {
                    throw new IllegalStateException();
                }
        } catch (Exception e) {
            throw new IllegalStateException("Please update <your_channel_id> in app/build.gradle with your LINE Channel ID");
        }

        // Verify if validation server URL has been setup
        if (getString(R.string.line_validation_server_domain).contentEquals("your_line_token_verification_server")) {
            throw new IllegalStateException("Please set your validation server domain in res/configs.xml");
        }
    }
}
