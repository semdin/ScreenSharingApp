package com.example.screensharingapp;

import static android.content.ContentValues.TAG;

import android.content.Context;
import android.content.Intent;
import android.media.projection.MediaProjection;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.WindowManager;

import androidx.annotation.NonNull;

import com.example.screensharingapp.DataModel;
import com.example.screensharingapp.DataModelType;
import com.google.gson.Gson;

import org.jetbrains.annotations.Contract;
import org.webrtc.DefaultVideoDecoderFactory;
import org.webrtc.DefaultVideoEncoderFactory;
import org.webrtc.*;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

public class WebrtcClient {

    private Context context;
    private Gson gson;

    private String username;
    private PeerConnection.Observer observer;
    private SurfaceViewRenderer localSurfaceView;
    private Listener listener;
    private Intent permissionIntent;

    private PeerConnection peerConnection;
    private EglBase eglBase;
    private EglBase.Context eglBaseContext;
    private PeerConnectionFactory peerConnectionFactory;
    private MediaConstraints mediaConstraint;
    private List<PeerConnection.IceServer> iceServers;

    private VideoCapturer screenCapturer;
    private VideoSource localVideoSource;
    private VideoTrack localVideoTrack;
    private MediaStream localStream;

    private final String localTrackId = "local_track";
    private final String localStreamId = "local_stream";

    @Inject
    public WebrtcClient(Context context, Gson gson) {
        this.context = context;
        this.gson = gson;
        init();
    }

    private void init() {
        eglBase = EglBase.create();
        eglBaseContext = eglBase.getEglBaseContext();
        initPeerConnectionFactory();

        mediaConstraint = new MediaConstraints();
        mediaConstraint.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"));

        iceServers = Collections.singletonList(PeerConnection.IceServer.builder("turn:openrelay.metered.ca:443?transport=tcp")
                .setUsername("openrelayproject")
                .setPassword("openrelayproject")
                .createIceServer());
    }

    private void initPeerConnectionFactory() {
        PeerConnectionFactory.InitializationOptions initializationOptions =
                PeerConnectionFactory.InitializationOptions.builder(context)
                        .setEnableInternalTracer(true)
                        .setFieldTrials("WebRTC-H264HighProfile/Enabled/")
                        .createInitializationOptions();
        PeerConnectionFactory.initialize(initializationOptions);

        peerConnectionFactory = PeerConnectionFactory.builder()
                .setVideoEncoderFactory(new DefaultVideoEncoderFactory(eglBaseContext, true, true))
                .setVideoDecoderFactory(new DefaultVideoDecoderFactory(eglBaseContext))
                .createPeerConnectionFactory();
    }

    public void initializeWebrtcClient(String username, SurfaceViewRenderer view, PeerConnection.Observer observer) {
        this.username = username;
        this.observer = observer;
        this.localSurfaceView = view;
        initSurfaceView();
        peerConnection = createPeerConnection();
    }

    private void initSurfaceView() {
        localSurfaceView.setMirror(false);
        localSurfaceView.setEnableHardwareScaler(true);
        localSurfaceView.init(eglBaseContext, null);
        localSurfaceView.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL); // test for TV because of aspect ratio.
    }

    private PeerConnection createPeerConnection() {
        return peerConnectionFactory.createPeerConnection(iceServers, mediaConstraint, observer);
    }

    public void setPermissionIntent(Intent intent) {
        this.permissionIntent = intent;
    }

    public void startScreenCapturing(SurfaceViewRenderer view) {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        WindowManager windowsManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        windowsManager.getDefaultDisplay().getMetrics(displayMetrics);

        int screenWidthPixels = displayMetrics.widthPixels;
        int screenHeightPixels = displayMetrics.heightPixels;

        SurfaceTextureHelper surfaceTextureHelper = SurfaceTextureHelper.create("CaptureThread", eglBaseContext);

        screenCapturer = createScreenCapturer();
        if (screenCapturer == null) {
            return;
        }
        localVideoSource = peerConnectionFactory.createVideoSource(screenCapturer.isScreencast());
        screenCapturer.initialize(surfaceTextureHelper, context, localVideoSource.getCapturerObserver());
        screenCapturer.startCapture(screenWidthPixels, screenHeightPixels, 60);


        localVideoTrack = peerConnectionFactory.createVideoTrack(localTrackId, localVideoSource);
        //localVideoTrack.addSink(localSurfaceView);
        localVideoTrack.addSink(view);

        localStream = peerConnectionFactory.createLocalMediaStream(localStreamId);
        localStream.addTrack(localVideoTrack);

        peerConnection.addStream(localStream);
    }

    private VideoCapturer createScreenCapturer() {
        if (permissionIntent == null) {
            return null;
        }
        return new ScreenCapturerAndroid(permissionIntent, new MediaProjection.Callback() {
            @Override
            public void onStop() {
                super.onStop();
                Log.d(TAG, "onStop: ");
            }
        });
    }

    public void call(String target) {
        peerConnection.createOffer(new MySdpObserver() {
            @Override
            public void onCreateSuccess(SessionDescription sessionDescription) {
                peerConnection.setLocalDescription(new MySdpObserver(), sessionDescription);
                listener.onTransferEventToSocket(new DataModel(
                        DataModelType.Offer, username, target, sessionDescription.description));
            }
        }, mediaConstraint);
    }

    public void answer(String target) {
        peerConnection.createAnswer(new MySdpObserver() {
            @Override
            public void onCreateSuccess(SessionDescription sessionDescription) {
                peerConnection.setLocalDescription(new MySdpObserver(), sessionDescription);
                listener.onTransferEventToSocket(new DataModel(
                        DataModelType.Answer, username, target, sessionDescription.description));
            }
        }, mediaConstraint);
    }

    public void onRemoteSessionReceived(SessionDescription sessionDescription) {
        peerConnection.setRemoteDescription(new MySdpObserver(), sessionDescription);
    }

    public void addIceCandidate(IceCandidate iceCandidate) {
        peerConnection.addIceCandidate(iceCandidate);
    }

    public void sendIceCandidate(IceCandidate candidate, String target) {
        listener.onTransferEventToSocket(new DataModel(
                DataModelType.IceCandidates, username, target, gson.toJson(candidate)));
    }

    public void closeConnection() throws InterruptedException {
        if (screenCapturer != null) {
            screenCapturer.stopCapture();
            screenCapturer.dispose();
        }
        if (localStream != null) {
            localStream.dispose();
        }
        if (peerConnection != null) {
            peerConnection.close();
        }
    }

    public void restart() throws InterruptedException {
        closeConnection();
        initializeWebrtcClient(username, localSurfaceView, observer);
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public interface Listener {
        void onTransferEventToSocket(DataModel data);
    }
}
