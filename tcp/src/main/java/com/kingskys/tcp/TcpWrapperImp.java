package com.kingskys.tcp;

import android.util.Log;

import com.kingskys.tcp.listener.TcpClientListener;
import com.kingskys.tcp.listener.TcpWrapperListener;

import static com.kingskys.tcp.TcpTools.encodeBase64;
import static com.kingskys.tcp.TcpTools.joinBytes;

public class TcpWrapperImp {

    private static final byte Type_MSG_Null = 0;
    private static final byte Type_MSG_Normal = 1;
    private static final byte Type_MSG_Json = 2;
    private static final byte Type_MSG_File = 3;

    TcpClient mTcpClient = null;
    private boolean mUseBase64 = false;

    private TcpWrapperListener m_listener = null;

    TcpWrapperImp(String host, int port, boolean useBase64, boolean bigEndian) {
        mTcpClient = new TcpClient(host, port);
        mUseBase64 = useBase64;
        mTcpClient.setBigEndian(bigEndian);

        mTcpClient.setListener(new TcpClientListener() {
            @Override
            public void onReceived(byte[] data, int length) {
                if (mUseBase64) {
                    data = TcpTools.decodeBase64(data);
                    length = data.length;
                }

                processReceivedMsg(data, length);
            }

            @Override
            public void onDisconnected() {
                if (m_listener != null) {
                    m_listener.onDisconnected();
                }
            }

            @Override
            public void onConnected() {
                if (m_listener != null) {
                    m_listener.onConnected();
                }
            }
        });
    }

    private void processReceivedMsg(byte[] data, int length) {
        byte type = data[0];
        switch (type) {
            case Type_MSG_Null: {
                break;
            }
            case Type_MSG_Normal: {
                log("收到普通的消息类型");
                processNormalMsg(data);
                break;
            }
            case Type_MSG_Json: {
                log("收到json的消息类型");
                processJsonMsg(data);
                break;
            }
            case Type_MSG_File: {
                log("收到文件的消息类型");
                processFileMsg(data);
                break;
            }
            default: {
                log("收到未知消息类型：" + (int)type);
            }
        }
    }

    private void processNormalMsg(byte[] data) {
        log("收到普通数据长度为：" + data.length);
        if (data.length < 1) {
            return;
        }

        if (m_listener != null) {
            byte[] newData = new byte[data.length - 1];
            System.arraycopy(data, 1, newData, 0,data.length-1);

            m_listener.onReceivedNormal(newData, newData.length);
        }
    }

    private void processJsonMsg(byte[] data) {
        log("收到json数据长度为：" + data.length);
        if (data.length < 1) {
            return;
        }

        if (m_listener != null) {
            byte[] newData = new byte[data.length - 1];
            System.arraycopy(data, 1, newData, 0,data.length-1);

            m_listener.onReceivedJson(newData, newData.length);
        }
    }

    private void processFileMsg(byte[] data) {
        log("收到文件数据长度：" + data.length);
        if (data.length < 5) {
            return;
        }

        if (m_listener != null) {
            int nameLen = TcpTools.bytes2int(data, 1, mTcpClient.isBigEndian());
            if (nameLen < 1) {
                log("文件名长度异常 长度：" + nameLen);
                return;
            }
            log("文件名 长度：" + nameLen);
            byte[] nameBytes = new byte[nameLen];
            System.arraycopy(data, 5, nameBytes, 0, nameLen);
            String fileName = new String(nameBytes);
            log("文件名：" + fileName);

            int bodyLen = data.length - 5 - nameLen;
            log("文件内容大小：" + bodyLen);
//            byte[] bodyData = new byte[bodyLen];
//            System.arraycopy(data, 5 + nameLen, bodyData, 0, bodyLen);
            m_listener.onReceivedFile(fileName, data, 5 + nameLen, bodyLen);
        }
    }

    // 发送普通数据
    boolean sendNormal(byte[] data) {
        if (mUseBase64) {
            data = encodeData(Type_MSG_Normal, data);
            return mTcpClient.send(data);
        } else {
            byte[] msgType = {Type_MSG_Normal};
            return mTcpClient.send(msgType, data);
        }
    }

    // 发送json格式数据
    boolean sendJson(byte[] data) {
        if (mUseBase64) {
            data = encodeData(Type_MSG_File, data);
            return mTcpClient.send(data);
        } else {
            byte[] msgType = {Type_MSG_Json};
            return mTcpClient.send(msgType, data);
        }
    }

    /** 发送文件
     *
     * @param fileName 文件名
     * @param data 文件内容
     * @return 是否发送成功
     */
    boolean sendFile(String fileName, byte[] data) {
        try {
            byte[] nameBytes = fileName.getBytes();
            byte[] nameLen = TcpTools.int2bytes(nameBytes.length, mTcpClient.isBigEndian());

            if (mUseBase64) { // 文件不用base64
                return false;
            } else {
                byte[] msgType = {Type_MSG_File};
                return mTcpClient.send(msgType, nameLen, nameBytes, data);
            }
        } catch (Throwable e) {
            return false;
        }
    }

    // 编码
    private byte[] encodeData(byte msgType, byte[] data) {
        data = joinBytes(msgType, data);
        data = encodeBase64(data);
        return data;
    }

    void setListener(TcpWrapperListener listener) {
        m_listener = listener;
    }

    private static void log(String msg) {
        Log.w("tcp-wrapper", msg);
    }
}
