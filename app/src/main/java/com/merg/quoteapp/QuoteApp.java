package com.merg.quoteapp;

import android.app.Application;

import com.google.firebase.FirebaseApp;
import com.google.firebase.appcheck.FirebaseAppCheck;
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory;

public class QuoteApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        FirebaseApp.initializeApp(this);
        FirebaseAppCheck appCheck = FirebaseAppCheck.getInstance();
        if (BuildConfig.DEBUG) {
            appCheck.installAppCheckProviderFactory(
                    com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory.getInstance());
        } else {
            appCheck.installAppCheckProviderFactory(
                    PlayIntegrityAppCheckProviderFactory.getInstance());
        }
    }
}
