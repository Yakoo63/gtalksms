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
        if (mFont != null) {
            return  "font-family:" + this.mFont + (mColor == null ? "" : "; " + "color:" + this.mColor);
        } else if (mColor != null) {
            return "color:" + this.mColor;
        } else {
            return "font-family:null";
        }
    }
}
