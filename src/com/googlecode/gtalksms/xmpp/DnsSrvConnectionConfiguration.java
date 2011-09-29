package com.googlecode.gtalksms.xmpp;

import org.jivesoftware.smack.ConnectionConfiguration;

public class DnsSrvConnectionConfiguration {
    
    public static ConnectionConfiguration getDnsSrvConnectionConfiguration(String serviceName) {
        class DoDnsSrvLookup implements Runnable {
            ConnectionConfiguration conConf;
            String serviceName;
            
            public DoDnsSrvLookup(String serviceName) {
                this.serviceName = serviceName;
            }
            
            @Override
            public void run() {
                conConf = new ConnectionConfiguration(serviceName);              
            }
            
            public ConnectionConfiguration getConnectionConfiguration() {
                return conConf;
            }
        }
        
        DoDnsSrvLookup dnsSrv = new DoDnsSrvLookup(serviceName); 
        Thread t = new Thread(dnsSrv, "dns-srv-lookup");
        t.start();
        try {
            t.join(3000);
        } catch (InterruptedException e) {
            return null;
        }
        return dnsSrv.getConnectionConfiguration();        
    }

}
