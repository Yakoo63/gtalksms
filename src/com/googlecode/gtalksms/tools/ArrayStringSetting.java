package com.googlecode.gtalksms.tools;

import android.text.TextUtils;

import com.googlecode.gtalksms.SettingsManager;

import java.util.ArrayList;

/**
 * Created by Florent on 02/10/13.
 */
public class ArrayStringSetting {
    private String _string;
    private String _key;
    private String _separator;
    private SettingsManager _settingsManager;
    private final ArrayList<String> _strings = new ArrayList<String>();

    public ArrayStringSetting(String key, SettingsManager settingsManager)
    {
        this(key, "|", settingsManager);
    }

    public ArrayStringSetting(String key, String separator, SettingsManager settingsManager) {
        _key = key;
        _separator = separator;
        _settingsManager = settingsManager;
    }

    public String[] getAll() {
        return _strings.toArray(new String[_strings.size()]);
    }

    public String get() {
        return _string;
    }

    public String getKey() {
        return _key;
    }

    public void set(String value) {
        if (_string == null || !_string.equals(value)) {
            _string = _settingsManager.saveSetting(_key, value);
        }
        update();
    }

    void update() {
        _strings.clear();
        for (String str : TextUtils.split(_string, "\\" + _separator)) {
            _strings.add(str);
        }
    }

    public boolean contains(String value) {
        for (String s: _strings)
        {
            if (value.toLowerCase().equals(s.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    public void add(String value) {
        if (! contains(value)) {
            _strings.add(value);
        }
        set(TextUtils.join(_separator, _strings));
    }

    public void remove(String value) {
        if (contains(value)) {
            _strings.remove(value);
        }
        set(TextUtils.join(_separator, _strings));
    }
}
