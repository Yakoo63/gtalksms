package com.googlecode.gtalksms.cmd;

import android.text.TextUtils;

import com.googlecode.gtalksms.MainService;
import com.googlecode.gtalksms.tools.GoogleMail;
import com.googlecode.gtalksms.tools.Log;

public class MailCmd extends CommandHandlerBase {

    public MailCmd(MainService mainService) {
        super(mainService, CommandHandlerBase.TYPE_INTERNAL, "EMail", new Cmd("email", "e"), new Cmd("emailfile", "ef"));
    }

    @Override
    protected void execute(Command cmd) {
        if (cmd.getCommand().equals("email")) {
            try {
                String dest = cmd.getArg1();
                String subject = cmd.getArg2();
                String message = cmd.getAllArg(3);
                new GoogleMail(sContext).send(subject, message, dest);
                send("Email sent");
            } catch (Exception e) {
                Log.e("Error", e);
                send("Error: " + e.getMessage());
            }
        } else if (cmd.getCommand().equals("emailfile")) {
            try {
                String dest = cmd.getArg1();
                String subject = cmd.getArg2();
                String files[] = cmd.getArg(3).split("\\|");
                String message = cmd.getAllArg(4);
                send("Sending files '" + cmd.getArg(3) + "'...");
                new GoogleMail(sContext).send(subject, message, dest, files);
                send("Email sent");
            } catch (Exception e) {
                Log.e("Error", e);
                send("Email error: " + e.getMessage());
            }
        }
    }

    @Override protected void onCommandActivated() {}
    @Override protected void onCommandDeactivated() {}

    @Override
    protected void initializeSubCommands() {
    }
}
