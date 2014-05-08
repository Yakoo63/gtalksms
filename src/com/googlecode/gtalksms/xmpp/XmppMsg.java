package com.googlecode.gtalksms.xmpp;

import java.util.ArrayList;


import android.os.Parcel;
import android.os.Parcelable;

import com.googlecode.gtalksms.tools.Log;
import com.googlecode.gtalksms.tools.Tools;

import org.jivesoftware.smackx.xhtmlim.XHTMLText;

public class XmppMsg implements Parcelable {
    private final static String BOLD_BEGIN = "##BOLD_BEGIN##";
    private final static String BOLD_END = "##BOLD_END##";
    private final static String ITALIC_BEGIN = "##ITALIC_BEGIN##";
    private final static String ITALIC_END = "##ITALIC_END##";
    private final static String FONT_BEGIN = "##FONT_BEGIN##";
    
    private static final XmppFont DEFAULT_FONT = new XmppFont();
    private XmppFont mMainFont;
    private final StringBuilder mMessage = new StringBuilder();
    private final ArrayList<XmppFont> mFonts = new ArrayList<XmppFont>();
    
    public XmppMsg() {
        mMainFont = DEFAULT_FONT;
    }
    
    public XmppMsg(String msg) {
        mMainFont = DEFAULT_FONT;
        mMessage.append(msg);
    }

    public XmppMsg(String msg, boolean newLine) {
        mMainFont = DEFAULT_FONT;
        mMessage.append(msg);
        if (newLine) {
        	newLine();
        }
    }

    public XmppMsg(XmppFont font) {
        mMainFont = font;
    }
    
    public void clear() {
        mMainFont = DEFAULT_FONT;
        mMessage.setLength(0);
        mFonts.clear();
    }

    public static String makeBold(String in) {
        return BOLD_BEGIN + in + BOLD_END;
    }

    private static String makeItalic(String in) {
        return ITALIC_BEGIN + in + ITALIC_END;
    }
    
    public void setFont(XmppFont font) {
        mMessage.append(FONT_BEGIN);
        mFonts.add(font);
    }

    public void append(String msg) {
        mMessage.append(msg);
    }
    
    public void append(int value) {
        append(Integer.toString(value));
    }

    public void appendLine(String msg) {
        mMessage.append(msg);
        newLine();
    }
    
    public void appendLine(int value) {
        appendLine(Integer.toString(value));
    }

    public void insertLineBegin(String msg) {
        mMessage.insert(0, msg + Tools.LineSep);
    }

    public void appendBold(String msg) {
        mMessage.append(makeBold(msg));
    }

    public void appendBoldItalic(String msg) {
        mMessage.append(makeBold(makeItalic(msg)));
    }
    
    public void appendBoldLine(String msg) {
        mMessage.append(makeBold(msg));
        newLine();
    }
    
    public void appendItalic(String msg) {
        mMessage.append(makeItalic(msg));
    }
    
    public void appendItalicLine(String msg) {
        mMessage.append(makeItalic(msg));
        newLine();
    }
    
    public void newLine() {
        mMessage.append(Tools.LineSep);
    }
    
    /**
     * Adds the Strings in the given Arrary to the XmppMsg
     * One String per line
     * 
     * @param s
     */
    public final void addStringArray(String[] s) {
        for(String line : s) {
            this.appendLine(line);
        }
    }
    
    public XmppMsg append(XmppMsg input) {
        mMessage.append(input.mMessage);
        mFonts.addAll(input.mFonts);
        return this;
    }
    
    public String generateTxt() {
        String message = removeLastNewline(mMessage.toString());
        return message
                    .replaceAll(FONT_BEGIN, "")
                    .replaceAll(BOLD_BEGIN, "")
                    .replaceAll(BOLD_END, "")
                    .replaceAll(ITALIC_BEGIN, "")
                    .replaceAll(ITALIC_END, "");
    }

    public String generateFmtTxt() {
        String message = removeLastNewline(mMessage.toString());
        return message
                    .replaceAll(FONT_BEGIN, "")
                    .replaceAll(BOLD_BEGIN, " *")
                    .replaceAll(BOLD_END, "* ")
                    .replaceAll(ITALIC_BEGIN, " _")
                    .replaceAll(ITALIC_END, "_ ");
    }
    
