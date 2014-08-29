package com.googlecode.gtalksms.services;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.inputmethodservice.InputMethodService;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.os.IBinder;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.ExtractedTextRequest;
import android.view.inputmethod.InputConnection;

import com.googlecode.gtalksms.MainService;
import com.googlecode.gtalksms.R;

public class KeyboardInputMethodService extends InputMethodService
    implements KeyboardView.OnKeyboardActionListener {

    private MainService mainService;
    private KeyboardView _inputView;
    private int _lastDisplayWidth;
    private Keyboard _keyboard;
    
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
            mainService.send(msg, null);
        }
    }
    
    @Override public View onCreateInputView() {
        _inputView = (KeyboardView) getLayoutInflater().inflate(R.layout.keyboard, null);
        _inputView.setOnKeyboardActionListener(this);
        _inputView.setKeyboard(_keyboard);
        return _inputView;
    }
    
    @Override public void onInitializeInterface() {
        if (_keyboard != null) {
            // Configuration changes can happen after the keyboard gets recreated,
            // so we need to be able to re-build the keyboards if the available
            // space has changed.
            int displayWidth = getMaxWidth();
            if (displayWidth == _lastDisplayWidth) return;
            _lastDisplayWidth = displayWidth;
        }
        _keyboard = new Keyboard(this, R.xml.keyboard);
    }

    @Override
    public void onStartInput(EditorInfo attribute, boolean restarting) {
        super.onStartInput(attribute, restarting);
        if (mainService != null) {
            mainService.setKeyboard(this);
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
    
    @Override public void onStartInputView(EditorInfo attribute, boolean restarting) {
        super.onStartInputView(attribute, restarting);
        // Apply the selected keyboard to the input view.
        _inputView.setKeyboard(_keyboard);
        _inputView.closing();
    }

    @Override
    public void onFinishInput() {
        super.onFinishInput();
        if (mainService != null) {
            mainService.setKeyboard(null);
        }
        
        if (_inputView != null) {
            _inputView.closing();
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

        if (!this.bindService(new Intent(this, MainService.class), mainServiceConnection, BIND_AUTO_CREATE)) {
            throw new RuntimeException("failed to connect to mainService");
        }
    }

    public boolean setText(String text) {
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

    public String getText() {
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

    @Override
    public void onKey(int arg0, int[] arg1) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void onPress(int primaryCode) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void onRelease(int primaryCode) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void onText(CharSequence text) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void swipeDown() {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void swipeLeft() {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void swipeRight() {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void swipeUp() {
        // TODO Auto-generated method stub
        
    }
}
