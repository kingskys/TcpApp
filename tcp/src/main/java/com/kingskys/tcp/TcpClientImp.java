package com.kingskys.tcp;

import com.kingskys.tcp.common.Value;
import com.kingskys.tcp.listener.TcpClientListener;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;

class TcpClientImp {
    private String m_host = "";
    private int m_port = 0;

    private boolean m_bigEndian = true;

    private InputStream m_inputStream = null;
    private OutputStream m_outputStream = null;
    private TcpClientListener m_listener = null;

    private Socket m_socket = null;

    // 长度位左匹配
    private static byte[] i_patternL = new byte[3];
    // 长度位又匹配
    private static byte[] i_patternR = new byte[3];
    static {
        i_patternL[0] = 'h';
        i_patternL[1] = 'o';
        i_patternL[2] = 't';

        i_patternR[0] = 'i';
        i_patternR[1] = 'n';
        i_patternR[2] = 'g';
    }

    TcpClientImp(String host, int port) {
        m_host = host;
        m_port = port;
    }

    String getHost() {
        return m_host;
    }

    int getPort() {
        return m_port;
    }

    // 设置使用大端模式
    void setBigEndian(boolean b) {
        m_bigEndian = b;
    }

    // 返回是否使用大端模式
    boolean isBigEndian() {
        return m_bigEndian;
    }

    boolean connect() {
        if (isConnected()) {
            return true;
        }

        disConnect();

        m_socket = new Socket();
        try {
            SocketAddress socketAddress = new InetSocketAddress(m_host, m_port);
            m_socket.connect(socketAddress, 6000);
            m_inputStream = m_socket.getInputStream();
            m_outputStream = m_socket.getOutputStream();
            receiveData();
            onConnected();

            return true;
        } catch (Throwable e) {
            log("连接服务器失败：" + e);
            return false;
        }
    }

    boolean isConnected() {
        return m_socket != null && m_socket.isConnected();
    }

    synchronized void disConnect() {
        _onDisconnected();
    }

    private void _onDisconnected() {
        try {
            if (m_outputStream != null) {
                m_outputStream.close();
            }
        } catch (Throwable ignored) {}

        try {
            if (m_inputStream != null) {
                m_inputStream.close();
            }
        } catch (Throwable ignored) {}

        try {
            if (m_socket != null) {
                m_socket.close();
            }
        } catch (Throwable ignored) {}

        m_outputStream = null;
        m_inputStream = null;
        m_socket = null;
    }

    private boolean matchHead(byte[] data, Value<Integer> length) {
        for (int i = 0; i < i_patternL.length; i++) {
            if (data[i] != i_patternL[i]) {
                return false;
            }
        }

        int idx = i_patternL.length + 4;
        for (int i = 0; i < i_patternR.length; i++) {
            if (data[i + idx] != i_patternR[i]) {
                return false;
            }
        }

        int n = TcpTools.bytes2int(data, i_patternL.length, true);
        length.set(n);

        return true;
    }

    // buf 用于读取数据
    // dataLen 需要读取的数据长度
    // length 返回解析到的数据体长度
    private boolean readHead(byte[] buf, int dataLen, Value<Integer> length) {
        try {
//            int len = m_inputStream.read(buf, 0, dataLen);
            int len = read(buf, 0, dataLen);
            if (len < 0) {
                log("收到数据头长度错误 len = " + len + "，与服务器断开了连接");
                return false;
            } else if (len != dataLen) {
                log("收到数据头非法 len = " + len + ", 应为：" + dataLen);
                return false;
            } else {
//                log("收到数据头长度 len = " + len);
                if (matchHead(buf, length)) {
                    return true;
                }
                log("数据头格式错误");
                return false;
            }
        } catch (Throwable e) {
            log("读取数据头异常 err = " + e);
            return false;
        }
    }

    // buf 用于读取数据
    // dataLen 需要读取的数据长度
    // length 返回解析到的数据体长度
    private boolean readBody(byte[] buf, int dataLen) {
        return readBody(buf, 0, dataLen);
    }

    // buf 用于读取数据
    // off buf写入的偏移
    // dataLen 需要读取的数据长度
    // length 返回解析到的数据体长度
    private boolean readBody(byte[] buf, int off, int dataLen) {
        try {
//            int len = m_inputStream.read(buf, off, dataLen);
            int len = read(buf, off, dataLen);
            if (len < 0) {
                log("读取数据体长度错误 len = " + len + "，与服务器断开了连接");
                return false;
            } else if (len < dataLen) {
//                float per = 1.0f * (off + len) / buf.length;
//                log("" + per);
                log("缓存数据不足，继续读取 本次读取：" + len + ", 本次需要：" + dataLen + "，之前读取：" + off);
                return readBody(buf, off + len, dataLen - len);
            } else if (len == dataLen) {
//                log("读取完成，最后读取长度 len = " + len);
                return true;
            } else {
                log("读取缓存系统异常 本次读取：" + len + ", 需要读取：" + dataLen);
                return false;
            }
        } catch (Throwable e) {
            log("读取数据体异常 err = " + e);
            return false;
        }
    }

