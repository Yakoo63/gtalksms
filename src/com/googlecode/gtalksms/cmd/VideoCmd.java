package com.googlecode.gtalksms.cmd;

import android.content.Intent;

import com.googlecode.gtalksms.MainService;
import com.googlecode.gtalksms.panels.Video;
import com.googlecode.gtalksms.tools.Tools;
import com.googlecode.gtalksms.xmpp.XmppMsg;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;

public class VideoCmd extends CommandHandlerBase {

    ArrayList<String> mParamList = new ArrayList<String>();

    public VideoCmd(MainService mainService) {
        super(mainService, CommandHandlerBase.TYPE_MEDIA, "Video", new Cmd("video"));

        mParamList.add("cameraMaxDurationInMs");
        mParamList.add("cameraMaxFileSizeInBytes");
        mParamList.add("cameraRotationInDegree");
        mParamList.add("cameraProfile");
        Collections.sort(mParamList);
    }

    @Override
    public void activate() {
        super.activate();
    }

    @Override
    public synchronized void deactivate() {
        super.deactivate();
    }

    @Override
    public void execute(Command c) {
        String subCommand = c.get1();
        if (isMatchingCmd("video", c.getCommand())) {
            if (subCommand.equals("")) {
                Intent intent = new Intent(Video.VIDEO_START, null, sContext, Video.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK + Intent.FLAG_ACTIVITY_SINGLE_TOP);
                sContext.startActivity(intent);
            } else if (subCommand.equals("stop")) {
                try {
                    Intent intent = new Intent(Video.VIDEO_STOP, null, sContext, Video.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK + Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    sContext.startActivity(intent);
                } catch (Exception e) {
                }
            } else if (subCommand.equals("params")) {
                String param = c.get2();

                if (param.equals("")) {
                    XmppMsg msg = new XmppMsg();
                    Map<String, ?> settings = sSettingsMgr.getAllSharedPreferences();
                    msg.appendLine("Settings are:");
                    for(String k : mParamList) {
                        msg.appendBold(k);
                        msg.appendLine(":" + settings.get(k));
                    }
                    send(msg);
                } else if (mParamList.contains(param)) {
                    String value = c.get3();

                    if (!value.equals("")) {
                        Integer intValue = Tools.parseInt(value);
                        Boolean boolValue = Tools.parseBool(value);
                        if (intValue != null) {
                            sSettingsMgr.saveSetting(param, intValue);
                        } else if (boolValue != null) {
                            sSettingsMgr.saveSetting(param, boolValue);
                        } else {
                            sSettingsMgr.saveSetting(param, value);
                        }
                    }
                    Map<String, ?> settings = sSettingsMgr.getAllSharedPreferences();
                    send(param + ":" + settings.get(param));
                } else {
                    send("Unknown parameter " + param);
                }
            }
        }
    }


    @Override
    protected void initializeSubCommands() {
        // TODO ad help

        // TODO add allowed values for CAMProfile
    }
}
