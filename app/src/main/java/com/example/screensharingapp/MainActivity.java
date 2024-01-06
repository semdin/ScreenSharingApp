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
import org.webrtc.VideoFrame;
import org.webrtc.VideoSink;
import org.webrtc.VideoTrack;

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
        views.idTv.setText("Your id: " + username);
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
            views.notificationTitle.setText(target + " wants to share screen. Do you want to accept?");
            views.notificationLayout.setVisibility(View.VISIBLE);
            views.requestLayout.setVisibility(View.GONE);
            views.notificationAcceptBtn.setOnClickListener(v -> {
                webrtcServiceRepository.acceptCall(target);
                views.notificationLayout.setVisibility(View.GONE);
            });
            views.notificationDeclineBtn.setOnClickListener(v -> {
                views.notificationLayout.setVisibility(View.GONE);
                views.requestLayout.setVisibility(View.VISIBLE);
            });
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
            // if device is tv scale the surface view
            /*if (ViewCompat.isAttachedToWindow(views.surfaceView)) {
                views.surfaceView.setScaleX(1.5f);
                views.surfaceView.setScaleY(1.5f);
            }*/
        });
    }

    /* trying to fix the video size issue */
//    @Override
//    public void onRemoteStreamAdded(MediaStream stream) {
//        runOnUiThread(() -> {
//            views.surfaceView.setVisibility(View.VISIBLE);
//            VideoTrack videoTrack = stream.videoTracks.get(0);
//            if (videoTrack != null) {
//                videoTrack.addSink(views.surfaceView);
//
//                videoTrack.addSink(new VideoSink() {
//                    @Override
//                    public void onFrame(VideoFrame frame) {
//                        runOnUiThread(() -> {
//                            int videoWidth = frame.getBuffer().getWidth();
//                            int videoHeight = frame.getBuffer().getHeight();
//
//
//                            int surfaceViewWidth = views.surfaceView.getWidth();
//                            int surfaceViewHeight = views.surfaceView.getHeight();
//
//                            if (surfaceViewWidth != 0 && surfaceViewHeight != 0) {
//                                float scaleX = (float) surfaceViewWidth / videoWidth;
//                                float scaleY = (float) surfaceViewHeight / videoHeight;
//
//                                float scale = Math.min(scaleX, scaleY);
//
//                                views.surfaceView.setScaleX(scale);
//                                views.surfaceView.setScaleY(scale);
//                            }
//                        });
//                    }
//                });
//            }
//        });
//    }


    private void restartUi() {
        views.disconnectBtn.setVisibility(View.GONE);
        views.requestLayout.setVisibility(View.VISIBLE);
        views.notificationLayout.setVisibility(View.GONE);
        views.surfaceView.setVisibility(View.GONE);
    }
}
