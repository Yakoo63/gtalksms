package com.googlecode.gtalksms.cmd;

import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Strongly typed representation of the user's command.
 * 
 * @author Hugo
 */
public class Command {
    //The string received from the user.
    private final String originalCommand;
    private final String replyTo;
    private final static String separator = ":";
    private String[] args;

    public Command(String originalCommand, String replyTo) {
        this.originalCommand = originalCommand == null ? "" : originalCommand.trim();
        this.replyTo = replyTo;
        this.args = this.originalCommand == null ? new String[] { "" } : TextUtils.split(this.originalCommand, separator);
    }

    public Command(String cmd, String args, String replyTo) {
        this(cmd + separator + (args == null ? "" : args), replyTo);
    }

    public String getReplyTo() { return replyTo; }
    public String getOriginalCommand() { return originalCommand; }
    public String getCommand() { return args[0]; }

    public String getArg1() { return args.length > 1 ? args[1] : ""; }
    public String getArg2() { return args.length > 2 ? args[2] : ""; }
    public String getArg(int index) { return args.length > index ? args[index] : ""; }

    public String getAllArg1() { return getEndOfString(1); }
    public String getAllArg2() { return getEndOfString(2); }
    public String getAllArg(int index) { return getEndOfString(index); }

    private String getEndOfString(int index) {
        List<String> tab = new ArrayList<String>(Arrays.asList(args));
        for (int i = 0 ; i < index && tab.size() > 0 ; ++i) {
            tab.remove(0);
        }

        return tab.size() > 0 ? TextUtils.join(separator, tab) : "";
    }
}
