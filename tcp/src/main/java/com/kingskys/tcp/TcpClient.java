package com.kingskys.tcp;

import com.kingskys.tcp.listener.TcpClientListener;

public class TcpClient {
    private TcpClientImp imp;

    public TcpClient(String host, int port) {
        imp = new TcpClientImp(host, port);
    }

    public String getHost() {
        return imp.getHost();
    }

    public int getPort() {
        return imp.getPort();
    }

    // 设置使用大端模式，默认是大端模式
    public void setBigEndian(boolean b) {
        imp.setBigEndian(b);
    }

    // 返回是否使用大端模式
    public boolean isBigEndian() {
        return imp.isBigEndian();
    }

    // 开始连接
    public boolean connect() {
        return imp.connect();
    }

    // 是否连接
    public boolean isConnected() {
        return imp.isConnected();
    }

    // 断开连接
    // 调用此方法不会触发 onDisconnected()
    public void disConnect() {
        imp.disConnect();
    }

    // 重新连接
    public boolean reConnect() {
        imp.disConnect();
        return imp.connect();
    }

    // 发送数据
    public boolean send(byte[]... datas) {
        return imp.send(datas);
    }

    // 监听
    public void setListener(TcpClientListener listener) {
        imp.setListener(listener);
    }



}
