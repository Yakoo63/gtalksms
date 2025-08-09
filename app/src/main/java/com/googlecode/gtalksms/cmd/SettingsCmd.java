package com.googlecode.gtalksms.cmd;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;

import com.googlecode.gtalksms.MainService;
import com.googlecode.gtalksms.tools.Tools;
import com.googlecode.gtalksms.xmpp.XmppMsg;

public class SettingsCmd extends CommandHandlerBase {
    
    public SettingsCmd(MainService mainService) {
        super(mainService, CommandHandlerBase.TYPE_INTERNAL, "Settings", new Cmd("settings", "set"));
    }

    @Override
    protected void onCommandActivated() {
    }

    @Override
    protected void onCommandDeactivated() {
    }

    @Override
    public void execute(Command c) {
        Map<String, ?> settings = sSettingsMgr.getAllSharedPreferences();
        ArrayList<String> protectedSettings = sSettingsMgr.getProtectedSettings();
        String key = c.getArg1();
        
        if(key.equals("")) {
            XmppMsg msg = new XmppMsg();
            ArrayList<String> keys = new ArrayList<String>(settings.keySet());
            Collections.sort(keys);
            msg.appendLine("Settings are:");
            for(String k : keys) {
                if (!protectedSettings.contains(k)) {
                    msg.appendBold(k);
                    msg.appendLine(":" + settings.get(k));
                } else {
                    msg.appendBold("[" + k + "]");
                    msg.appendLine(":" + settings.get(k));
                }
            }
            send(msg);
        }
        else if(settings.containsKey(key)) {
            String newVal = c.getAllArg2();
            if(!"".equals(newVal)) {
                if (!protectedSettings.contains(key)) {
                    Integer intValue = Tools.parseInt(newVal);
                    Boolean boolValue = Tools.parseBool(newVal);
                    if (intValue != null) {
                        sSettingsMgr.saveSetting(key, intValue);
                    } else if (boolValue != null) {
                        sSettingsMgr.saveSetting(key, boolValue);
                    } else {
                        sSettingsMgr.saveSetting(key, newVal);
                    }
                    send(key + ":" + settings.get(key));
                } else {
                    send(key + " setting is protected.");
                }
            } else {
                send(key + ":" + settings.get(key));
            }
        } else {
            send("Unknown setting: " + key);
        }
    }
    
    @Override
    protected void initializeSubCommands() {
    }
}
