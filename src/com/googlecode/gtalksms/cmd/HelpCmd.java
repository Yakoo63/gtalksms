package com.googlecode.gtalksms.cmd;

import java.util.Map;
import java.util.Set;

import android.util.Log;

import com.googlecode.gtalksms.MainService;
import com.googlecode.gtalksms.R;
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
    
    private static XmppMsg _msgCategories = new XmppMsg();
    
    
    public HelpCmd(MainService mainService) {
        super(mainService, new String[] {"?", "help"}, CommandHandlerBase.TYPE_SYSTEM);
        
        _msg = new XmppMsg();
        _msgAll = new XmppMsg();
        _msgContact = new XmppMsg();
        _msgMessage = new XmppMsg();
        _msgGeo = new XmppMsg();
        _msgSystem = new XmppMsg();
        _msgCopy = new XmppMsg();
        _msgCategories = new XmppMsg();
        
        String contactCmds = "";
        String messageCmds = "";
        String geoCmds = "";
        String systemCmds = "";
        String copyCmds = "";
        
        commands = mainService.getCommands();
        Set<CommandHandlerBase> commandSet = mainService.getCommandSet();
        
        _msg.appendLine(getString(R.string.chat_help_title));
        _msg.appendLine(getString(R.string.chat_help_help, makeBold("\"?\""), makeBold("\"help\"")));
        _msg.appendLine(getString(R.string.chat_help_help_all, makeBold("\"help:all\"")));
        _msg.appendLine(getString(R.string.chat_help_help_categories, makeBold("\"help:categories\""), makeBold("\"help:cat\"") ));
                     
        _msgAll.append(_msg); // attach the header to the full help message
        
        _msg.appendLine("- " + makeBold("\"help:#command#\"") + " - " + makeBold("\"help:#category#\""));
        
        for (CommandHandlerBase c : commandSet) {
            String[] helpLines = c.help();
            if (helpLines == null)  // do nothing if the command provides no help
                continue;
            
            addLinesToMsg(_msgAll, helpLines);
            
            switch (c.mCmdType) {
            case (CommandHandlerBase.TYPE_CONTACTS):
                contactCmds = contactCmds + c.getCommandsAsString() + " ";
                addLinesToMsg(_msgContact, helpLines);
                break;
            case (CommandHandlerBase.TYPE_COPY):
                copyCmds = copyCmds + c.getCommandsAsString()  + " ";
                addLinesToMsg(_msgCopy, helpLines);
                break;
            case (CommandHandlerBase.TYPE_GEO):
                geoCmds = geoCmds + c.getCommandsAsString()  + " ";
                addLinesToMsg(_msgGeo, helpLines);
            break;
            case (CommandHandlerBase.TYPE_MESSAGE):
                messageCmds = messageCmds + c.getCommandsAsString()  + " ";
                addLinesToMsg(_msgMessage, helpLines);
                break;
            case (CommandHandlerBase.TYPE_SYSTEM):
                systemCmds = systemCmds + c.getCommandsAsString()  + " ";
                addLinesToMsg(_msgSystem, helpLines);
                break;
            default:
                Log.w(Tools.LOG_TAG, "help command unkown command type");           
            }
        }
        
        contactCmds = contactCmds.substring(0, contactCmds.length() - 1);
        messageCmds = messageCmds.substring(0, messageCmds.length() - 1);
        geoCmds = geoCmds.substring(0, geoCmds.length()- 1);
        systemCmds = systemCmds.substring(0, systemCmds.length() - 1);
        copyCmds = copyCmds.substring(0, copyCmds.length() - 1);
        
        // after we have iterated over the command set, we can assemble the category message
        _msgCategories.appendLine(getString(R.string.chat_help_title));
        _msgCategories.appendLine("- " + makeBold("\"help:contacts\"") + ": " + contactCmds);
        _msgCategories.appendLine("- " + makeBold("\"help:message\"") + ": " + messageCmds);
        _msgCategories.appendLine("- " + makeBold("\"help:geo\"") + ": " + geoCmds);
        _msgCategories.appendLine("- " + makeBold("\"help:system\"") + ": " + systemCmds);
        _msgCategories.appendLine("- " + makeBold("\"help:copy\"") + ": " + copyCmds);               
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
        } else if (args.equals("copy")) {
            send(_msgCopy);
        } else if (args.equals("cat") || args.equals("categories")) {
            send(_msgCategories);
        } else {
            send(_msg);
        }
    }
    
    @Override
    public String[] help() {
        return null;
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
}
