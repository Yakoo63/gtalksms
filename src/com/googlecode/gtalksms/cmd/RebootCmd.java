package com.googlecode.gtalksms.cmd;

import android.content.Context;
import android.os.Build;
import android.os.PowerManager;

import com.googlecode.gtalksms.MainService;

/**
 * Nice idea at first, a reboot could remotely lock a device and "reboot:recovery"
 * could become handy. But as of
 * https://groups.google.com/forum/#!topic/android-developers/0N8iNqyEufQ
 * this is only supported for system apps etc.
 * 
 * Maybe this works on some custom ROMs, so I let this in as hidden
 * command.
 * 
 * @author Florian Schmaus fschmaus@gmail.com - on behalf of the GTalkSMS Team
 *
 */
public class RebootCmd extends CommandHandlerBase {
    
    private PowerManager mPowerManager;

    public RebootCmd(MainService mainService) {
        super(mainService, CommandHandlerBase.TYPE_INTERNAL, "Reboot", new Cmd("reboot"));
    }

    @Override
    protected void execute(Command cmd) {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.FROYO) {
            send("You need to run at last Froyo to issue the " + cmd + " command");
            return;
        }
        String args = cmd.getAllArg1();
        mPowerManager.reboot(args.equals("") ? null : args);
    }

    @Override
    protected void onCommandActivated() {
        mPowerManager = (PowerManager) sContext.getSystemService(Context.POWER_SERVICE);
    }

    @Override
    protected void onCommandDeactivated() {
        mPowerManager = null;
    }
    
    @Override
    protected void initializeSubCommands() {
    }
}
