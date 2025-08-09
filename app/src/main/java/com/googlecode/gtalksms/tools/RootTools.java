package com.googlecode.gtalksms.tools;

import java.io.DataOutputStream;

public class RootTools {
    
    /**
     * Ask from the supervising app for root access
     * 
     * @return true if root access could be achieved, otherwise false
     */
    public static boolean askRootAccess() {
        try {
            Process p = Runtime.getRuntime().exec("su");

            DataOutputStream os = new DataOutputStream(p.getOutputStream());
            // TODO issue "id" command an check if result contains "uid=0"
            os.writeBytes("exit\n");
            os.flush();
            p.waitFor();
            return p.exitValue() != 255;
        } catch (Exception e) {
            return false;
        }
    }
}
