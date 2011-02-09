package com.googlecode.gtalksms.tools;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

import android.content.Context;

public class NewInstallUpdate {
	public static final int NONE = 0;
	public static final int FRESH_INSTALL = 1;
	public static final int UPDATE = 2;
	
	private final String version_filename;
	private final Context ctx;
	final String version;
	
	public NewInstallUpdate(Context c) {
		version = Tools.getVersionCode(c, getClass());
		this.ctx = c;
		version_filename = Tools.LOG_TAG + version;
	}
	
	/**
	 * Checks if the user has newly installed this app
	 * or if it is an update
	 * 
	 * @return 2 on update, 1 if this is an fresh install, otherwise 0
	 */
	public int isNewInstallUpdate() {
		if(isVersionInstalled()) {
			return NONE;
		} else if (hasOldVersionFile()) {
			deleteOldVersionFiles();
			createVersionFile();
			return UPDATE;
		} else {
			createVersionFile();
			return FRESH_INSTALL;
		}
	}
	
	/**
	 * Checks if there are old version files
	 * 
	 * @return true if one or more old version files where found, otherwise false
	 */
	private boolean hasOldVersionFile() {
		return false;
	}
	
	/**
	 * Checks if this version is already installed
	 * 
	 * @return true if this version is installed, otherwise false
	 */
	private boolean isVersionInstalled() {
		for(String s : ctx.fileList())
			if(s.equals(version_filename))
				return true;
		
		return false;
	}
	
	/**
	 * Creates the version file in the apps file storage
	 * 
	 * @return true on success, otherwise false
	 */
	private boolean createVersionFile() {
		try {
			FileOutputStream fOut = ctx.openFileOutput(version_filename, Context.MODE_PRIVATE);
			OutputStreamWriter osw = new OutputStreamWriter(fOut);
			osw.write("GTalkSMS Version File. Versioin: " + version);
			osw.close();
		} catch (IOException ioe) {
			return false;
		}
		return true;
	}
	
	private void deleteOldVersionFiles() {
		for(String s : ctx.fileList()) {
			if (!s.equals(version_filename)) {
				ctx.deleteFile(s);
			}
		}
	}
}
