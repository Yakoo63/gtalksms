package com.googlecode.gtalksms.xmpp;

public class XmppFont {
    String _font = null;
    String _color = null;
    String _size = null;
    
    public XmppFont() {
    }
    
    public XmppFont(String font) {
        _font = font; 
    }
    
    public XmppFont(String font, String color) {
        _font = font; 
        _color = color;
    }
    
    public String toString() {        
        if (_font != null && _color == null) {
            return  "font-family:" + this._font;
        } else if (_font != null && _color != null) {
            return "font-family:" + this._font + " " + "color:" + this._color;
        } else if (_font == null && _color == null) {
            return "font-family:null";
        } else {
            return "color:" + this._color;
        }
    }
}
