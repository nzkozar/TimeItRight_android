package com.ak93.timeit;

import android.app.Application;

import io.branch.referral.Branch;
import timber.log.Timber;

/**
 *
 */

public class TimeItApp extends Application {
    private static TimeItApp instance = null;

    @Override
    public void onCreate() {
        super.onCreate();
        setInstance(this);

        Timber.plant(new Timber.DebugTree());
        Timber.i("onCreate()");

        // Branch logging for debugging
        Branch.enableLogging();

        // Branch object initialization
        Branch.getAutoInstance(this);

    }

    public static TimeItApp getInstance() {
        return instance;
    }

    public static void setInstance(TimeItApp instance){
        TimeItApp.instance=instance;
    }
}
