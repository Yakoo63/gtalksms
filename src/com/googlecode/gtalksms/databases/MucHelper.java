package com.googlecode.gtalksms.databases;

import android.content.Context;

/**
 * Middle-end Helper, if we ever want to integrate the Alias function also in
 * the App itself: This is the way to go.
 * At one place in the code, the constructor needs to be called, to make sure
 * that the Database is setup correctly.
 * 
 * @author Florian Schmaus fschmaus@gmail.com - on behalf of the GTalkSMS Team
 *
 */
public class MucHelper {	
    private static MucHelper mucHelper = null;
    
    /**
     * This constructor ensures that the database is setup correctly
     * @param ctx
     */
    private MucHelper(Context ctx) {
        new MUCDatabase(ctx);
    }
    
    public static MucHelper getMUCHelper(Context ctx) {
    	if (mucHelper == null) {
    		mucHelper = new MucHelper(ctx);
    	}
    	return mucHelper;
    }
    
    public boolean addMUC(String muc, String number) {
        if (muc.contains("'") || number.contains("'"))
            return false;
        
        addOrUpdate(muc, number);
        return true;
    }
    
    public boolean deleteMUC(String muc) {
        if(!muc.contains("'") && MUCDatabase.containsMUC(muc)) {
            return MUCDatabase.deleteMUC(muc);
        } else {
            return false;
        }
    }
    
    public boolean containsMUC(String muc) {
        if(!muc.contains("'")) {
    		return MUCDatabase.containsMUC(muc);
        } else {
        	return false;
        }
        
    }
     
	public String getValue(String muc) {
		if (!muc.contains("'")) {
			return MUCDatabase.getValue(muc);
		} else {
			return null;
		}
	}
    
    public String[][] getAllMUC() {
        String[][] res = MUCDatabase.getFullDatabase();
        if (res.length == 0)
            res = null;
        return res;
    }
    
    private void addOrUpdate(String muc, String number) {
       if(MUCDatabase.containsMUC(muc)) {
           MUCDatabase.updateMUC(muc, number);
       } else {
           MUCDatabase.addMUC(muc, number);
       }
    }
}
