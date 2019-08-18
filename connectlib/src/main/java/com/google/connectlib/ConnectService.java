package com.google.connectlib;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.Nullable;

import java.io.IOException;
import java.net.InetSocketAddress;

public class ConnectService extends Service implements SimpleServer.MessageListener {
    private Thread thread;
    private SimpleServer server;
    private static MessageListener msgListener;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        if (thread != null) {
            thread = null;
        }
        thread = new Thread() {
            @Override
            public void run() {
                super.run();
                server = new SimpleServer(getBaseContext(), new InetSocketAddress(Constants.HOST, Constants.PORT));
                server.setMessageListener(ConnectService.this);
                server.run();
            }
        };
        thread.start();
    }

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msgListener != null) {
                msgListener.onMessage(msg.obj.toString());
            }
        }
    };

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (server != null) {
            try {
                server.stop();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onMessage(String text) {
        Message message = new Message();
        message.obj = text;
        handler.sendMessage(message);
    }

    public static void setMessageListener(MessageListener messageListener) {
        msgListener = messageListener;
    }

    public interface MessageListener {
        void onMessage(String message);
    }
}
