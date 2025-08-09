package com.googlecode.gtalksms.tools;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import android.app.backup.BackupAgent;
import android.app.backup.BackupDataInput;
import android.app.backup.BackupDataOutput;
import android.content.SharedPreferences;
import android.os.ParcelFileDescriptor;

import com.googlecode.gtalksms.SettingsManager;

public class MyBackupAgent extends BackupAgent {
    public static final int KEYTYPE_UNKNOWN = 0;
    private static final int KEYTYPE_STRING = 1;
    private static final int KEYTYPE_INT = 2;
    private static final int KEYTYPE_BOOLEAN = 3;
    
    private SettingsManager settingsManager;
    
    @Override
    public void onBackup(ParcelFileDescriptor oldState, BackupDataOutput data, ParcelFileDescriptor newState) throws IOException {
        Log.i("Starting backup");
        settingsManager = SettingsManager.getSettingsManager(this);

        String sharedPrefsPath = Tools.getSharedPrefDir(this) + "/" + Tools.APP_NAME + ".xml";
        File mDataFile = new File(sharedPrefsPath);
        DataInputStream in = null;
        try {
            // step 1 - check if update necessary
            in = new DataInputStream(new FileInputStream(oldState.getFileDescriptor()));
            // Get the last modified timestamp from the state file and data file
            long stateModified = in.readLong();
            long fileModified = mDataFile.lastModified();

            if (stateModified == fileModified) {
                return;
            }
        } catch (IOException e) {
            // Unable to read state file... be safe and do a backup
        } catch (Exception e) {
            Log.w("Backup error", e);
        } finally {
            if (in != null) {
                in.close();
            }
        }
        
        //step 2
        writeData(data);
        
        //step 3
        FileOutputStream outputStream = new FileOutputStream(newState.getFileDescriptor());
        DataOutputStream out = new DataOutputStream(outputStream);
        long modified = mDataFile.lastModified();
        out.writeLong(modified);
        out.close();
        Log.i("Backup ended");
    }

    @Override
    public void onRestore(BackupDataInput data, int appVersionCode, ParcelFileDescriptor newState) throws IOException {        
        Log.i("Starting to restore saved preferences");
        settingsManager = SettingsManager.getSettingsManager(this);

        Class<?> cls;
        try {
            cls = Class.forName("com.googlecode.gtalksms.SettingsManager");
        } catch (ClassNotFoundException e) {
            return;
        }
        SharedPreferences.Editor prefEditor = settingsManager.getEditor();
        Map<String, Object> stringMap = convertToMap(getAllTypeFields(cls, String.class));
        Map<String, Object> intMap = convertToMap(getAllTypeFields(cls, int.class));
        Map<String, Object> booleanMap = convertToMap(getAllTypeFields(cls, boolean.class));
        Set<String> stringKeys = stringMap.keySet();
        Set<String> intKeys = intMap.keySet();
        Set<String> booleanKeys = booleanMap.keySet();
        
        while (data.readNextHeader()) {
            String key = data.getKey();
            int dataSize = data.getDataSize();
            int keyType;
            if (stringKeys.contains(key)) {
                keyType = MyBackupAgent.KEYTYPE_STRING;
            } else if (intKeys.contains(key)) {
                keyType = MyBackupAgent.KEYTYPE_INT;
            } else if (booleanKeys.contains(key)) {
                keyType = MyBackupAgent.KEYTYPE_BOOLEAN;
            } else {
                // unknown or unsupported key
                data.skipEntityData();
                continue;
            }
            try {
                byte[] dataBuf = new byte[dataSize];
                data.readEntityData(dataBuf, 0, dataSize);
                ByteArrayInputStream baStream = new ByteArrayInputStream(dataBuf);
                DataInputStream in = new DataInputStream(baStream);
                switch (keyType) {
                case MyBackupAgent.KEYTYPE_STRING:
                    prefEditor.putString(key, in.readUTF());
                    break;
                case MyBackupAgent.KEYTYPE_INT:
                    prefEditor.putInt(key, in.readInt());
                    break;
                case MyBackupAgent.KEYTYPE_BOOLEAN:
                    prefEditor.putBoolean(key, in.readBoolean());
                    break;
                default:
                    break;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        prefEditor.commit();
        Log.i("Settings restored");
    }
    
    private void writeData(BackupDataOutput data) {
        Log.i("New data found - starting backup");
        Class<?> cls;
        try {
            cls = Class.forName("com.googlecode.gtalksms.SettingsManager");
        } catch (ClassNotFoundException e) {
            return;
        }
        Set<String> keys;
        Iterator<String> i;
        
        // First Strings without the password
        Map<String, Object> stringMap = convertToMap(getAllTypeFields(cls, String.class));
        stringMap.remove("password");
        keys = stringMap.keySet();
        i = keys.iterator();
        while(i.hasNext()) {
            String key = i.next();
            // skip the key if its not saved in the xml file
            // e.g. the settingsManager calculates the value of some keys from 
            // other keys
            if(!settingsManager.SharedPreferencesContains(key))
                continue;
            
            String value = (String) stringMap.get(key);
            ByteArrayOutputStream bufStream = new ByteArrayOutputStream();
            DataOutputStream outWriter = new DataOutputStream(bufStream);
            try {
                outWriter.writeUTF(value);
                byte[] buffer = bufStream.toByteArray();
                int len = buffer.length;
                data.writeEntityHeader(key, len);
                data.writeEntityData(buffer, len);
            } catch (IOException e) { /* Ignore */ }
        }
        // Then int
        Map<String, Object> intMap = convertToMap(getAllTypeFields(cls, int.class));
        keys = intMap.keySet();
        i = keys.iterator();
        while(i.hasNext()) {
            String key = i.next();
            int value = (Integer) intMap.get(key);
            ByteArrayOutputStream bufStream = new ByteArrayOutputStream();
            DataOutputStream outWriter = new DataOutputStream(bufStream);
            try {
                outWriter.writeInt(value);
                byte[] buffer = bufStream.toByteArray();
                int len = buffer.length;
                data.writeEntityHeader(key, len);
                data.writeEntityData(buffer, len);
            } catch (IOException e) { /* Ignore */ }
        }
        
        // Then boolean
        Map<String, Object> booleanMap = convertToMap(getAllTypeFields(cls, boolean.class));
        keys = booleanMap.keySet();
        i = keys.iterator();
        while(i.hasNext()) {
            String key = i.next();
            boolean value = (Boolean) booleanMap.get(key);
            ByteArrayOutputStream bufStream = new ByteArrayOutputStream();
            DataOutputStream outWriter = new DataOutputStream(bufStream);
            try {
                outWriter.writeBoolean(value);
                byte[] buffer = bufStream.toByteArray();
                int len = buffer.length;
                data.writeEntityHeader(key, len);
                data.writeEntityData(buffer, len);
            } catch (IOException e) { /* Ignore */ }
        }
    }
    
    private ArrayList<Field> getAllTypeFields(Class<?> fromCls, Class<?> typeCls) {
        ArrayList<Field> typeFields = new ArrayList<Field>();
        try {
            Field fieldList[] = fromCls.getFields();
            for (Field fld : fieldList) {
                if (fld.getType().getName().equals(typeCls.getName()))
                    typeFields.add(fld);
            }
        }
        catch (Throwable e) { /* Ignore */ }
        return typeFields;
    }
    
    private Map<String, Object> convertToMap(ArrayList<Field> fieldArray) {
        HashMap<String, Object> map = new HashMap<String, Object>();
        for (Field f : fieldArray) {
            try {
                map.put(f.getName(), f.get(settingsManager));
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }        
        return map;
    }
}
