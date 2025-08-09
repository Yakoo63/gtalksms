package com.googlecode.gtalksms.tools;

import java.util.Arrays;
import java.util.List;

import com.googlecode.gtalksms.xmpp.XmppMsg;

import android.text.SpannableStringBuilder;
import android.text.style.CharacterStyle;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.text.style.URLSpan;

public class StringFmt {

    public static String join(String[] list, String sep) {
        return join(Arrays.asList(list), sep);
    }
    
    public static String join(String[] list, String sep, boolean bold) {
        return join(Arrays.asList(list), sep, bold);
    }
    
    public static String makeBold(String msg) {
        return XmppMsg.makeBold(msg);
    }

    public static String join(List<String> list, String sep) {
        return join(list, sep, false);
    }
    
    public static String join(List<String> list, String sep, boolean bold) {
        String res = "";
        
        for (String s : list) {
            res += (bold ? makeBold(s) : s) + sep;
        }
        
        return delLastChar(res, sep.length());
    }

    public static String delLastChar(String s) {
        return delLastChar(s, 1);
    }
    
    public static String delLastChar(String s, int nb) {
        try {
            return s.substring(0, s.length() - nb);
        } catch (Exception e) {
            return "";
        }
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
    
    public static CharSequence Url(CharSequence str, CharSequence link) {
        return format(str, new URLSpan(link.toString()));
    }
    
    private static CharSequence format(CharSequence text, CharacterStyle... cs) {
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