    public XHTMLText generateXHTMLText() {
        int pos;
        String message = mMessage.toString();
        message = removeLastNewline(message);
        StringBuilder m = new StringBuilder(message); 
        ArrayList<XmppFont> fonts = new ArrayList<XmppFont>(mFonts);
        
        XHTMLText x = new XHTMLText(null, null);
        x.appendOpenParagraphTag(mMainFont.toString()); // open a paragraph with default font. When null, clients will fall back to their default font
        x.appendOpenSpanTag("");  // needed because we close span on fontbegin
        while((pos = getTagPos(m)) != -1) {
            procesTagAt(pos, x, m, fonts);
        }
        if(m.length() != 0) 
            x.append(m.toString());
        x.appendCloseSpanTag();
        x.appendCloseParagraphTag();
        return x;
        
    }
    
    public String toString() {
        return generateTxt();
    }
    
    public String toShortString() {
        String message = this.toString();
        return Tools.shortenMessage(message);
    }
    
    public XHTMLText toXHTMLText() {
        return generateXHTMLText();
    }
    
    /**
     * Returns the smallest indexposition of a internal string format tag
     * @param msg
     * @return smallest indexposition, -1 if no more tags were found
     */
    private static int getTagPos(StringBuilder msg) {
        int newline = msg.indexOf("\n");
        int boldbeg = msg.indexOf(BOLD_BEGIN);
        int boldend = msg.indexOf(BOLD_END);
        int italbeg = msg.indexOf(ITALIC_BEGIN);
        int italend = msg.indexOf(ITALIC_END);
        int fontbeg = msg.indexOf(FONT_BEGIN);  //there is no font end tag, so just treat every fontbegin as the end of the previous font
        
        //if all int's are -1 we found no tag
        if(-1 == newline && -1 == boldbeg && -1 == boldend && -1 == italbeg && -1 == italend && -1 == fontbeg) {
            return -1;
        } else {
            return Tools.getMinNonNeg(newline, boldbeg, boldend, italbeg, italend, fontbeg);
        }
    }
    
    private static void procesTagAt(int i, XHTMLText x, StringBuilder msg, ArrayList<XmppFont> fonts) {
        String s = msg.substring(0, i);
        msg.delete(0, i);
        x.append(s);
        if (msg.indexOf("\n") == 0) {                   // newline
            x.appendBrTag();
            msg.delete(0, "\n".length());
        } else if (msg.indexOf(BOLD_BEGIN) == 0) {       // bold
            x.appendOpenSpanTag("font-weight:bold");  
//            x.appendOpenStrongTag();
            msg.delete(0, BOLD_BEGIN.length());
        } else if (msg.indexOf(BOLD_END) == 0) {
            x.appendCloseSpanTag();
//            x.appendCloseStrongTag();
            msg.delete(0, BOLD_END.length());
        } else if (msg.indexOf(ITALIC_BEGIN) == 0) {     // italic
//            x.appendOpenSpanTag("font-style:italic");
            x.appendOpenEmTag();
            msg.delete(0, ITALIC_BEGIN.length());
        } else if (msg.indexOf(ITALIC_END) == 0) {
//            x.appendCloseSpanTag();
            x.appendCloseEmTag();
            msg.delete(0, ITALIC_END.length());
        } else if (msg.indexOf(FONT_BEGIN) == 0) {       // font
            x.appendCloseSpanTag();
            if (fonts.size() > 0) {
                XmppFont font = fonts.remove(0);
                x.appendOpenSpanTag(font.toString());
            } else {
                Log.e("XmppMsg.generateXhtml: Font tags doesn't match");
                x.appendOpenSpanTag("font:null");   
            }
            msg.delete(0, FONT_BEGIN.length());
        }
    }
    
    /**
     * If the last char in a given string is newline,
     * return a string without the newline as last char
     * 
     * @param str
     * @return
     */
    private static String removeLastNewline(String str) {
        int strlen = str.length();
        if (strlen == 0) {
            return str;
        }
        
        int lastNewline = str.lastIndexOf("\n");
        if (strlen == lastNewline + 1) {
            return str.substring(0, strlen-1);            
        } else {
            return str;
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(toString());
    }
    
    public static final Parcelable.Creator<XmppMsg> CREATOR = new Parcelable.Creator<XmppMsg>() {
        public XmppMsg createFromParcel(Parcel in) {
            return new XmppMsg(in.readString());
        }

        public XmppMsg[] newArray(int size) {
            return new XmppMsg[size];
        }
    };
}
