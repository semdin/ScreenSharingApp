package com.example.screensharingapp;

import java.io.*;
import java.net.Socket;

public class TcpConnection {
    private static final int TCP_SERVER_PORT = 9090;

    public interface TcpConnectionListener {
        void onMessageReceived(String message);

        void onConnectionClosed();
    }

    private TcpConnectionListener listener;
    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;

    public TcpConnection(TcpConnectionListener listener) {
        this.listener = listener;
    }

    public void connect(String ipAddress) {
        new Thread(() -> {
            try {
                socket = new Socket(ipAddress, TCP_SERVER_PORT);
                reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);

                startListening();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    public void sendMessage(String message) {
        if (writer != null) {
            writer.println(message);
        }
    }

    private void startListening() {
        new Thread(() -> {
            try {
                String receivedMessage;
                while ((receivedMessage = reader.readLine()) != null) {
                    if (listener != null) {
                        listener.onMessageReceived(receivedMessage);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                closeConnection();
            }
        }).start();
    }

    public void closeConnection() {
        try {
            if (reader != null) {
                reader.close();
            }
            if (writer != null) {
                writer.close();
            }
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
            if (listener != null) {
                listener.onConnectionClosed();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
