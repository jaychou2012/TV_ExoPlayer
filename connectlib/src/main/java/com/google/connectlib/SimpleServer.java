package com.google.connectlib;

import android.content.Context;

import org.java_ws.WebSocket;
import org.java_ws.handshake.ClientHandshake;
import org.java_ws.server.WebSocketServer;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

public class SimpleServer extends WebSocketServer {
    private Context context;
    private MessageListener messageListener;

    public SimpleServer(Context context, InetSocketAddress address) {
        super(address);
        this.context = context;
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        if (messageListener != null) {
            messageListener.onMessage("new connection to " + conn.getRemoteSocketAddress());
        }
        conn.send("Welcome to the web server!"); //This method sends a message to the new client
        broadcast("new connection: " + handshake.getResourceDescriptor()); //This method sends a message to all clients connected
        System.out.println("new connection to " + conn.getRemoteSocketAddress());
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        System.out.println("closed " + conn.getRemoteSocketAddress() + " with exit code " + code + " additional info: " + reason);
        if (messageListener != null) {
            messageListener.onMessage("closed " + conn.getRemoteSocketAddress() + " with exit code " + code + " additional info: " + reason);
        }
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        System.out.println("received message from " + conn.getRemoteSocketAddress() + ": " + message);
        if (messageListener != null) {
            messageListener.onMessage(message);
        }
    }

    @Override
    public void onMessage(WebSocket conn, ByteBuffer message) {
        System.out.println("received ByteBuffer from " + conn.getRemoteSocketAddress());
        if (messageListener != null) {
            messageListener.onMessage("received ByteBuffer from " + conn.getRemoteSocketAddress());
        }
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        System.err.println("an error occured on connection " + conn.getRemoteSocketAddress() + ":" + ex);
        if (messageListener != null) {
            messageListener.onMessage("an error occured on connection " + conn.getRemoteSocketAddress() + ":" + ex);
        }
    }

    @Override
    public void onStart() {
        System.out.println("server started successfully");
        if (messageListener != null) {
            messageListener.onMessage("server started successfully");
        }
    }

    public void setMessageListener(MessageListener messageListener) {
        this.messageListener = messageListener;
    }

    public interface MessageListener {
        void onMessage(String message);
    }

    public static void main(String[] args) {
        String host = Utils.getLocalIpV4Address();
        int port = 8887;

        WebSocketServer server = new SimpleServer(null, new InetSocketAddress(host, port));
        server.run();
    }
}
