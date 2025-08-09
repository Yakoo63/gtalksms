package com.googlecode.gtalksms.tools;

import android.content.Context;
import android.preference.EditTextPreference;
import android.util.AttributeSet;

/** Used to put an integer in the preferences string (e.g. for server port)*/

public class EditIntegerPreference extends EditTextPreference {
    
    public EditIntegerPreference(Context context) { 
        super(context); 
    }
    
    public EditIntegerPreference(Context context, AttributeSet attrs) { 
        super(context, attrs); 
    }
    
    public EditIntegerPreference(Context context, AttributeSet attrs, int defStyle) { 
        super(context, attrs, defStyle); 
    }
    
    @Override 
    public String getText() { 
        return String.valueOf(getSharedPreferences().getInt(getKey(), 0)); 
    }
    
    @Override 
    public void setText(String text) {
        try {
            getSharedPreferences().edit().putInt(getKey(), Integer.parseInt(text)).commit();
        } catch (Exception ex) {
            Log.w("Error while updating EditIntegerPreference: text=" + text, ex);
        }
    }
    
    @Override 
    protected void onSetInitialValue(boolean restoreValue, Object defaultValue) { 
        if (restoreValue) getEditText().setText(getText()); 
        else super.onSetInitialValue(restoreValue, defaultValue); 
    }
    
} 