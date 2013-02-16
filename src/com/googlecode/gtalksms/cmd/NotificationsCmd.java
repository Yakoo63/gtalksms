package com.googlecode.gtalksms.cmd;

import java.util.ArrayList;
import java.util.Arrays;

import android.text.TextUtils;

import com.googlecode.gtalksms.MainService;
import com.googlecode.gtalksms.R;
import com.googlecode.gtalksms.tools.Tools;

public class NotificationsCmd extends CommandHandlerBase {
    
    public NotificationsCmd(MainService mainService) {
        super(mainService, CommandHandlerBase.TYPE_SYSTEM, "Notification", new Cmd("notification", "notif"));
    }

    @Override
    protected void execute(String cmd, String args) {
        ArrayList<String> apps = new ArrayList<String>(Arrays.asList(TextUtils.split(sSettingsMgr.hiddenNotifications, "#sep#")));
        if (!args.equals("")) {
            int separatorPos = args.indexOf(":");

            // There is more than 1 argument
            if (-1 != separatorPos) {
                String subCmd = args.substring(0, separatorPos);
                String app = args.substring(separatorPos + 1);
            
                if (subCmd.equals("hide")) {
                    if (!apps.contains(app)) {
                        apps.add(app);
                        sSettingsMgr.saveSetting("hiddenNotifications", TextUtils.join("#sep#", apps));
                    }
                } else if (subCmd.equals("unhide")) {
                    if (apps.contains(app)) {
                        apps.remove(app);
                        sSettingsMgr.saveSetting("hiddenNotifications", TextUtils.join("#sep#", apps));
                    }
                } else if (subCmd.equals("ignoreDelay")) {
                    Integer intValue = Tools.parseInt(app);
                    if (intValue != null) {
                        sSettingsMgr.saveSetting("notificationIgnoreDelay", intValue);
                    }
                    send("Ignoring duplicated notifications under " + sSettingsMgr.notificationIgnoreDelay + "ms");
                    return;
                }
            } else if (args.equals("ignoreDelay")) {
                send("Ignoring duplicated notifications under " + sSettingsMgr.notificationIgnoreDelay + "ms");
                return;
            }
        }
        send("Black list: " + TextUtils.join(", ", apps));
    }
    
    @Override
    protected void initializeSubCommands() {
        Cmd notif = mCommandMap.get("notification");
        notif.setHelp(R.string.chat_help_notif_general, null);
        
        notif.AddSubCmd("hide", R.string.chat_help_notif_hide, "#appname#");
        notif.AddSubCmd("unhide",R.string.chat_help_notif_unhide, "#appname#");
        notif.AddSubCmd("ignoreDelay",R.string.chat_help_notif_ignore_delay, "#timeInMs#");
    }
}
