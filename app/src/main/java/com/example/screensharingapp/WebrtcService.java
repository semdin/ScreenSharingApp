package com.example.screensharingapp;

import static android.content.ContentValues.TAG;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import dagger.hilt.android.AndroidEntryPoint;

import com.example.screensharingapp.R;
import com.example.screensharingapp.MainRepository;

import org.webrtc.MediaStream;
import org.webrtc.SurfaceViewRenderer;

import javax.inject.Inject;

@AndroidEntryPoint
public class WebrtcService extends Service implements MainRepository.Listener {
    public static Intent screenPermissionIntent = null;
    public static SurfaceViewRenderer surfaceView = null;
    public static MainRepository.Listener listener = null;

    @Inject
    MainRepository mainRepository;

    private NotificationManager notificationManager;
    private String username;

    @Override
    public void onCreate() {
        super.onCreate();
        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        mainRepository.setListener(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            switch (intent.getAction()) {
                case "StartIntent":
                    username = intent.getStringExtra("username");
                    mainRepository.init(username, surfaceView);
                    startServiceWithNotification();
                    break;
                case "StopIntent":
                    try {
                        stopMyService();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    break;
                case "EndCallIntent":
                    mainRepository.sendCallEndedToOtherPeer();
                    try {
                        mainRepository.onDestroy();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    try {
                        stopMyService();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    break;
                case "AcceptCallIntent":
                    String target = intent.getStringExtra("target");
                    if (target != null) {
                        mainRepository.startCall(target);
                    }
                    break;
                case "RequestConnectionIntent":
                    String targetForConnection = intent.getStringExtra("target");
                    if (targetForConnection != null) {
                        Log.d(TAG, "onStartCommand: " + screenPermissionIntent);
                        mainRepository.setPermissionIntentToWebrtcClient(screenPermissionIntent);
                        mainRepository.startScreenCapturing(surfaceView);
                        mainRepository.sendScreenShareConnection(targetForConnection);
                    }
                    break;
            }
        }
        return START_STICKY;
    }

    private void stopMyService() throws InterruptedException {
        mainRepository.onDestroy();
        stopSelf();
        notificationManager.cancelAll();
    }

    private void startServiceWithNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel notificationChannel = new NotificationChannel(
                    "channel1", "foreground", NotificationManager.IMPORTANCE_HIGH
            );
            notificationManager.createNotificationChannel(notificationChannel);
            Notification notification = new NotificationCompat.Builder(this, "channel1")
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .build();

            startForeground(1, notification);
        }
    }

    @Override
    public void onConnectionRequestReceived(String target) {
        if (listener != null) {
            listener.onConnectionRequestReceived(target);
        }
    }

    @Override
    public void onConnectionConnected() {
        if (listener != null) {
            listener.onConnectionConnected();
        }
    }

    @Override
    public void onCallEndReceived() throws InterruptedException {
        if (listener != null) {
            listener.onCallEndReceived();
        }
        stopMyService();
    }

    @Override
    public void onRemoteStreamAdded(MediaStream stream) {
        if (listener != null) {
            listener.onRemoteStreamAdded(stream);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
