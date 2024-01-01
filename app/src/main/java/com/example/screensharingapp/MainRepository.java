package com.example.screensharingapp;

import android.content.Intent;
import android.util.Log;

import com.example.screensharingapp.SocketClient;
import com.example.screensharingapp.DataModel;
import com.example.screensharingapp.MyPeerObserver;
import com.example.screensharingapp.WebrtcClient;
import com.google.gson.Gson;

import org.webrtc.IceCandidate;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.SessionDescription;
import org.webrtc.SurfaceViewRenderer;

import javax.inject.Inject;

public class MainRepository implements SocketClient.Listener, WebrtcClient.Listener {

    private SocketClient socketClient;
    private WebrtcClient webrtcClient;
    private Gson gson;

    private String username;
    private String target;
    private SurfaceViewRenderer surfaceView;
    private Listener listener;

    @Inject
    public MainRepository(SocketClient socketClient, WebrtcClient webrtcClient, Gson gson) {
        this.socketClient = socketClient;
        this.webrtcClient = webrtcClient;
        this.gson = gson;
    }

    public void init(String username, SurfaceViewRenderer surfaceView) {
        this.username = username;
        this.surfaceView = surfaceView;
        initSocket();
        initWebrtcClient();
    }

    private void initSocket() {
        socketClient.setListener(this);
        socketClient.init(username);
    }

    public void setPermissionIntentToWebrtcClient(Intent intent) {
        webrtcClient.setPermissionIntent(intent);
    }

    public void sendScreenShareConnection(String target) {
        socketClient.sendMessageToSocket(
                new DataModel(DataModelType.StartStreaming, username, target, null)
        );
    }

    public void startScreenCapturing(SurfaceViewRenderer surfaceView) {
        webrtcClient.startScreenCapturing(surfaceView);
    }

    public void startCall(String target) {
        webrtcClient.call(target);
    }

    public void sendCallEndedToOtherPeer() {
        socketClient.sendMessageToSocket(
                new DataModel(DataModelType.EndCall, username, target, null)
        );
    }

    public void restartRepository() throws InterruptedException {
        webrtcClient.restart();
    }

    public void onDestroy() throws InterruptedException {
        socketClient.onDestroy();
        webrtcClient.closeConnection();
    }

    private void initWebrtcClient() {
        webrtcClient.setListener(this);
        webrtcClient.initializeWebrtcClient(username, surfaceView, new MyPeerObserver() {
            @Override
            public void onIceCandidate(IceCandidate p0) {
                super.onIceCandidate(p0);
                if (p0 != null) {
                    webrtcClient.sendIceCandidate(p0, target);
                }
            }

            @Override
            public void onConnectionChange(PeerConnection.PeerConnectionState newState) {
                super.onConnectionChange(newState);
                Log.d("TAG", "onConnectionChange: " + newState);
                if (newState == PeerConnection.PeerConnectionState.CONNECTED) {
                    if (listener != null) {
                        listener.onConnectionConnected();
                    }
                }
            }

            @Override
            public void onAddStream(MediaStream p0) {
                super.onAddStream(p0);
                Log.d("TAG", "onAddStream: " + p0);
                if (p0 != null && listener != null) {
                    listener.onRemoteStreamAdded(p0);
                }
            }
        });
    }

    @Override
    public void onNewMessageReceived(DataModel model) throws InterruptedException {
        switch (model.getType()) {
            case StartStreaming:
                this.target = model.getUsername();
                if (listener != null) {
                    listener.onConnectionRequestReceived(model.getUsername());
                }
                break;
            case EndCall:
                if (listener != null) {
                    listener.onCallEndReceived();
                }
                break;
            case Offer:
                webrtcClient.onRemoteSessionReceived(new SessionDescription(
                        SessionDescription.Type.OFFER, model.getData().toString()));
                this.target = model.getUsername();
                webrtcClient.answer(target);
                break;
            case Answer:
                webrtcClient.onRemoteSessionReceived(new SessionDescription(
                        SessionDescription.Type.ANSWER, model.getData().toString()));
                break;
            case IceCandidates:
                IceCandidate candidate = null;
                try {
                    candidate = gson.fromJson(model.getData().toString(), IceCandidate.class);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (candidate != null) {
                    webrtcClient.addIceCandidate(candidate);
                }
                break;
            default:
                // Do nothing
                break;
        }
    }

    @Override
    public void onTransferEventToSocket(DataModel data) {
        socketClient.sendMessageToSocket(data);
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public interface Listener {
        void onConnectionRequestReceived(String target);
        void onConnectionConnected();
        void onCallEndReceived() throws InterruptedException;
        void onRemoteStreamAdded(MediaStream stream);
    }
}
