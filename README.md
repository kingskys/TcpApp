# TcpApp
Android Tcp 封装

# gradle 引用tcp模块
<pre>
allprojects {
    repositories {
        ...
        
        maven { url "https://jitpack.io" }
    }
}

dependencies {
    ...

    implementation 'com.github.kingskys:TcpApp:v1.1.7'
}
</pre>

TcpWrapper简单示例
----------------
```
# 创建
private TcpWrapper createTcp() {
    String host = "192.168.31.33"; // 要用自己的服务器的地址
    TcpWrapper tcpWrapper = new TcpWrapper(host, 11001, useBase64, true);
    tcpWrapper.setListener(new TcpWrapperListener() {
        @Override
        public void onReceivedNormal(byte[] data, int length) {
            log("收到了普通数据");
        }

        @Override
        public void onReceivedJson(byte[] data, int length) {
            log("收到服务器json数据：" + new String(data));
        }

        @Override
        public void onReceivedFile(String fileName, byte[] data, int off, int length) {
            log("收到了文件数据");
        }

        @Override
        public void onDisconnected() {
            log("与服务器断开了连接");
        }

        @Override
        public void onConnected() {
            log("与服务器建立了连接");
        }
    });

    return tcpWrapper;
}

# 发送json数据
private void sendJsonData(final TcpWrapper tcpWrapper, final byte[] data) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (!tcpWrapper.isConnected()) {
                    if (!tcpWrapper.connect()) {
                        log("连接服务器失败");
                        return;
                    }
                }
                if (!tcpWrapper.sendJson(data)) {
                    log("发送失败");
                }
            }
        }).start();
    }
```

TcpClient内部发送数据协议
-----------------------
数据头 = "hot" + 数据体长度 + "ing"
数据体 = 数据体数据

读取时，先读10个字节的数据头，匹配是不是 ("hot" + 数据体长度 + "ing") 的格式，然后拿到数据体长度，再读取数据体
写入时，同理 ("hot" + 数据体长度 + "ing" + 数据体数据)

TcpWrapper封装
---------------
* TcpClient可以单独使用
* TcpWrapper是对TcpClient的封装
* 具体使用参考源码
