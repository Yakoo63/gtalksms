package com.googlecode.gtalksms.cmd;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

import com.googlecode.gtalksms.MainService;
import com.googlecode.gtalksms.R;
import com.googlecode.gtalksms.tools.StringFmt;

public class ApplicationsCmd extends CommandHandlerBase {
  
    public ApplicationsCmd(MainService mainService) {
        super(mainService, CommandHandlerBase.TYPE_SYSTEM, "Applications", new Cmd("applist", "apps"), new Cmd("appfind"), new Cmd("appstart", "start"));
    }

    @Override
    protected void execute(Command cmd) {
        if (isMatchingCmd(cmd, "applist")) {
            appsList(cmd.getAllArg1());
        } else if (isMatchingCmd(cmd, "appstart")) {
            launchApp(cmd.getAllArg1());
        } else if (isMatchingCmd(cmd, "appfind")) {
            findApp(cmd.getAllArg1());
        }
    }

    @Override
    protected void onCommandActivated() {
    }

    @Override
    protected void onCommandDeactivated() {
    }
    
    void appsList(String args) {
        StringBuilder list = new StringBuilder();
        
        boolean doUser = true;
        boolean doSystem = true;
        
        if (args != null) {
            if (args.toLowerCase().equals("user")) {
                doSystem = false;
            } else if (args.toLowerCase().equals("system")) {
                doUser = false;
            }
        }
        
        if (doUser) {
            list.append(getString(R.string.chat_apps_user));
            list.append(StringFmt.join(getPackages(false), "\n")).append("\n");
        }
        
        if (doSystem) {
            list.append(getString(R.string.chat_apps_system));
            list.append(StringFmt.join(getPackages(true), "\n")).append("\n");
        }
        
        send(list.toString());
    }
    
    void findApp(String args) {
        StringBuilder str = new StringBuilder();
        
        final PackageManager pm = sContext.getPackageManager();
        List<ApplicationInfo> packages = pm.getInstalledApplications(PackageManager.GET_META_DATA);
        
        ArrayList<String> list = new ArrayList<String>();
        for (ApplicationInfo packageInfo : packages) {
            String name = packageInfo.loadLabel(pm).toString();
            if (name.toLowerCase().contains(args.toLowerCase()))  {
                list.add(StringFmt.makeBold(name + ": ") + packageInfo.packageName);
            }
        }
        
        if (list.size() > 0) {
            Collections.sort(list);
            str.append(getString(R.string.chat_apps_find, args));
            str.append(StringFmt.join(list, "\n")).append("\n");
        } else {
            str.append(getString(R.string.chat_apps_find_err, args));  
        }
        send(str.toString());
    }
    

    private ArrayList<String> getPackages(boolean isSystem) {
        ArrayList<String> list = new ArrayList<String>();
        
        final PackageManager pm = sContext.getPackageManager();
        List<ApplicationInfo> packages = pm.getInstalledApplications(PackageManager.GET_META_DATA);
        
        for (ApplicationInfo packageInfo : packages) {
            if (isSystem == isSystemPackage(packageInfo))  {
                list.add(StringFmt.makeBold(packageInfo.loadLabel(pm) + ": ") + packageInfo.packageName);
            }
        }
        Collections.sort(list);
        
        return list;
    }

    private boolean isSystemPackage(ApplicationInfo packageInfo) {
        return (packageInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
    }
    
    void launchApp(String name) {
        final PackageManager pm = sContext.getPackageManager();
        List<ApplicationInfo> packages = pm.getInstalledApplications(PackageManager.GET_META_DATA);
        for (ApplicationInfo packageInfo : packages) {
            if (name.toLowerCase().equals(packageInfo.loadLabel(pm).toString().toLowerCase())) {
                try {
                    sContext.startActivity(pm.getLaunchIntentForPackage(packageInfo.packageName));
                    send(R.string.chat_apps_start, name);
                } catch (Exception e) {
                    send(R.string.chat_apps_start_err, name);
                    send(e);
                }
            }
        }
    }

    @Override
    protected void initializeSubCommands() {
        Cmd cmd = mCommandMap.get("applist");
        cmd.setHelp(R.string.chat_help_apps_list_all, null);
        cmd.AddSubCmd("user", R.string.chat_help_apps_list_user, null);
        cmd.AddSubCmd("system", R.string.chat_help_apps_list_system, null);
               
        mCommandMap.get("appfind").setHelp(R.string.chat_help_apps_find, "#app#");   
        mCommandMap.get("appstart").setHelp(R.string.chat_help_apps_start, "#app#");   
    }  
}
