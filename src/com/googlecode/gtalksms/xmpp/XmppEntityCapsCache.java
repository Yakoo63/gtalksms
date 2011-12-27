package com.googlecode.gtalksms.xmpp;

import java.io.File;

import org.jivesoftware.smackx.entitycaps.EntityCapsManager;
import org.jivesoftware.smackx.entitycaps.EntityCapsPersistentCache;
import org.jivesoftware.smackx.entitycaps.SimpleDirectoryPersistentCache;

import android.content.Context;

public class XmppEntityCapsCache {
    private static final String CACHE_DIR = "EntityCapsCache";
    private static EntityCapsPersistentCache sCache;
    
    public static void enableEntityCapsCache(Context ctx) {
        File cacheDir = new File(ctx.getFilesDir(), CACHE_DIR);
        
        if (!cacheDir.exists())
            if (!cacheDir.mkdir())
                throw new IllegalStateException("Can not create entity caps cache dir");
        // Disabled for now
        sCache = new SimpleDirectoryPersistentCache(cacheDir);
        EntityCapsManager.setPersistentCache(sCache);
    }
    
    public static void emptyCache() {
        if (sCache != null)
            sCache.emptyCache();
    }
}
