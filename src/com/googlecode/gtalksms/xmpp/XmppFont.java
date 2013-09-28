package com.googlecode.gtalksms.xmpp;

public class XmppFont {
    private String mFont = null;
    private String mColor = null;
    String mSize = null;
    
    public XmppFont() {
    }
    
    public XmppFont(String font) {
        mFont = font; 
    }
    
    public XmppFont(String font, String color) {
        mFont = font; 
        mColor = color;
    }
    
    public String toString() {        
        if (mFont != null && mColor == null) {
            return  "font-family:" + this.mFont;
        } else if (mFont != null && mColor != null) {
            return "font-family:" + this.mFont + "; " + "color:" + this.mColor;
        } else if (mFont == null && mColor == null) {
            return "font-family:null";
        } else {
            return "color:" + this.mColor;
        }
    }
}
