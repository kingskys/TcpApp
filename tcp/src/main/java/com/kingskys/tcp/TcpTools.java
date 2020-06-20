package com.kingskys.tcp;


import android.util.Base64;

public class TcpTools {
    public static void setLogFlag(String flag) {
        Const.logFlag = flag;
    }

    // 将int转换成4个字节的byte数组
    static byte[] int2bytes(int v, boolean isBigEndian) {
        byte[] buf = new byte[4];
        if (isBigEndian) {
            buf[0] = TcpToolsImp.int2byte(v, 3);
            buf[1] = TcpToolsImp.int2byte(v, 2);
            buf[2] = TcpToolsImp.int2byte(v, 1);
            buf[3] = TcpToolsImp.int2byte(v, 0);
        } else {
            buf[0] = TcpToolsImp.int2byte(v, 1);
            buf[1] = TcpToolsImp.int2byte(v, 2);
            buf[2] = TcpToolsImp.int2byte(v, 3);
            buf[3] = TcpToolsImp.int2byte(v, 4);
        }
        return buf;
    }

    // byte数组转成int
    static int bytes2int(byte[] src, int pos, boolean isBigEndian) {
        if (pos > 0) {
            byte[] buf = new byte[4];
            System.arraycopy(src, pos, buf, 0, 4);
            src = buf;
        }

        if (isBigEndian) {
            return TcpToolsImp.bytes2intB(src);
        } else {
            return TcpToolsImp.bytes2intL(src);
        }
    }

    // 转成base64编码
    static byte[] encodeBase64(byte[] data) {
        return Base64.encode(data, Base64.NO_WRAP);
    }

    static byte[] decodeBase64(byte[] data) {
        return Base64.decode(data, Base64.NO_WRAP);
    }

    // 拼接byte数组

    static byte[] joinBytes(byte[] data1, byte[] data2) {
        return joinBytes(data1, 0, data1.length, data2, 0, data2.length);
    }

    static byte[] joinBytes(byte[] data1, int pos1, int len1, byte[] data2, int pos2, int len2) {
        int length = len1 + len2;
        byte[] newData = new byte[length];
        System.arraycopy(data1, pos1, newData, 0, len1);
        System.arraycopy(data2, pos2, newData, len1, len2);
        return newData;
    }

    // 拼接 data1字节 和 data2字节数组
    static byte[] joinBytes(byte data1, byte[] data2) {
        return joinBytes(data1, data2, 0, data2.length);
    }

    // 拼接 data1字节 和 data2[pos2 : pos2+len2]
    static byte[] joinBytes(byte data1, byte[] data2, int pos2, int len2) {
        int len1 = 1;
        int length = len1 + len2;
        byte[] newData = new byte[length];
        newData[0] = data1;
        System.arraycopy(data2, pos2, newData, len1, len2);
        return newData;
    }

    // 拼接 data2字节 到 data1字节数组后面
    static byte[] joinBytes(byte[] data1, byte data2) {
        return joinBytes(data1, 0, data1.length, data2);
    }

    // 拼接 data1[pos1: pos1+len1] 和 data2字节
    static byte[] joinBytes(byte[] data1, int pos1, int len1, byte data2) {
        int len2 = 1;
        int length = len1 + len2;
        byte[] newData = new byte[length];
        System.arraycopy(data1, pos1, newData, 0, len1);
        newData[len1] = data2;
        return newData;
    }

}
