package com.googlecode.gtalksms.cmd;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;

import com.googlecode.gtalksms.MainService;

public class SettingsCmd extends CommandHandlerBase {
    
    public SettingsCmd(MainService mainService) {
        super(mainService, CommandHandlerBase.TYPE_SYSTEM, new Cmd("settings"));
    }

    @Override
    public void execute(Command c) {
    	Map<String, ?> settings = sSettingsMgr.getAllSharedPreferences();
    	String key = c.get1();
    	
    	if(key.equals("")) {
    		StringBuffer buf = new StringBuffer();
    		ArrayList<String> keys = new ArrayList<String>(settings.keySet());
    		Collections.sort(keys);
    		for(String k : keys) {
    			buf.append("\n").append(k + "=" + settings.get(k));
    		}
    		c.respond("Settings are: " + buf);
    	}
    	else if(settings.containsKey(key)) {
    		String newval = c.get2();
    		if(!"".equals(newval)) {
    			String before = ""+settings.get(key);
    			//TODO: smgr.setParameter(key, newval);
    			c.respond("Setting preferences is not currently supported; " + "Tried setting: " 
    			        + key + " to " + newval + " (previous value: " + before + ")");
    		} else {
    			c.respond(key + "=" + settings.get(key));
    		}
    	} else {
    		c.respond("Unknown setting: " + key);
    	}
    }
    
    @Override
    public String[] help() {
        return null;
    }
}
