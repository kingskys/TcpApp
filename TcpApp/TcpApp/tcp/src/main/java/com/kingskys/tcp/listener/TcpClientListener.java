package com.kingskys.tcp.listener;

public interface TcpClientListener {
    void onReceived(byte[] data, int length);
    void onDisconnected();
    void onConnected();
}
