package com.googlecode.gtalksms.cmd;

import android.content.Intent;

import com.googlecode.gtalksms.MainService;
import com.googlecode.gtalksms.panels.Video;
import com.googlecode.gtalksms.tools.Tools;

public class VideoCmd extends CommandHandlerBase {

    public VideoCmd(MainService mainService) {
        super(mainService, CommandHandlerBase.TYPE_MEDIA, "Video", new Cmd("video"));
    }

    public void activate() {
        super.activate();
    }

    @Override
    public synchronized void deactivate() {
        super.deactivate();
    }

    @Override
    protected void execute(String cmd, String args) {
        String[] splitedArgs = splitArgs(args);
        if (isMatchingCmd("video", cmd)) {
            if (args.equals("") || splitedArgs[0].equals("")) {
                Intent intent = new Intent(Video.VIDEO_START, null, sContext, Video.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK + Intent.FLAG_ACTIVITY_SINGLE_TOP);
                sContext.startActivity(intent);
            } else if (splitedArgs[0].equals("stop")) {
                try {
                    Intent intent = new Intent(Video.VIDEO_STOP, null, sContext, Video.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK + Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    sContext.startActivity(intent);
                } catch (Exception e) {
                }
            }
        }
    }


    @Override
    protected void initializeSubCommands() {
        // TODO
    }
}
