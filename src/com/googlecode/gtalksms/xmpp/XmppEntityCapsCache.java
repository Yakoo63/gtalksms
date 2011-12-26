package com.googlecode.gtalksms.xmpp;

import java.io.File;

import org.jivesoftware.smackx.entitycaps.EntityCapsManager;
import org.jivesoftware.smackx.entitycaps.EntityCapsPersistentCache;
import org.jivesoftware.smackx.entitycaps.SimpleDirectoryPersistentCache;

import android.content.Context;

public class XmppEntityCapsCache {
    private static final String CACHE_DIR = "EntityCapsCache";
    
    public static void enableEntityCapsCache(Context ctx) {
        File cacheDir = new File(ctx.getFilesDir(), CACHE_DIR);
        
        if (!cacheDir.exists())
            if (!cacheDir.mkdir())
                throw new IllegalStateException("Can not create entity caps cache dir");
        // Disabled for now
        EntityCapsPersistentCache cache = new SimpleDirectoryPersistentCache(cacheDir);
        EntityCapsManager.setPersistentCache(cache);
    }
}
