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
    protected void execute(Command cmd) {
        ArrayList<String> apps = new ArrayList<String>(Arrays.asList(TextUtils.split(sSettingsMgr.hiddenNotifications, "#sep#")));
        String arg1 = cmd.getArg1();
        String arg2 = cmd.getArg2();

        if (arg1.equals("hide")) {
            if (!apps.contains(arg2)) {
                apps.add(arg2);
                sSettingsMgr.saveSetting("hiddenNotifications", TextUtils.join("#sep#", apps));
            }
        } else if (arg1.equals("unhide") || arg1.equals("show")) {
            if (apps.contains(arg2)) {
                apps.remove(arg2);
                sSettingsMgr.saveSetting("hiddenNotifications", TextUtils.join("#sep#", apps));
            }
        } else if (arg1.equals("ignoreDelay")) {
            Integer intValue = Tools.parseInt(arg2);
            if (intValue != null) {
                sSettingsMgr.saveSetting("notificationIgnoreDelay", intValue);
            }
            send("Ignoring duplicated notifications under " + sSettingsMgr.notificationIgnoreDelay + "ms");
            return;
        }

        send("Black list: " + TextUtils.join(", ", apps));
    }
    
    @Override
    protected void initializeSubCommands() {
        Cmd notif = mCommandMap.get("notification");
        notif.setHelp(R.string.chat_help_notif_general, null);
        
        notif.AddSubCmd("hide", R.string.chat_help_notif_hide, "#appname#");
        notif.AddSubCmd("unhide",R.string.chat_help_notif_unhide, "#appname#", "show");
        notif.AddSubCmd("ignoreDelay",R.string.chat_help_notif_ignore_delay, "#timeInMs#");
    }
}
