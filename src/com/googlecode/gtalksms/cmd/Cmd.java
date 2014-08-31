package com.googlecode.gtalksms.cmd;

import java.util.ArrayList;
import java.util.Arrays;

import android.content.Context;
import android.preference.PreferenceManager;

import com.googlecode.gtalksms.R;
import com.googlecode.gtalksms.tools.StringFmt;
import com.googlecode.gtalksms.xmpp.XmppMsg;

public class Cmd {
    public class SubCmd {
        private final String mName;
        private final String mHelp;
        private String mHelpMsg;
        private final String mHelpArgs;
        private final String [] mAlias;
        
        SubCmd(String name, Cmd baseCmd, int resHelp, String args, Object... alias) {
            mName = name.toLowerCase();
            mAlias = new String[alias.length];
            for (int i = 0 ; i < alias.length ; ++i) {
                mAlias[i] = alias[i].toString().toLowerCase();  
            }
            
            if (resHelp > 0) {
                mHelpMsg = getString(resHelp);
            }
            mHelpArgs = args;
            mHelp = buildHelp(baseCmd, mName, mAlias, resHelp, args);
        }
        
        public String getName() {
            return mName;
        }
        
        public String getHelp() {
            return mHelp;
        }

        public String getHelpMsg() {
            return mHelpMsg;
        }
        
        public String getHelpArgs() {
            return mHelpArgs;
        }
        
        public String[] getAlias() {
            return mAlias;
        }
    }
    
    private final String mName;
    private int mResHelp;
    private String mHelpArgs;
    private final String [] mAlias;
    private final ArrayList<SubCmd> mSubCmds;
    private static Context sContext;
    private boolean mDefaultActivationValue;

    // Default status
    public final static boolean ENABLED = true;
    public final static boolean DISABLED = false;

    Cmd(String name, String... alias) {
        this(name, Cmd.ENABLED, alias);
    }

    Cmd(String name, boolean defaultActivationValue, String... alias) {
        mDefaultActivationValue = defaultActivationValue;
        mName = name.toLowerCase();
        mAlias = new String[alias.length];
        for (int i = 0 ; i < alias.length ; ++i) {
            mAlias[i] = alias[i].toLowerCase();
        }
        mSubCmds = new ArrayList<SubCmd>();
    }

    public boolean isActive() {
        return PreferenceManager.getDefaultSharedPreferences(sContext).getBoolean("cmd_" + mName, mDefaultActivationValue);
    }

    public void setActive(boolean val) {
        PreferenceManager.getDefaultSharedPreferences(sContext).edit().putBoolean("cmd_" + mName, val).commit();
    }

    public void AddSubCmd(String name, int resHelp) {
        mSubCmds.add(new SubCmd(name, this, resHelp, null, new Object[]{}));
    }

    public void AddSubCmd(String name, int resHelp, String args, Object... alias) {
        mSubCmds.add(new SubCmd(name, this, resHelp, args, alias));
    }
    
    public static void setContext(Context c) {
        sContext = c;
    }
    
    private static String getString(int id, Object... args) {
        return sContext.getString(id, args);
    }
    
    private static String buildHelp(Cmd root, String name, String[] alias, int resHelp, String args) {
        if (resHelp <= 0) {
            return null;
        }
        
        ArrayList<String> cmds = new ArrayList<String>();

        if (root != null) {
            ArrayList<String> roots = new ArrayList<String>();
            roots.add(root.mName);
            roots.addAll(Arrays.asList(root.mAlias));

            for (String r : roots) {
                ArrayList<String> cur = new ArrayList<String>();
                cur.add(name);
                cur.addAll(Arrays.asList(alias));
                for (String c : cur) {
                    cmds.add("\"" + r + ":" + c + (args == null ? "" : ":" + args) + "\"");
                }
            }
        } else {
            cmds.add(name);
            cmds.addAll(Arrays.asList(alias));
            for (int i = 0 ; i < cmds.size() ; ++i) {
                cmds.set(i, "\"" + cmds.get(i) + (args == null ? "" : ":" + args) + "\"");
            }
        }
        return "- " + StringFmt.join(cmds, getString(R.string.or), true) + " : " + getString(resHelp);
    }
    
    public void setHelp(int resHelp, String args) {
        mResHelp = resHelp;
        mHelpArgs = args;
    }
    
    public void setHelp(int resHelp) {
        setHelp(resHelp, null);
    }
    
    public String getName() {
        return mName;
    }
    
    public String getHelp() {
        return buildHelp(null, mName, mAlias, mResHelp, mHelpArgs);
    }
    
    public String getHelpMsg() {
        if (mResHelp > 0) {
            return getString(mResHelp);
        } else {
            return "";
        }
    }
    
    public String getHelpArgs() {
        return mHelpArgs;
    }
    
    public String getHelpSummary() {
        return mResHelp <= 0 ? "" : getString(mResHelp);
    }
    
    public String[] getAlias() {
        return mAlias;
    }    
 
    public ArrayList<SubCmd> getSubCmds() {
        return mSubCmds;
    }
}