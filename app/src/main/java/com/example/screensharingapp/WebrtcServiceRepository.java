package com.example.screensharingapp;

import android.content.Context;
import android.content.Intent;
import android.os.Build;

import javax.inject.Inject;

public class WebrtcServiceRepository {
    private final Context context;

    @Inject
    public WebrtcServiceRepository(Context context) {
        this.context = context;
    }

    public void startIntent(String username) {
        Thread thread = new Thread(() -> {
            Intent startIntent = new Intent(context, WebrtcService.class);
            startIntent.setAction("StartIntent");
            startIntent.putExtra("username", username);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(startIntent);
            } else {
                context.startService(startIntent);
            }
        });
        thread.start();
    }

    public void requestConnection(String target) {
        Thread thread = new Thread(() -> {
            Intent startIntent = new Intent(context, WebrtcService.class);
            startIntent.setAction("RequestConnectionIntent");
            startIntent.putExtra("target", target);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(startIntent);
            } else {
                context.startService(startIntent);
            }
        });
        thread.start();
    }

    public void acceptCall(String target) {
        Thread thread = new Thread(() -> {
            Intent startIntent = new Intent(context, WebrtcService.class);
            startIntent.setAction("AcceptCallIntent");
            startIntent.putExtra("target", target);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(startIntent);
            } else {
                context.startService(startIntent);
            }
        });
        thread.start();
    }

    public void endCallIntent() {
        Thread thread = new Thread(() -> {
            Intent startIntent = new Intent(context, WebrtcService.class);
            startIntent.setAction("EndCallIntent");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(startIntent);
            } else {
                context.startService(startIntent);
            }
        });
        thread.start();
    }

    public void stopIntent() {
        Thread thread = new Thread(() -> {
            Intent startIntent = new Intent(context, WebrtcService.class);
            startIntent.setAction("StopIntent");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(startIntent);
            } else {
                context.startService(startIntent);
            }
        });
        thread.start();
    }
}
