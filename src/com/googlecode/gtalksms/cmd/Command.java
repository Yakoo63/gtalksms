package com.googlecode.gtalksms.cmd;

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
	
	public Command(String originalCommand, String replyTo) {
		this.originalCommand = originalCommand == null ? "" : originalCommand.trim();
		this.replyTo = replyTo;
	}

	@Deprecated
	public Command(String cmd, String args, String replyTo) {
		if(args == null || "".equals(args))
			this.originalCommand = cmd;
		else
			this.originalCommand = cmd+":"+args;
		this.replyTo = replyTo;
	}

	public abstract void respond(String message);
	
	public void respond(int id, Object... args) {
		
	}

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
		if(x > -1)
			return originalCommand.substring(x+1).trim();
		return "";
	}
	
	public String getOriginalCommand() {
		return originalCommand;
	}

	public String getCommand() {
		int x = originalCommand.indexOf(separator);
		if(x > -1)
			return originalCommand.substring(0, x).trim();
		return originalCommand;
	}

	/**
	 * Get the first argument. Stops at separator.
	 * 
	 * @return first argument if any, otherwise empty string. Never returns null.
	 */
	public String get1() {
		int x = originalCommand.indexOf(separator);
		if(x < 0 || x == originalCommand.length())
			return "";
		int y = originalCommand.indexOf(separator, x+1);
		if(y<0)
			y = originalCommand.length();
		return originalCommand.substring(x+1,y).trim();
	}
	
	/**
	 * Get the second argument. Does NOT stop at separator.
	 * 
	 * @return second argument if any, otherwise empty string. Never returns null.
	 */
	public String get2() {
		int x = originalCommand.indexOf(separator);
		if(x < 0)
			return "";
		int y = originalCommand.indexOf(separator, x+1);
		if(y<0)
			return "";
		return originalCommand.substring(y+1);
	}
}
