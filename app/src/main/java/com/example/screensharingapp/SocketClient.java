package com.example.screensharingapp;


import android.util.Log;

import com.example.screensharingapp.DataModel;
import com.example.screensharingapp.DataModelType;
import com.google.gson.Gson;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;

import javax.inject.Inject;
import javax.inject.Singleton;

import kotlin.Unit;
import kotlinx.coroutines.CoroutineScope;
import kotlinx.coroutines.Dispatchers;

@Singleton
public class SocketClient {
    private Gson gson;
    private String username;
    private WebSocketClient webSocket;
    private Listener listener;

    @Inject
    public SocketClient(Gson gson) {
        this.gson = gson;
    }

    public void init(String username) {
        this.username = username;

        webSocket = new WebSocketClient(URI.create("ws://192.168.1.105:3000")) {
            @Override
            public void onOpen(ServerHandshake handshakedata) {
                sendMessageToSocket(new DataModel(DataModelType.SignIn, username, null, null));
            }

            @Override
            public void onMessage(String message) {
                DataModel model = null;
                try {
                    model = gson.fromJson(message, DataModel.class);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                Log.d("TAG", "onMessage: " + model);
                if (model != null) {
                    try {
                        listener.onNewMessageReceived(model);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            }

            @Override
            public void onClose(int code, String reason, boolean remote) {
                new Thread(() -> {
                    try {
                        Thread.sleep(5000);
                        init(username);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }).start();
            }

            @Override
            public void onError(Exception ex) {
            }
        };

        webSocket.connect();
    }

    public void sendMessageToSocket(Object message) {
        try {
            webSocket.send(gson.toJson(message));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void onDestroy() {
        if (webSocket != null) {
            webSocket.close();
        }
    }

    public interface Listener {
        void onNewMessageReceived(DataModel model) throws InterruptedException;

        void onTransferEventToSocket(DataModel data);
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }
}
