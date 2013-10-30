package com.googlecode.gtalksms.cmd;

import android.content.Intent;

import com.googlecode.gtalksms.MainService;
import com.googlecode.gtalksms.R;
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

        mParamList.add("cameraMaxDurationInSec");
        mParamList.add("cameraMaxFileSizeInMegaBytes");
        mParamList.add("cameraRotationInDegree");
        mParamList.add("cameraProfile");
        Collections.sort(mParamList);
    }

    @Override
    protected void onCommandActivated() {
    }

    @Override
    protected void onCommandDeactivated() {
    }

    @Override
    public void execute(Command c) {
        String subCommand = c.getArg1();
        if (isMatchingCmd(c, "video")) {
            if (subCommand.equals("") || subCommand.equals("start")) {
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
                String param = c.getArg2();

                if (param.equals("")) {
                    XmppMsg msg = new XmppMsg();
                    Map<String, ?> settings = sSettingsMgr.getAllSharedPreferences();
                    msg.appendLine(sContext.getString(R.string.chat_video_settings));
                    for(String k : mParamList) {
                        msg.appendBold(k);
                        msg.appendLine(":" + settings.get(k));
                    }
                    send(msg);
                } else if (mParamList.contains(param)) {
                    String value = c.getAllArg(3);

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
                    send(R.string.chat_video_settings_unknown, param);
                }
            }
        }
    }

    @Override
    protected void initializeSubCommands() {
        Cmd cam = mCommandMap.get("video");
        cam.setHelp(R.string.chat_help_video, "start");
        cam.AddSubCmd("stop", R.string.chat_help_video_stop, null);
        cam.AddSubCmd("params", R.string.chat_help_video_maxDuration, "cameraMaxDurationInSec:[0-9999]");
        cam.AddSubCmd("params", R.string.chat_help_video_maxSize, "cameraMaxFileSizeInMegaBytes:[0-9999]");
        cam.AddSubCmd("params", R.string.chat_help_vdeo_orientation, "cameraRotationInDegree:[0-359]");
        cam.AddSubCmd("params", R.string.chat_help_video_profile, "cameraProfile:[1080p|720p|CIF|QCIF]");
    }
}
