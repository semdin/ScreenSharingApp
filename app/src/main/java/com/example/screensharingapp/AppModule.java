package com.example.screensharingapp;

import android.content.Context;
import com.google.gson.Gson;
import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.components.SingletonComponent;

@Module
@InstallIn(SingletonComponent.class)
public class AppModule {

    @Provides
    public Context provideContext(@ApplicationContext Context context) {
        return context;
    }

    @Provides
    public Gson provideGson() {
        return new Gson();
    }
}

