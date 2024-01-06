package com.example.screensharingapp;

import java.io.IOException;
import java.net.*;

public class UdpBroadcastClient {
    private static final int UDP_BROADCAST_PORT = 8888;
    private static final String UDP_BROADCAST_ADDRESS = "255.255.255.255";

    public interface UdpBroadcastListener {
        void onBroadcastReceived(String message);
    }

    private UdpBroadcastListener listener;

    public void setListener(UdpBroadcastListener listener) {
        this.listener = listener;
    }

    public void startBroadcastListening() {
        new Thread(() -> {
            try {
                DatagramSocket socket = new DatagramSocket(UDP_BROADCAST_PORT);
                socket.setBroadcast(true);

                byte[] receiveBuffer = new byte[1024];

                while (true) {
                    DatagramPacket packet = new DatagramPacket(receiveBuffer, receiveBuffer.length);
                    socket.receive(packet);

                    String message = new String(packet.getData(), 0, packet.getLength());
                    if (listener != null) {
                        listener.onBroadcastReceived(message);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    public void sendBroadcast(String message) {
        new Thread(() -> {
            try {
                DatagramSocket socket = new DatagramSocket();
                socket.setBroadcast(true);

                byte[] sendData = message.getBytes();

                DatagramPacket packet = new DatagramPacket(
                        sendData,
                        sendData.length,
                        InetAddress.getByName(UDP_BROADCAST_ADDRESS),
                        UDP_BROADCAST_PORT
                );

                socket.send(packet);
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }
}