    private int read(byte b[], int off, int len) throws IOException {
        if (b == null) {
            throw new NullPointerException();
        } else if (off < 0 || len < 0 || len > b.length - off) {
            throw new IndexOutOfBoundsException();
        } else if (len == 0) {
            return 0;
        }

        int c = m_inputStream.read();
        if (c == -1) {
            return -1;
        }
        b[off] = (byte)c;

        int i = 1;
        try {
            for (; i < len ; i++) {
                c = m_inputStream.read();
                if (c == -1) {
                    break;
                }
                b[off + i] = (byte)c;
            }
        } catch (IOException ee) {
            log("read err = " + ee);
        }
        return i;
    }

    private void receiveData() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                doReceiveData();
            }
        }).start();
    }

    private void doReceiveData() {
        Value<Integer> len = new Value<Integer>();
        int headLen = i_patternL.length + i_patternR.length + 4;
        byte[] headBuf = new byte[headLen];
        while (true) {
            try {
                if (!readHead(headBuf, headLen, len)) {
                    break;
                }

//                log("数据体长度为：" + len.get());

                if (len.get() == 0) {
                    // 空的数据，不做处理
//                    log("空的数据");
                } else if (len.get() > 0) { // 获取到正确的数据长度
                    byte[] buf = new byte[len.get()];
                    if (readBody(buf, len.get())) {
                        onReceivedData(buf);
                    } else {
                        break;
                    }

                } else {
                    // 错误
                    log("收到错误的数据长度 len = " + len);
                    break;
                }

            } catch (Throwable e) {
                log("读取数据异常 err = " + e);
                break;
            }
        }

        onDisconnected();
    }

    private void onReceivedData(final byte[] data) {
//        log("onReceivedData - len = " + data.length);
//        log("data - " + data);
        if (m_listener != null) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    m_listener.onReceived(data, data.length);
                }
            }).start();
        }
    }

    private void onDisconnected() {
        disConnect();
        if (m_listener != null) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    m_listener.onDisconnected();
                }
            }).start();
        }
    }

    private void onConnected() {
        if (m_listener != null) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    m_listener.onConnected();
                }
            }).start();
        }
    }

    boolean send(byte[]... datas) {
        try {
            if (m_outputStream == null) {
                return false;
            }

            if (datas == null || datas.length == 0) {
                return false;
            }

            int len = 0;
            for (byte[] data : datas) {
                if (data != null) {
                    len += data.length;
                }
            }

            writeHead(len);
            for (byte[] data : datas) {
                if (data != null && data.length > 0) {
                    writeBody(data);
                }
            }

            m_outputStream.flush();
            return true;
        } catch (Throwable e) {
            log("发送数据失败 err = " + e);
            return false;
        }
    }

    boolean canSend() {
        return m_outputStream != null;
    }

    void flush() throws Throwable {
        m_outputStream.flush();
    }

    private void writeHead(int len) throws Throwable {
        try {
            // 数据头左标记
            for (byte b : i_patternL) {
                m_outputStream.write(b);
            }

            // 数据体长度
            byte[] bodyLen = TcpTools.int2bytes(len, m_bigEndian);
            for (byte b : bodyLen) {
                m_outputStream.write(b);
            }

            // 数据头右标记
            for (byte b : i_patternR) {
                m_outputStream.write(b);
            }

        } catch (Throwable e) {
            throw e;
        }
    }

    private void writeBody(byte[] data) throws Throwable {
        try {
            // 数据体
            for (byte b : data) {
                m_outputStream.write(b);
            }
        } catch (Throwable e) {
            throw e;
        }
    }

//    private byte[] formatSendData(byte[] data) {
//        int headLen = i_patternL.length + i_patternR.length + 4;
//        byte[] buf = new byte[data.length + headLen];
//        // 数据头左
//        System.arraycopy(i_patternL, 0, buf, 0, i_patternL.length);
//        // 数据头右
//        System.arraycopy(i_patternR, 0, buf, i_patternL.length + 4, i_patternR.length);
//        // 数据头记录数据体长度部分
//        System.arraycopy(TcpTools.int2bytes(data.length, m_bigEndian), 0, buf, i_patternL.length, 4);
//        // 数据体
//        System.arraycopy(data, 0, buf, headLen, data.length);
//        return buf;
//    }

    void setListener(TcpClientListener listener) {
        m_listener = listener;
    }

    private static void log(String msg) {
        Const.log("TcpClient", msg);
    }

}
