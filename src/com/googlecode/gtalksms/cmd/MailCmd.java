package com.googlecode.gtalksms.cmd;

import com.googlecode.gtalksms.MainService;
import com.googlecode.gtalksms.tools.GoogleMail;
import com.googlecode.gtalksms.tools.Log;

public class MailCmd extends CommandHandlerBase {

    public MailCmd(MainService mainService) {
        super(mainService, CommandHandlerBase.TYPE_INTERNAL, "EMail", new Cmd("email"));
    }

    @Override
    protected void execute(Command cmd) {
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
    }

    @Override protected void onCommandActivated() {}
    @Override protected void onCommandDeactivated() {}

    @Override
    protected void initializeSubCommands() {
    }
}
