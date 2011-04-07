package com.googlecode.gtalksms.cmd;

import android.test.AndroidTestCase;

public class TestUserCommand extends AndroidTestCase {
	UserCommand cmd;
	
	protected void setUp() throws Exception {
		super.setUp();
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}
	
	public void testGetCommand() {
		assertEquals("cmd", cmd("cmd").getCommand());
		assertEquals("cmd", cmd("cmd:arg").getCommand());
		assertEquals("cmd", cmd("cmd: arg").getCommand());
		assertEquals("cmd", cmd("cmd :arg").getCommand());
		assertEquals("cmd", cmd(" cmd :arg").getCommand());
		assertEquals("cmd", cmd(" cmd:arg").getCommand());
		assertEquals("cmd", cmd(" cmd:").getCommand());
		assertEquals("cmd", cmd(" cmd").getCommand());
		assertEquals("", cmd("").getCommand());
		assertEquals("", cmd(null).getCommand());
		assertEquals("", cmd(":").getCommand());
		assertEquals("", cmd(":arg").getCommand());
	}
	
	public void testGetAllArguments() {
		assertEquals("arg1 arg2", cmd("mycmd:arg1 arg2").getAllArguments());
		assertEquals("arg1 arg2", cmd("mycmd :arg1 arg2").getAllArguments());
		assertEquals("arg1 arg2", cmd("mycmd: arg1 arg2 ").getAllArguments());
		assertEquals("", cmd("mycmd:").getAllArguments());
		assertEquals("", cmd("mycmd :").getAllArguments());
		assertEquals("", cmd(":").getAllArguments());
		assertEquals("", cmd("").getAllArguments());
	}
	
	public void testGet1() {
		assertEquals("arg1", cmd("mycmd:arg1:arg2").get1());
		assertEquals("arg1", cmd("mycmd : arg1 : arg2").get1());
		assertEquals("arg1", cmd("mycmd:arg1:arg2").get1());
		assertEquals("arg1", cmd("mycmd:arg1").get1());
		assertEquals("", cmd("mycmd::arg2").get1());
		assertEquals("", cmd("mycmd:").get1());
		assertEquals("", cmd("mycmd").get1());
		assertEquals("", cmd(":").get1());
		assertEquals("", cmd("").get1());
	}
	
	public void testGet2() {
		assertEquals("arg2", cmd("mycmd:arg1:arg2").get2());
		assertEquals("", cmd("mycmd:arg1:").get2());
		assertEquals("", cmd("mycmd:arg1").get2());
	}

	private UserCommand cmd(String cmd) {
		return new NonReplyingUserCommand(cmd, "replyTo");
	}
	
	static class NonReplyingUserCommand extends UserCommand {
		public NonReplyingUserCommand(String originalCommand, String replyTo) {
			super(originalCommand, replyTo);
		}

		@Override
		public void respond(String message) {
			return;
		}
	}

}
