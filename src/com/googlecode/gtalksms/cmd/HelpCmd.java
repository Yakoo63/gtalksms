package com.googlecode.gtalksms.cmd;

import java.util.Map;
import java.util.Set;

import android.util.Log;

import com.googlecode.gtalksms.MainService;
import com.googlecode.gtalksms.R;
import com.googlecode.gtalksms.tools.StringFmt;
import com.googlecode.gtalksms.tools.Tools;
import com.googlecode.gtalksms.xmpp.XmppMsg;

public class HelpCmd extends CommandHandlerBase {
    private static XmppMsg _msg;  // brief help message
    private static XmppMsg _msgAll;   // full help message    
    private Map<String, CommandHandlerBase> commands;
    
    private static XmppMsg _msgContact;
    private static XmppMsg _msgMessage;
    private static XmppMsg _msgGeo;
    private static XmppMsg _msgSystem;
    private static XmppMsg _msgCopy;
    private static XmppMsg _msgMedia;
    private static XmppMsg _msgInternal; // Dev or Expert users
    
    private static XmppMsg _msgCategories = new XmppMsg();
    
    private String format(int resHelp, Object... objects ) {
        String [] keys = new String[objects.length];
        for (int i = 0 ; i < objects.length ; ++i) {
            keys[i] = objects[i].toString();  
        }
        return "- " + StringFmt.join(keys, getString(R.string.or), true) + ": " + getString(resHelp);
    }
    
    public HelpCmd(MainService mainService) {
        super(mainService, CommandHandlerBase.TYPE_INTERNAL, "Help", new Cmd("?", "help"));
        
        _msg = new XmppMsg();
        _msgAll = new XmppMsg();
        _msgContact = new XmppMsg();
        _msgMessage = new XmppMsg();
        _msgGeo = new XmppMsg();
        _msgSystem = new XmppMsg();
        _msgCopy = new XmppMsg();
        _msgMedia = new XmppMsg();
        _msgCategories = new XmppMsg();
        _msgInternal = new XmppMsg();
            
        String contactCmds = "";
        String messageCmds = "";
        String geoCmds = "";
        String systemCmds = "";
        String mediaCmds = "";
        String copyCmds = "";
        String internalCmds = "";
            
        commands = mainService.getActiveCommands();
        Set<CommandHandlerBase> commandSet = mainService.getActiveCommandSet();
        
        _msg.appendLine(getString(R.string.chat_help_title));
        _msg.appendLine(format(R.string.chat_help_help, "\"help\""));
        _msg.appendLine(format(R.string.chat_help_help_all, "\"help:all\""));
        _msg.appendLine(format(R.string.chat_help_help_categories, "\"help:categories\"", "\"help:cat\""));
                     
        _msgAll.append(_msg); // attach the header to the full help message
        
        _msg.appendLine("- " + makeBold("\"help:#command#\"") + " - " + makeBold("\"help:#category#\""));
        
        for (CommandHandlerBase c : commandSet) {
            String[] helpLines = c.help();
            String str = c.getCommandsAsString();
            
            // do nothing if the command provides no help
            if (helpLines == null)  { 
                if (c.mCmdType == CommandHandlerBase.TYPE_INTERNAL) {
                    internalCmds += str;
                }
                continue;
            }
            
            addLinesToMsg(_msgAll, helpLines);
            
            switch (c.mCmdType) {
            case CommandHandlerBase.TYPE_CONTACTS:
                contactCmds += str;
                addLinesToMsg(_msgContact, helpLines);
                break;
            case CommandHandlerBase.TYPE_COPY:
                copyCmds += str;
                addLinesToMsg(_msgCopy, helpLines);
                break;
            case CommandHandlerBase.TYPE_GEO:
                geoCmds += str;
                addLinesToMsg(_msgGeo, helpLines);
                break;
            case CommandHandlerBase.TYPE_MESSAGE:
                messageCmds += str;
                addLinesToMsg(_msgMessage, helpLines);
                break;
            case CommandHandlerBase.TYPE_SYSTEM:
                systemCmds += str;
                addLinesToMsg(_msgSystem, helpLines);
                break;
            case CommandHandlerBase.TYPE_MEDIA:
                mediaCmds += str;
                addLinesToMsg(_msgMedia, helpLines);
                break;
            default:
                Log.w(Tools.LOG_TAG, "help command unkown command type");           
            }
        }
        
        contactCmds = StringFmt.delLastChar(contactCmds, 2);
        messageCmds = StringFmt.delLastChar(messageCmds, 2);
        geoCmds = StringFmt.delLastChar(geoCmds, 2);
        systemCmds = StringFmt.delLastChar(systemCmds, 2);
        copyCmds = StringFmt.delLastChar(copyCmds, 2);
        mediaCmds = StringFmt.delLastChar(mediaCmds, 2);
        internalCmds = StringFmt.delLastChar(internalCmds, 2);
        
        _msgInternal.appendLine(makeBold("Internal commands") + ": " + internalCmds);
        
        // after we have iterated over the command set, we can assemble the category message
        _msgCategories.appendLine(getString(R.string.chat_help_title));
        _msgCategories.appendLine("- " + makeBold("\"help:contacts\"") + ": " + contactCmds);
        _msgCategories.appendLine("- " + makeBold("\"help:text\"") + ": " + copyCmds);               
        _msgCategories.appendLine("- " + makeBold("\"help:geo\"") + ": " + geoCmds);
        _msgCategories.appendLine("- " + makeBold("\"help:media\"") + ": " + mediaCmds);
        _msgCategories.appendLine("- " + makeBold("\"help:message\"") + ": " + messageCmds);
        _msgCategories.appendLine("- " + makeBold("\"help:system\"") + ": " + systemCmds);
    }

    @Override
    protected void execute(String cmd, String args) {
        if (args.equals("all")) {
            send(_msgAll);
        } else if (commands.containsKey(args)) {
            String[] helpLines = commands.get(args).help();
            if (helpLines != null) {
                XmppMsg helpMsg = new XmppMsg();
                addLinesToMsg(helpMsg, helpLines);
                send(helpMsg);
            }
        } else if (args.equals("contacts")) {
            send(_msgContact);
        } else if (args.equals("message")) {
            send(_msgMessage);
        } else if (args.equals("geo")) {
            send(_msgGeo);
        } else if (args.equals("system")) {
            send(_msgSystem);
        } else if (args.equals("media")) {
            send(_msgMedia);
        } else if (args.equals("text")) {
            send(_msgCopy);
        } else if (args.equals("internal")) {
            send(_msgInternal);
        } else if (args.equals("cat") || args.equals("categories")) {
            send(_msgCategories);
        } else {
            send(_msg);
        }
    }
    
    /**
     * Adds lines to the XmppMsg, one per line
     * does nothing if lines is null
     * 
     * @param msg
     * @param lines - can be null
     */
    private static final void addLinesToMsg(XmppMsg msg, String[] lines) {
        if (lines == null) return;
        for (String line : lines) 
            msg.appendLine(line);
    }

    @Override
    protected void initializeSubCommands() {
    }
}
