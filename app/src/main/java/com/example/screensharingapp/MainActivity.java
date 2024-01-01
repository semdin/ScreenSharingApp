package com.example.screensharingapp;

import static android.content.ContentValues.TAG;

import android.content.Context;
import android.content.Intent;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import com.example.screensharingapp.databinding.ActivityMainBinding;
import com.example.screensharingapp.MainRepository;
import com.example.screensharingapp.WebrtcService;
import com.example.screensharingapp.WebrtcServiceRepository;
import dagger.hilt.android.AndroidEntryPoint;
import org.webrtc.MediaStream;
import javax.inject.Inject;

@AndroidEntryPoint
public class MainActivity extends AppCompatActivity implements MainRepository.Listener {

    private String username = null;
    private ActivityMainBinding views;




    @Inject
    WebrtcServiceRepository webrtcServiceRepository;
    private final int capturePermissionRequestCode = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        views = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(views.getRoot());
        webrtcServiceRepository = new WebrtcServiceRepository(this);
        if (webrtcServiceRepository == null) {
            Log.e(TAG, "WebrtcServiceRepository is null!");
            return;
        }
        init();
    }

    private void init() {
        username = getIntent().getStringExtra("username");
        if (username == null || username.isEmpty()) {
            finish();
            return;
        }
        WebrtcService.surfaceView = views.surfaceView;
        WebrtcService.listener = this;
        webrtcServiceRepository.startIntent(username);
        views.requestBtn.setOnClickListener(view -> startScreenCapture());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != capturePermissionRequestCode) return;
        WebrtcService.screenPermissionIntent = data;
        webrtcServiceRepository.requestConnection(views.targetEt.getText().toString());
    }

    private void startScreenCapture() {
        MediaProjectionManager mediaProjectionManager = (MediaProjectionManager)
                getApplication().getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        startActivityForResult(mediaProjectionManager.createScreenCaptureIntent(), capturePermissionRequestCode);
    }

    @Override
    public void onConnectionRequestReceived(String target) {
        runOnUiThread(() -> {
            views.notificationTitle.setText(target + " is requesting for connection");
            views.notificationLayout.setVisibility(View.VISIBLE);
            views.notificationAcceptBtn.setOnClickListener(v -> {
                webrtcServiceRepository.acceptCall(target);
                views.notificationLayout.setVisibility(View.GONE);
            });
            views.notificationDeclineBtn.setOnClickListener(v -> views.notificationLayout.setVisibility(View.GONE));
        });
    }

    @Override
    public void onConnectionConnected() {
        runOnUiThread(() -> {
            views.requestLayout.setVisibility(View.GONE);
            views.disconnectBtn.setVisibility(View.VISIBLE);
            views.disconnectBtn.setOnClickListener(v -> {
                webrtcServiceRepository.endCallIntent();
                restartUi();
            });
        });
    }

    @Override
    public void onCallEndReceived() {
        runOnUiThread(this::restartUi);
    }

    @Override
    public void onRemoteStreamAdded(MediaStream stream) {
        runOnUiThread(() -> {
            views.surfaceView.setVisibility(View.VISIBLE);
            stream.videoTracks.get(0).addSink(views.surfaceView);
        });
    }

    private void restartUi() {
        views.disconnectBtn.setVisibility(View.GONE);
        views.requestLayout.setVisibility(View.VISIBLE);
        views.notificationLayout.setVisibility(View.GONE);
        views.surfaceView.setVisibility(View.GONE);
    }
}
