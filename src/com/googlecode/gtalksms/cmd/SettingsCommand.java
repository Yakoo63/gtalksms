package com.googlecode.gtalksms.cmd;

import java.util.Map;

import com.googlecode.gtalksms.MainService;
import com.googlecode.gtalksms.SettingsManager;

public class SettingsCommand extends Command {
    
    public SettingsCommand(MainService mainService) {
        super(mainService, new String[] {"settings"}, Command.TYPE_SYSTEM);
    }

    @Override
    public void execute(UserCommand c) {
    	SettingsManager smgr = getSettingsManager();
    	Map<String, ?> settings = smgr.getAllSharedPreferences();
    	String key = c.get1();
    	
    	if(key.equals("")) {
    		StringBuffer buf = new StringBuffer();
    		for(String k : settings.keySet())
    			buf.append("\n").append(k);
    		c.respond("These are the possible settings:"+buf);
    	}
    	else if(settings.containsKey(key)) {
    		String newval = c.get2();
    		if(!"".equals(newval)) {
    			String before = ""+settings.get(key);
    			//TODO: smgr.setParameter(key, newval);
    			c.respond("Setting preferences is not currently supported; " +
    					"Tried setting: "+key+" to "+newval+" (previous value: "+before+")");
    		} else
    			c.respond(key+"="+settings.get(key));
    	} else {
    		c.respond("Unknown setting: "+key);
    	}
    }
    
    @Override
    public String[] help() {
        return null;
    }
}
