package com.kingskys.tcp.listener;

public interface TcpWrapperListener {
    void onReceivedNormal(byte[] data, int length);
    void onReceivedJson(byte[] data, int length);
    void onReceivedFile(String fileName, byte[] data, int off, int length);
    void onDisconnected();
    void onConnected();
}
