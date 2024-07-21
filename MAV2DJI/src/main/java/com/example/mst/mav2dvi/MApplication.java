package com.example.mst.mav2dvi;

import android.app.Application;
import android.content.Context;
import com.secneo.sdk.Helper;

public class MApplication extends Application {

    private ApplicationCore mApplicationCore = null;
    @Override
    protected void attachBaseContext(Context paramContext) {
        super.attachBaseContext(paramContext);
        Helper.install(MApplication.this);
        if (mApplicationCore == null) {
            mApplicationCore = new ApplicationCore();
            mApplicationCore.setContext(this);
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mApplicationCore.onCreate();
    }

}
