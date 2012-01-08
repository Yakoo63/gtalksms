package com.googlecode.gtalksms.cmd;

import java.util.Arrays;

public class Cmd {
    private String mName;
    private String [] mAlias;
    
    Cmd(String name, Object... alias) {
        mName = name;
        mAlias = Arrays.copyOf(alias, alias.length, String[].class);
    }
    
    public String getName() {
        return mName;
    }
    
    public String[] getAlias() {
        return mAlias;
    }
}