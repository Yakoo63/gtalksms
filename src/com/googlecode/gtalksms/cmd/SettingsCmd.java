package com.googlecode.gtalksms.cmd;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;

import com.googlecode.gtalksms.MainService;
import com.googlecode.gtalksms.tools.Tools;

public class SettingsCmd extends CommandHandlerBase {
    
    public SettingsCmd(MainService mainService) {
        super(mainService, CommandHandlerBase.TYPE_INTERNAL, "Settings", new Cmd("settings", "set"));
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
                buf.append("\n").append(k + ":" + settings.get(k));
            }
            c.respond("Settings are: " + buf);
        }
        else if(settings.containsKey(key)) {
            String newval = c.get2();
            if(!"".equals(newval)) {
                Integer intValue = Tools.parseInt(newval);
                Boolean boolValue = Tools.parseBool(newval);
                if (intValue != null) {
                    sSettingsMgr.saveSetting(key, intValue);
                } else if (boolValue != null) {
                    sSettingsMgr.saveSetting(key, boolValue);
                } else {
                    sSettingsMgr.saveSetting(key, newval);
                }
            } else {
                c.respond(key + ":" + settings.get(key));
            }
        } else {
            c.respond("Unknown setting: " + key);
        }
    }
    
    @Override
    protected void initializeSubCommands() {
    }
}
