package com.googlecode.gtalksms.xmpp;

import java.util.ArrayList;

import android.util.Log;

import com.googlecode.gtalksms.tools.StringFmt;
import com.googlecode.gtalksms.tools.Tools;

public class XmppMsg {
    public final static String BoldBegin = "##BOLD_BEGIN##";
    public final static String BoldEnd = "##BOLD_END##";
    public final static String ItalicBegin = "##ITALIC_BEGIN##";
    public final static String ItalicEnd = "##ITALIC_END##";
    public final static String FontBegin = "##FONT_BEGIN##";
    
    // TODO to be initialized by SettingsMgr
    public static XmppFont DefaultFont = new XmppFont();
    private XmppFont _mainFont;
    private StringBuilder _message = new StringBuilder();
    private ArrayList<XmppFont> _fonts = new ArrayList<XmppFont>();
    
    public XmppMsg() {
        _mainFont = DefaultFont;
    }
    
    public XmppMsg(String msg) {
        _mainFont = DefaultFont;
        _message.append(msg);
    }

    public XmppMsg(XmppFont font) {
        _mainFont = font;
    }

    public static String makeBold(String in) {
        return BoldBegin + in + BoldEnd;
    }

    public static String makeItalic(String in) {
        return ItalicBegin + in + ItalicEnd;
    }
    
    public void setFont(XmppFont font) {
        _message.append(FontBegin);
        _fonts.add(font);
    }

    public void insertFont(XmppFont font) {
        _fonts.add(font);
    }

    public void append(String msg) {
        _message.append(msg);
    }

    public void appendLine(String msg) {
        _message.append(msg);
        newLine();
    }
    
    public void appendBold(String msg) {
        _message.append(makeBold(msg));
    }
    
    public void appendBoldLine(String msg) {
        _message.append(makeBold(msg));
        newLine();
    }
    
    public void appendItalic(String msg) {
        _message.append(makeItalic(msg));
    }
    
    public void appendItalicLine(String msg) {
        _message.append(makeItalic(msg));
        newLine();
    }
    
    public void newLine() {
        _message.append(Tools.LineSep);
    }
    
    public String generateTxt() {
        return _message.toString()
                    .replaceAll(FontBegin, "")
                    .replaceAll(BoldBegin, "")
                    .replaceAll(BoldEnd, "")
                    .replaceAll(ItalicBegin, "")
                    .replaceAll(ItalicEnd, "");
    }

    public String generateFmtTxt() {
        return _message.toString()
                    .replaceAll(FontBegin, "")
                    .replaceAll(BoldBegin, " *")
                    .replaceAll(BoldEnd, "* ")
                    .replaceAll(ItalicBegin, " _")
                    .replaceAll(ItalicEnd, "_ ");
    }

    public String generateXhtml() {
        // TODO add parameters to configure default XHTML layout: font color style size...
        
        String msg = StringFmt.encodeHTML(_message.toString())
                    .replaceAll("\n", "<BR/>\n")
                    .replaceAll(BoldBegin, "<B>")
                    .replaceAll(BoldEnd, "</B>")
                    .replaceAll(ItalicBegin, "<I>")
                    .replaceAll(ItalicEnd, "</I>");
        
        ArrayList<XmppFont> fonts = new ArrayList<XmppFont>(_fonts);
        while (msg.contains(FontBegin)) {
            if (fonts.size() > 0) {
                XmppFont font = fonts.remove(0);
                msg = msg.replaceFirst(FontBegin, "</FONT>" + font.toString());
            } else {
                Log.e(Tools.LOG_TAG, "XmppMsg.generateXhtml: Font tags doesn't match");
                msg = msg.replace(FontBegin, "");
            }
        }

        return "<body>" + _mainFont + msg + "</FONT></body>";
    }
}
