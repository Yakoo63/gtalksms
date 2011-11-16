package com.googlecode.gtalksms.xmpp;

import java.util.ArrayList;

import org.jivesoftware.smackx.XHTMLText;

import android.os.Parcel;
import android.os.Parcelable;

import com.googlecode.gtalksms.tools.GoogleAnalyticsHelper;
import com.googlecode.gtalksms.tools.Tools;

public class XmppMsg implements Parcelable {
    public final static String BoldBegin = "##BOLD_BEGIN##";
    public final static String BoldEnd = "##BOLD_END##";
    public final static String ItalicBegin = "##ITALIC_BEGIN##";
    public final static String ItalicEnd = "##ITALIC_END##";
    public final static String FontBegin = "##FONT_BEGIN##";
    
    // TODO to be initialized by SettingsMgr
    private static final XmppFont DEFAULT_FONT = new XmppFont();
    private XmppFont mMainFont;
    private StringBuilder mMessage = new StringBuilder();
    private ArrayList<XmppFont> mFonts = new ArrayList<XmppFont>();
    
    public XmppMsg() {
        mMainFont = DEFAULT_FONT;
    }
    
    public XmppMsg(String msg) {
        mMainFont = DEFAULT_FONT;
        mMessage.append(msg);
    }

    public XmppMsg(XmppFont font) {
        mMainFont = font;
    }

    public static String makeBold(String in) {
        return BoldBegin + in + BoldEnd;
    }

    public static String makeItalic(String in) {
        return ItalicBegin + in + ItalicEnd;
    }
    
    public void setFont(XmppFont font) {
        mMessage.append(FontBegin);
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
    
    public XmppMsg append(XmppMsg input) {
        mMessage.append(input.mMessage);
        mFonts.addAll(input.mFonts);
        return this;
    }
    
    public String generateTxt() {
        String message = removeLastNewline(mMessage.toString());
        return message
                    .replaceAll(FontBegin, "")
                    .replaceAll(BoldBegin, "")
                    .replaceAll(BoldEnd, "")
                    .replaceAll(ItalicBegin, "")
                    .replaceAll(ItalicEnd, "");
    }

    public String generateFmtTxt() {
        String message = removeLastNewline(mMessage.toString());
        return message
                    .replaceAll(FontBegin, "")
                    .replaceAll(BoldBegin, " *")
                    .replaceAll(BoldEnd, "* ")
                    .replaceAll(ItalicBegin, " _")
                    .replaceAll(ItalicEnd, "_ ");
    }
    
    public XHTMLText generateXHTMLText() {
        int pos;
        String message = mMessage.toString();
        message = removeLastNewline(message);
        StringBuilder m = new StringBuilder(message); 
        ArrayList<XmppFont> fonts = new ArrayList<XmppFont>(mFonts);
        
        XHTMLText x = new XHTMLText(null, null);
        x.appendOpenParagraphTag(mMainFont.toString()); // open a paragraph with default font - which is may be null - clients will fall back to their default font
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
        int boldbeg = msg.indexOf(BoldBegin);
        int boldend = msg.indexOf(BoldEnd);
        int italbeg = msg.indexOf(ItalicBegin);
        int italend = msg.indexOf(ItalicEnd);
        int fontbeg = msg.indexOf(FontBegin);  //there is no font end tag, so just treat every fontbegin as the end of the previous font
        
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
            x.appendBrTag();                                // smack appends "<br>" where the XEP-71 postulates "<br/>" 
            msg.delete(0, "\n".length());                   // we fix this in XmppManager
        } else if (msg.indexOf(BoldBegin) == 0) {       // bold
            x.appendOpenSpanTag("font-weight:bold");  
//            x.appendOpenStrongTag();
            msg.delete(0, BoldBegin.length());
        } else if (msg.indexOf(BoldEnd) == 0) {
            x.appendCloseSpanTag();
//            x.appendCloseStrongTag();
            msg.delete(0, BoldEnd.length());
        } else if (msg.indexOf(ItalicBegin) == 0) {     // italic
//            x.appendOpenSpanTag("font-style:italic");
            x.appendOpenEmTag();
            msg.delete(0, ItalicBegin.length());
        } else if (msg.indexOf(ItalicEnd) == 0) {
//            x.appendCloseSpanTag();
            x.appendCloseEmTag();
            msg.delete(0, ItalicEnd.length());
        } else if (msg.indexOf(FontBegin) == 0) {       // font
            x.appendCloseSpanTag();
            if (fonts.size() > 0) {
                XmppFont font = fonts.remove(0);
                x.appendOpenSpanTag(font.toString());
            } else {
                GoogleAnalyticsHelper.trackAndLogError("XmppMsg.generateXhtml: Font tags doesn't match");
                x.appendOpenSpanTag("font:null");   
            }
            msg.delete(0, FontBegin.length());
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
