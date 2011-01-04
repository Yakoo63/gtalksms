package com.googlecode.gtalksms;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.inputmethodservice.InputMethodService;
import android.os.IBinder;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.ExtractedTextRequest;
import android.view.inputmethod.InputConnection;

public class KeyboardInputMethod extends InputMethodService {

    private MainService mainService;

    private ServiceConnection mainServiceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            mainService = ((MainService.LocalBinder) service).getService();
        }

        public void onServiceDisconnected(ComponentName className) {
            mainService = null;
        }
    };

    void send(String msg) {
        if (mainService != null) {
            mainService.send(msg);
        }
    }

    @Override
    public void onStartInput(EditorInfo attribute, boolean restarting) {
        super.onStartInput(attribute, restarting);
        if (mainService != null) {
            mainService._keyboard = this;
        }
        String text = getText();
        
        if (text.length() > 0) {
            if (attribute.label != null) {
                send(attribute.label + " : " + text);
            } else if (attribute.fieldName != null) {
                send(attribute.fieldName + " : " + text);
            } else {
                send(attribute.packageName + " : " + text);
            }
        }
    }

    @Override
    public void onFinishInput() {
        super.onFinishInput();
        if (mainService != null) {
            mainService._keyboard = null;
        }
    }

    @Override
    public void onDestroy() {

        if (mainServiceConnection != null) {
            unbindService(mainServiceConnection);
        }
        mainServiceConnection = null;
        super.onDestroy();
    }

    @Override
    public void onCreate() {
        super.onCreate();

        if (this.bindService(new Intent(MainService.ACTION_CONNECT), mainServiceConnection, BIND_AUTO_CREATE) == false) {
            throw new RuntimeException("failed to connect to mainService");
        }
    }

    boolean setText(String text) {
        InputConnection conn = getCurrentInputConnection();
        if (conn == null) {
            return false;
        }
        conn.beginBatchEdit();
        conn.deleteSurroundingText(100000, 100000);
        conn.commitText(text, text.length());
        conn.endBatchEdit();
        return true;
    }

    String getText() {
        String text = "";
        try {
            InputConnection conn = getCurrentInputConnection();
            ExtractedTextRequest req = new ExtractedTextRequest();
            req.hintMaxChars = 1000000;
            req.hintMaxLines = 10000;
            req.flags = 0;
            req.token = 1;
            text = conn.getExtractedText(req, 0).text.toString();
        } catch (Throwable t) {
        }
        return text;
    }
}
