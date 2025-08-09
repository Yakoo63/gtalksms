package com.googlecode.gtalksms.cmd;

import com.googlecode.gtalksms.MainService;
import com.googlecode.gtalksms.R;
import com.googlecode.gtalksms.tools.GoogleMail;
import com.googlecode.gtalksms.tools.Log;

public class MailCmd extends CommandHandlerBase {

    public static final String CMD_EMAIL = "email";
    public static final String CMD_EMAIL_FILE = "emailfile";

    public MailCmd(MainService mainService) {
        super(mainService, CommandHandlerBase.TYPE_MESSAGE, "EMail", new Cmd(CMD_EMAIL, "e"), new Cmd(CMD_EMAIL_FILE, "ef"));
    }

    @Override
    protected void execute(Command cmd) {
        try {
            if (isMatchingCmd(cmd, CMD_EMAIL)) {
                String dest = cmd.getArg1();
                String subject = cmd.getArg2();
                String message = cmd.getAllArg(3);
                new GoogleMail(sContext).send(subject, message, dest);
                send(R.string.chat_email_sent);
            } else if (isMatchingCmd(cmd, CMD_EMAIL_FILE)) {
                String dest = cmd.getArg1();
                String subject = cmd.getArg2();
                String files[] = cmd.getArg(3).split("\\|");
                String message = cmd.getAllArg(4);
                send(R.string.chat_emailfile_sending, cmd.getArg(3));
                new GoogleMail(sContext).send(subject, message, dest, files);
                send(R.string.chat_email_sent);
            }
        } catch (Exception e) {
            Log.e("Email error for command: " + cmd.getOriginalCommand(), e);
            send(R.string.chat_error, e.getMessage());
        }
    }

    @Override protected void onCommandActivated() {}
    @Override protected void onCommandDeactivated() {}

    @Override
    protected void initializeSubCommands() {
        Cmd cam = mCommandMap.get("email");
        cam.setHelp(R.string.chat_help_email, "[#to#]:#subject#:#message#");
        Cmd flash = mCommandMap.get("emailfile");
        flash.setHelp(R.string.chat_help_emailfile, "[#to#]:#subject#:#file1#[|#file2#|#file3#]:#message#");
    }
}
