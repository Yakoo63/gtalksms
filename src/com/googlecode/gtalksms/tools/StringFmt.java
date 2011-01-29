package com.googlecode.gtalksms.tools;

import android.text.SpannableStringBuilder;
import android.text.style.CharacterStyle;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.text.style.URLSpan;

public class StringFmt {
    
    public static String encodeHTML(String s) {
        StringBuffer out = new StringBuffer();
        for (int i = 0 ; i < s.length() ; i++) {
            char c = s.charAt(i);
            if (c > 127 || c == '"' || c == '<' || c == '>') {
                out.append("&#" + (int)c + ";");
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }

    public static String encodeSQL(String s) {
        return s.replaceAll("'", "''");
    }

    public static CharSequence Fmt(CharSequence str, int color, Double size, int style) {
        return format(str, new ForegroundColorSpan(color), new RelativeSizeSpan(size.floatValue()), 
                new StyleSpan(style));
    }
    
    public static CharSequence Style(CharSequence str, int style) {
        return format(str, new StyleSpan(style));
    }
    
    public static CharSequence Url(CharSequence str) {
        return format(str, new URLSpan(str.toString()));
    }
    
    public static CharSequence format(CharSequence text, CharacterStyle... cs) {
        // Copy the spannable string to a mutable spannable string
        SpannableStringBuilder ssb = new SpannableStringBuilder(text);
        for (CharacterStyle c : cs) {
            ssb.setSpan(c, 0, text.length(), 0);
        }
        
        return ssb;
    }
    
    // TODO Iterate on all tokens
    public static CharSequence formatBetweenTokens(CharSequence text, String token, CharacterStyle... cs) {
        // Start and end refer to the points where the span will apply
        int tokenLen = token.length();
        int start = text.toString().indexOf(token) + tokenLen;
        int end = text.toString().indexOf(token, start);

        if (start > -1 && end > -1) {
            // Copy the spannable string to a mutable spannable string
            SpannableStringBuilder ssb = new SpannableStringBuilder(text);
            for (CharacterStyle c : cs) {
                ssb.setSpan(c, start, end, 0);
            }
            
            // Delete the tokens before and after the span
            ssb.delete(end, end + tokenLen);
            ssb.delete(start - tokenLen, start);

            text = ssb;
        }

        return text;
    }
}
