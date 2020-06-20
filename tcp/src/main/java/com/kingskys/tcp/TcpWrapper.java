package com.kingskys.tcp;

import com.kingskys.tcp.listener.TcpWrapperListener;

public class TcpWrapper {

    private TcpWrapperImp imp = null;

    /**
     *
     * @param host 地址
     * @param port 端口
     */
    public TcpWrapper(String host, int port) {
        this(host, port, false, true);
    }

    /**
     *
     * @param host 地址
     * @param port 端口
     * @param useBase64 使用base64加密
     * @param bigEndian 使用大端模式
     */
    public TcpWrapper(String host, int port, boolean useBase64, boolean bigEndian) {
        imp = new TcpWrapperImp(host, port, useBase64, bigEndian);
    }

    public void setListener(TcpWrapperListener listener) {
        imp.setListener(listener);
    }

    // 发送普通数据
    public boolean sendNormal(byte[] data) {
        return imp.sendNormal(data);
    }

    // 发送json格式数据
    public boolean sendJson(byte[] data) {
        return imp.sendJson(data);
    }

    /** 发送文件
     *
     * @param fileName 文件名
     * @param data 文件内容
     * @return 是否发送成功
     */
    public boolean sendFile(String fileName, byte[] data) {
        return imp.sendFile(fileName, data);
    }


    // 开始连接
    public boolean connect() {
        return imp.mTcpClient.connect();
    }

    // 是否连接
    public boolean isConnected() {
        return imp.mTcpClient.isConnected();
    }

    // 断开连接
    // 调用此方法不会触发 onDisconnected()
    public void disConnect() {
        imp.mTcpClient.disConnect();
    }

    // 重新连接
    public boolean reConnect() {
        imp.mTcpClient.disConnect();
        return imp.mTcpClient.connect();
    }

}
