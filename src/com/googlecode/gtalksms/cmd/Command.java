package com.googlecode.gtalksms.cmd;

import android.text.TextUtils;

/**
 * Strongly typed representation of the user's command.
 * 
 * @author Hugo
 */
public abstract class Command {
    //The string received from the user.
    private final String originalCommand;
    private final String replyTo;
    private final String separator = ":";
    private String[] args;
    
    public Command(String originalCommand, String replyTo) {
        this.originalCommand = originalCommand == null ? "" : originalCommand.trim();
        this.replyTo = replyTo;
        this.args = this.originalCommand == null ? new String[] { "" } : TextUtils.split(this.originalCommand, ":");
    }

    @Deprecated
    public Command(String cmd, String args, String replyTo) {
        if(args == null || args.equals("")) {
            this.originalCommand = cmd;
        } else {
            this.originalCommand = cmd + ":" + args;
        }
        this.replyTo = replyTo;
        this.args = this.originalCommand == null ? new String[] { "" } : TextUtils.split(this.originalCommand, ":");
    }

    public abstract void respond(String message);

    public String getReplyTo() {
        return replyTo;
    }

    /**
     * Returns stripped everything after first colon or "" if no colon exists in original command.
     * 
     * @return all arguments as defined above, never null.
     */
    public String getAllArguments() {
        int x = originalCommand.indexOf(separator);
        if (x > -1) {
            return originalCommand.substring(x+1).trim();
        }
        return "";
    }
    
    public String getOriginalCommand() {
        return originalCommand;
    }

    public String getCommand() {
        return args[0];
    }

    /**
     * Get the Nth argument. Stops at separator.
     *
     * @return Nth argument if any, otherwise empty string. Never returns null.
     */
    public String get1() { return args.length > 1 ? args[1] : ""; }
    public String get2() { return args.length > 2 ? args[2] : ""; }
    public String get3() { return args.length > 3 ? args[3] : ""; }
}
