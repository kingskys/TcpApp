package com.kingskys.tcp;

class TcpToolsImp {

    static byte int2byte(int v, int off) {
        return (byte) ((v >> (off*8)) & 0xFF);
    }

    // 字节数组转成int，小端模式
    static int bytes2intL(byte[] data) {
        return makeInt(data[3],
                data[2],
                data[1],
                data[0]);
    }

    // 字节数组转成int，使用大端模式
    static int bytes2intB(byte[] data) {
        return makeInt(data[0],
                data[1],
                data[2],
                data[3]);
    }

    private static int makeInt(byte b3, byte b2, byte b1, byte b0) {
        return (((b3 & 0xff) << 24) |
                ((b2 & 0xff) << 16) |
                ((b1 & 0xff) << 8) |
                ((b0 & 0xff)));
    }
}
