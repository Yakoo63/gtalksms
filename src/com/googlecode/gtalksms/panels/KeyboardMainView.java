package com.googlecode.gtalksms.panels;

import android.content.Context;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.inputmethodservice.Keyboard.Key;
import android.util.AttributeSet;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

public class KeyboardMainView extends KeyboardView {

    public KeyboardMainView(Context context, AttributeSet attrs, int defStyle) {
        this(context, attrs);
    }

    public KeyboardMainView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected boolean onLongPress(Key key) {
        InputMethodManager  inputMethodManager = (InputMethodManager) getContext().getApplicationContext().getSystemService(getContext().INPUT_METHOD_SERVICE);
        if (inputMethodManager != null) {
            inputMethodManager.showInputMethodPicker();
        }
        return true;
    }
}
