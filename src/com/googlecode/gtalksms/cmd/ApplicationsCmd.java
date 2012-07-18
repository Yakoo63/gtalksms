package com.googlecode.gtalksms.cmd;

import java.util.List;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

import com.googlecode.gtalksms.MainService;

public class ApplicationsCmd extends CommandHandlerBase {
  
    public ApplicationsCmd(MainService mainService) {
        super(mainService, CommandHandlerBase.TYPE_SYSTEM, new Cmd("appList", "apps"), new Cmd("startApp", "start"));
    }

    protected void execute(String cmd, String args) {
        if (isMatchingCmd("appList", cmd)) {
            applicationsList();
        } else if (isMatchingCmd("startApp", cmd)) {
            launchApp(args);
        }
    }
    
    protected void applicationsList() {
        final PackageManager pm = sContext.getPackageManager();
        List<ApplicationInfo> packages = pm.getInstalledApplications(PackageManager.GET_META_DATA);
        StringBuilder list = new StringBuilder();
        list.append("Installed applications are:");
        for (ApplicationInfo packageInfo : packages) {
            list.append("\n" + packageInfo.loadLabel(pm) + ": " + packageInfo.packageName);
        }
        send(list.toString());
    }

    protected void launchApp(String name) {
        final PackageManager pm = sContext.getPackageManager();
        List<ApplicationInfo> packages = pm.getInstalledApplications(PackageManager.GET_META_DATA);
        for (ApplicationInfo packageInfo : packages) {
            if (name.toLowerCase().equals(packageInfo.loadLabel(pm).toString().toLowerCase())) {
                try {
                    sContext.startActivity(pm.getLaunchIntentForPackage(packageInfo.packageName));
                    send("Application " + name + " started.");
                    return;
                } catch (Exception e) {
                }
            }
        }
        send("Failed to launch application: " + name);
    }

    @Override
    protected void initializeSubCommands() {
    }  
}
