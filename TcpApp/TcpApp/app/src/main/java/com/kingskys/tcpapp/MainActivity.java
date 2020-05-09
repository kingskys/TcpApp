package com.kingskys.tcpapp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

import com.kingskys.tcp.TcpWrapper;
import com.kingskys.tcp.listener.TcpWrapperListener;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private SimpleCursorAdapter m_adapter = null;
    private EditText mInputMsg = null;

    private static final int SELECT_FILE_CODE = 1;
    private static final boolean useBase64 = false;

    private TcpWrapper[] mTcpWrappers = new TcpWrapper[1];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initView();
        requestPermission();

    }

    private static String[] permissions = {
            "android.permission.READ_EXTERNAL_STORAGE",
            "android.permission.WRITE_EXTERNAL_STORAGE",
            "android.permission.CAMERA",
            "android.permission.RECORD_AUDIO",
            "android.permission.READ_PHONE_STATE"
    };

    private void requestPermission() {
        ActivityCompat.requestPermissions(this, permissions, 2);
    }

    private void initView() {
        findViewById(R.id.btn_send).setOnClickListener(this);
        findViewById(R.id.btn_file).setOnClickListener(this);

        mInputMsg = findViewById(R.id.input_msg);

        // 删除数据库内容
        LogProviderTools.deleteAll(getApplicationContext());

        createTcps();

        initListView();
    }

    private void createTcps() {
        int len = mTcpWrappers.length;
        for (int i = 0; i < len; i++) {
            mTcpWrappers[i] = createTcp();
        }
    }

    // 创建tcp
    private TcpWrapper createTcp() {
        TcpWrapper tcpWrapper = new TcpWrapper("192.168.31.123", 11001, useBase64, true);
        tcpWrapper.setListener(new TcpWrapperListener() {
            @Override
            public void onReceivedNormal(byte[] data, int length) {
                processNormalMsg(data);
            }

            @Override
            public void onReceivedJson(byte[] data, int length) {
                processJsonMsg(data);
            }

            @Override
            public void onReceivedFile(String fileName, byte[] data, int off, int length) {
                processFileMsg(fileName, data, off, length);
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

    private void processNormalMsg(byte[] data) {
        log("收到服务器普通数据：" + new String(data));
        LogProviderTools.insert(getApplicationContext(), System.currentTimeMillis(), new String(data));
        updateList();
    }

    private void processJsonMsg(byte[] data) {
        log("收到服务器json数据：" + new String(data));
        LogProviderTools.insert(getApplicationContext(), System.currentTimeMillis(), new String(data));
        updateList();
    }

    private static int i_idx = 0;
    private synchronized int nextIdx() {
        i_idx++;
        return i_idx;
    }

    private void processFileMsg(String fileName, byte[] data, int off, int length) {
        log("收到文件：" + fileName + "，数据长度：" + data.length);
        fileName = "" + nextIdx() + "_" + fileName;
        saveFile(fileName, data, off, length);
    }

    // 保存文件
    private boolean saveFile(String fileName, byte[] data, int off, int length) {
        File file = null;
        try {
            try {
                file = Environment.getExternalStorageDirectory();
                String path = file.getAbsolutePath() + File.separator + "tcp_download";
                file = new File(path);
            } catch (Throwable e) {
                file = null;
            }

            if (file == null) {
                file = getApplicationContext().getExternalFilesDir("tcp_download");
            }

            if (file == null) {
                log("打开存储路径失败！");
                return false;
            }

            if (!file.exists()) {
                if (!file.mkdirs()) {
                    log("创建存储目录失败，目录：" + file.getAbsolutePath());
                    return false;
                }
            }

            String filePath = file.getAbsolutePath() + File.separator + fileName;
            log("保存文件路径为：" + filePath);

            file = new File(filePath);
            if (file.exists()) {
                if (file.delete()) {
                    log("删除文件成功");
                }
            }

            FileOutputStream outputStream = new FileOutputStream(file);
            outputStream.write(data, off, length);
            outputStream.flush();

            outputStream.close();

            log("保存文件成功");
            return true;

        } catch (Throwable e) {
            log("save file error = " + e);
            return false;
        }

    }

    private void initListView() {
        ListView listView = findViewById(R.id.list_msg);
        m_adapter = new SimpleCursorAdapter(this, R.layout.item, null, new String[] {"name"}, new int[] {R.id.label_info}, 0);
        listView.setAdapter(m_adapter);
        updateList();
    }

    // 刷新listview
    private void updateList() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Cursor cursor = LogProviderTools.getQueryCursor(getApplicationContext());
                m_adapter.swapCursor(cursor);
            }
        });
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_send: {
                onSendEvent();
                break;
            }
            case R.id.btn_file: {
                onFileButtonEvent();
                break;
            }

        }
    }

    private void onSendEvent() {
        log("准备发送数据");
        String data = mInputMsg.getText().toString().trim();
        sendNormalData(data);
    }

    private void onFileButtonEvent() {
//        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        this.startActivityForResult(intent, SELECT_FILE_CODE);
    }

    private void sendNormalData(String data) {
        byte[] b = data.getBytes();
        for (TcpWrapper tcpWrapper : mTcpWrappers) {
            sendNormalData(tcpWrapper, b);
        }
    }

    private void sendNormalData(final TcpWrapper tcpWrapper, final byte[] data) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (!tcpWrapper.isConnected()) {
                    if (!tcpWrapper.connect()) {
                        log("连接服务器失败");
                        return;
                    }
                }
                if (!tcpWrapper.sendNormal(data)) {
                    log("发送失败");
                }
            }
        }).start();
    }

    private void sendFileData(final String fileName, byte[] data) {
        for (TcpWrapper tcpWrapper : mTcpWrappers) {
            sendFileData(tcpWrapper, fileName, data);
        }
    }

    private void sendFileData(final TcpWrapper tcpWrapper, final String fileName, final byte[] data) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                log("开始发送文件");
                if (!tcpWrapper.isConnected()) {
                    log("当前未连接");
                    if (!tcpWrapper.connect()) {
                        log("连接服务器失败");
                        return;
                    }
                } else {
                    log("当前已连接");
                }

                if (!tcpWrapper.sendFile(fileName, data)) {
                    log("发送文件失败！！！");
                } else {
                    log("发送文件成功！！！");
                }
            }
        }).start();
    }


    private static void log(String msg) {
        Log.w("AAA", "TcpApp - " + msg);
    }

    private void sendFile(String filepath) {
        log("send file - " + filepath);
        try {
            File f = new File(filepath);
            if (!f.exists()) {
                log("文件不存在");
                return;
            }

            sendFile(f);

        } catch (Throwable e) {
            log("send file (filepath) err = " + e);
        }

    }

    private File createFile(String filepath) {
        log("readFile - " + filepath);
        if (filepath == null || filepath.isEmpty()) {
            log("空文件路径");
            return null;
        }
        try {
            File f = new File(filepath);
            if (!f.exists()) {
                log("文件(" + filepath + ")不存在");
                return null;
            }

            log("文件(" + filepath + ") 存在");
            return f;
        } catch (Throwable e) {
            log("read file (" + filepath + ") err = \n" + e);
            return null;
        }
    }

    private void sendFile(Uri uri) {
        log("send file uri");
        try {
            String filepath = UriUtil.getPath(getApplicationContext(), uri);
            File f = createFile(filepath);
            if (f != null) {
                sendFile(f);
            }

        } catch (Throwable e) {
            log("send file (uri) err = " + e);
        }

    }


    // 当文件大小超过app允许的最大运行时内存，会报错
    private void sendFile(File f) {
        log("send file - " + f.getName());
        log("文件大小 = " + f.length());

        try {
            String name = f.getName();

            if (f.length() + name.length() + 9 > Integer.MAX_VALUE) {
                log("存储文件过大");
                return;
            }

            int dataLen = (int)f.length();

            FileInputStream fileInputStream = new FileInputStream(f);

            byte[] data;

            try {
                data = new byte[dataLen];
            } catch (OutOfMemoryError e) {
                log("运行内存不足");
                return;
            }

            if (!readFile(fileInputStream, data, 0, dataLen)) {
                log("读取文件失败");
                return;
            }

            fileInputStream.close();

            sendFileData(name, data);

        } catch (Throwable e) {
            log("send file err = " + e);
        }

    }

    private boolean readFile(FileInputStream fileInputStream, byte[] buf, int off, int len) {
        try {
            if (off + len > buf.length) {
                return false;
            }

            int n = 0;
            while ((n = fileInputStream.read(buf, off, len - off)) != -1) {
                off += n;
                if (off >= len) {
                    break;
                }
            }

            return (off == len);

        } catch (Throwable e) {
            log("读取文件错误：" + e);
            return false;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SELECT_FILE_CODE) {
            if (data == null) {
                // 未选择文件
                return;
            }
            Uri uri = data.getData(); // 获取文件URI
            log("uri = " + uri);
            log("uri path = " + uri.getPath());

            ContentResolver resolver = this.getContentResolver();
            Cursor cursor = resolver.query(uri, null, null, null, null);
            if (cursor == null) {
                sendFile(uri.getPath());
                return;
            }

            if (cursor.moveToFirst()) {
                sendFile(uri);
                cursor.close();
            }
        }
    }
}
