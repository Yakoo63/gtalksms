package com.googlecode.gtalksms.cmd;

import android.test.AndroidTestCase;

public class TestUserCommand extends AndroidTestCase {
    Command cmd;
    
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
        assertEquals("arg1", cmd("mycmd:arg1:arg2").getArg1());
        assertEquals("arg1", cmd("mycmd : arg1 : arg2").getArg1());
        assertEquals("arg1", cmd("mycmd:arg1:arg2").getArg1());
        assertEquals("arg1", cmd("mycmd:arg1").getArg1());
        assertEquals("", cmd("mycmd::arg2").getArg1());
        assertEquals("", cmd("mycmd:").getArg1());
        assertEquals("", cmd("mycmd").getArg1());
        assertEquals("", cmd(":").getArg1());
        assertEquals("", cmd("").getArg1());
    }
    
    public void testGet2() {
        assertEquals("arg2", cmd("mycmd:arg1:arg2").getArg2());
        assertEquals("", cmd("mycmd:arg1:").getArg2());
        assertEquals("", cmd("mycmd:arg1").getArg2());
    }

    private Command cmd(String cmd) {
        return new NonReplyingUserCommand(cmd, "replyTo");
    }
    
    static class NonReplyingUserCommand extends Command {
        public NonReplyingUserCommand(String originalCommand, String replyTo) {
            super(originalCommand, replyTo);
        }
    }

}
