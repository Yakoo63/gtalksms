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
        StringBuilder sb = new StringBuilder();
        
        sb.append("<font ");
        if (_font != null) {
            sb.append("face=\"" + _font + "\" ");
        }
        if (_color != null) {
            sb.append("color=\"" + _color + "\" ");
        }
        sb.append(">");
        
        return sb.toString();
    }
}
