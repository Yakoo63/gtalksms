package com.googlecode.gtalksms.cmd;

import java.util.ArrayList;
import java.util.Arrays;

import android.text.TextUtils;

import com.googlecode.gtalksms.MainService;
import com.googlecode.gtalksms.R;
import com.googlecode.gtalksms.tools.ArrayStringSetting;
import com.googlecode.gtalksms.tools.Tools;

public class NotificationsCmd extends CommandHandlerBase {
    
    public NotificationsCmd(MainService mainService) {
        super(mainService, CommandHandlerBase.TYPE_SYSTEM, "Notification", new Cmd("notification", "notif"));
    }

    @Override
    protected void onCommandActivated() {
    }

    @Override
    protected void onCommandDeactivated() {
    }

    @Override
    protected void execute(Command cmd) {
        ArrayStringSetting hiddenApps = sSettingsMgr.getNotifHiddenApps();
        ArrayStringSetting hiddenMsgs = sSettingsMgr.getNotifHiddenMsgs();
        String arg1 = cmd.getArg1();
        String arg2 = cmd.getArg2();
        String arg3 = cmd.getAllArg(3);

        if (arg1.equals("hide")) {
            if (arg2.equals("app")) {
                hiddenApps.add(arg3);
            } else if (arg2.equals("msg")) {
                hiddenMsgs.add(arg3);
            } else if (!arg2.equals("")) {
                send("Please specify 'msg' or 'app'");
            }
        } else if (arg1.equals("unhide") || arg1.equals("show")) {
            if (arg2.equals("app")) {
                hiddenApps.remove(arg3);
            } else if (arg2.equals("msg")) {
                hiddenMsgs.remove(arg3);
            } else if (!arg2.equals("")) {
                send("Please specify 'msg' or 'app'");
            }
        } else if (arg1.equals("ignoreDelay")) {
            Integer intValue = Tools.parseInt(arg2);
            if (intValue != null) {
                sSettingsMgr.saveSetting("notificationIgnoreDelay", intValue);
            }
            send("Ignoring duplicated notifications under " + sSettingsMgr.notificationIgnoreDelay + "ms");
            return;
        }

        send("Applications blacklist: " + TextUtils.join(", ", hiddenApps.getAll()));
        send("Messages blacklist: " + TextUtils.join(", ", hiddenMsgs.getAll()));
    }
    
    @Override
    protected void initializeSubCommands() {
        Cmd notif = mCommandMap.get("notification");
        notif.setHelp(R.string.chat_help_notif_general, null);

        notif.AddSubCmd("hide", R.string.chat_help_notif_hide, "app:#appname#");
        notif.AddSubCmd("show",R.string.chat_help_notif_unhide, "app:#appname#");
        notif.AddSubCmd("hide", R.string.chat_help_notif_hide_msg, "msg:#text#");
        notif.AddSubCmd("show",R.string.chat_help_notif_unhide_msg, "msg:#text#");
        notif.AddSubCmd("ignoreDelay",R.string.chat_help_notif_ignore_delay, "#timeInMs#");
    }
}
