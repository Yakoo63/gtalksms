package com.googlecode.gtalksms.cmd.smsCmd;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.MessageFormat;
import java.util.ArrayList;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;

import com.googlecode.gtalksms.Log;
import com.googlecode.gtalksms.R;
import com.googlecode.gtalksms.SettingsManager;
import com.googlecode.gtalksms.data.contacts.ContactsManager;
import com.googlecode.gtalksms.tools.StringFmt;
import com.googlecode.gtalksms.tools.Tools;

/**
 * Helper to manage MMS from the Android API
 * 
 * @source: http://stackoverflow.com/questions/3012287/how-to-read-mms-data-in-android
 * @author Florent L.
 */
public class MmsManager {

    private Context _context;
    private SettingsManager _settings;

    private static final int MMS_TYPE_SENT = 128;
    private static final int MMS_TYPE_RECEIVED = 132;
    private static final int MMS_TYPE_ADDR_RECIPIENT = 151;
    private static final int MMS_TYPE_ADDR_SENDER = 137;
    
    private static final Uri MMS_CONTENT_URI = Uri.parse("content://mms");
    private static final Uri MMS_INBOX_CONTENT_URI = Uri.withAppendedPath(MMS_CONTENT_URI, "inbox");
    private static final Uri MMS_SENTBOX_CONTENT_URI = Uri.withAppendedPath(MMS_CONTENT_URI, "sent");
    private static final Uri MMS_PART_CONTENT_URI = Uri.withAppendedPath(MMS_CONTENT_URI, "part");
    
    private static final String SORT_ORDER = "date DESC";

    public MmsManager(Context baseContext) {
        _settings = SettingsManager.getSettingsManager(baseContext);
        _context = baseContext;
        Log.initialize(_settings);
    }
    
    /**
     * Retrieve the X last MMS
     * TODO: To be tested with older Android's SDK
     * 
     * @param nbMMS Number of MMS to retrieve
     * @return The list of MMS
     */
    public ArrayList<Mms> getLastReceivedMmsDetails(int nbMMS) {
        return getLastMmsDetails(MMS_INBOX_CONTENT_URI, nbMMS);
    }
    
    public ArrayList<Mms> getLastSentMmsDetails(int nbMMS) {
        return getLastMmsDetails(MMS_SENTBOX_CONTENT_URI, nbMMS);
    }
    
    public ArrayList<Mms> getLastMmsDetails(Uri uri, int nbMMS) {
        ArrayList<Mms> allMms = new ArrayList<Mms>();
        
        // Looking for the last unread MMS
        Cursor c = _context.getContentResolver().query(uri, null, "m_type in (" + MMS_TYPE_RECEIVED + "," + MMS_TYPE_SENT + ")", null, SORT_ORDER);
        if (c != null) {
            try {
                if (c.getCount() > 0) {
                    c.moveToFirst();
                    int count = 0;
                    do {
                        // Log.dump("MMS: inbox ", c);
                        
                        // TODO: check with other languages
                        String subject = Tools.getString(c, "sub");
                        int id = Tools.getInt(c, "_id");
                        try {
                            subject = new String(subject.getBytes("ISO-8859-1"));
                        } catch (Exception e) {}
                        
                        Mms mms = new Mms(subject, Tools.getDateSeconds(c, "date"), Tools.getString(c, "m_id"), getAddress(id, MMS_TYPE_ADDR_SENDER), getAddress(id, MMS_TYPE_ADDR_RECIPIENT));
                        fillMms(id, mms);
                        allMms.add(mms);
                    } while (c.moveToNext() && ++count < nbMMS);
                }
            } finally {
                c.close();
            }
        }
        
        return allMms;
    }

    private void fillMms(int id, Mms mms) {
        // Read the content of the MMS
        Cursor cPart = _context.getContentResolver().query(MMS_PART_CONTENT_URI, null, "mid = " + id, null, null);
        if (cPart.moveToFirst()) {
            do {
                // Dump all fields into the logs
                // Log.dump("MMS: part ", cPart);
                
                String partId = Tools.getString(cPart, "_id");
                String type = Tools.getString(cPart, "ct");
                if ("text/plain".equals(type)) {
                    String data = Tools.getString(cPart, "_data");
                    if (data != null) {
                        mms.appendMessage(getMmsText(partId));
                    } else {
                        mms.appendMessage(Tools.getString(cPart, "text"));
                    }
                } else if ("image/jpeg".equals(type) || "image/bmp".equals(type) || "image/gif".equals(type) || "image/jpg".equals(type) || "image/png".equals(type)) {
                    Bitmap bitmap = getMmsImage(partId);
                    mms.appendMessage("Image Size: " + bitmap.getWidth() + "x" + bitmap.getHeight() + "\n");
                    mms.setBitmap(bitmap);
                }
            } while (cPart.moveToNext());
        }
        if (cPart != null) {
            cPart.close();
        }
    }

    private String getMmsText(String id) {
        InputStream is = null;
        StringBuilder sb = new StringBuilder();
        try {
            is = _context.getContentResolver().openInputStream(Uri.withAppendedPath(MMS_PART_CONTENT_URI, id));
            if (is != null) {
                InputStreamReader isr = new InputStreamReader(is, "UTF-8");
                BufferedReader reader = new BufferedReader(isr);
                String temp = reader.readLine();
                while (temp != null) {
                    sb.append(temp);
                    temp = reader.readLine();
                }
            }
        } catch (IOException e) {
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {}
            }
        }
        return sb.toString();
    }

    private Bitmap getMmsImage( String _id) {
        Uri partURI = Uri.parse("content://mms/part/" + _id);
        InputStream is = null;
        Bitmap bitmap = null;
        try {
            is = _context.getContentResolver().openInputStream(partURI);
            bitmap = BitmapFactory.decodeStream(is);
        } catch (IOException e) {
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {}
            }
        }
        return bitmap;
    }
    
    
    private String getAddress(int id, int mmsType) {
        ArrayList<String> names = new ArrayList<String>();
        String selectionAdd = new String("msg_id = " + id + " and type = " + mmsType);
        String uriStr = MessageFormat.format("content://mms/{0}/addr", id);
        Uri uriAddress = Uri.parse(uriStr);
        Cursor cAdd = _context.getContentResolver().query(uriAddress, null, selectionAdd, null, null);

        if (cAdd.moveToFirst()) {
            do {
                // Log.dump("MMS: addr ", cAdd);
                String address = Tools.getString(cAdd, "address");
                if (address.equals("insert-address-token")) {
                    names.add(_context.getString(R.string.chat_me));
                } else {
                    names.add(ContactsManager.getContactName(_context, address));
                }
            } while (cAdd.moveToNext());
        }
        if (cAdd != null) {
            cAdd.close();
        }
        return StringFmt.join(names, ", ");
    }
}