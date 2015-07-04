package com.googlecode.gtalksms.xmpp;

import java.io.File;

import org.jivesoftware.smackx.caps.EntityCapsManager;
import org.jivesoftware.smackx.caps.cache.EntityCapsPersistentCache;
import org.jivesoftware.smackx.caps.cache.SimpleDirectoryPersistentCache;

import android.content.Context;

import com.googlecode.gtalksms.tools.Log;

public class XmppEntityCapsCache {
    private static final String CACHE_DIR = "EntityCapsCacheBase32";
    private static EntityCapsPersistentCache sCache;
    
    public static void enableEntityCapsCache(Context ctx) {
        File cacheDir = new File(ctx.getFilesDir(), CACHE_DIR);
        
        if (!cacheDir.exists() && !cacheDir.mkdir()) {
            throw new IllegalStateException("Can not create entity caps cache dir");
        }

        sCache = new SimpleDirectoryPersistentCache(cacheDir);
        try {
            EntityCapsManager.setPersistentCache(sCache);
        } catch (Exception e) {
            Log.w("XmppEntityCapsCache: Could not set persistent cache", e);
        }
    }
    
    public static void emptyCache() {
        if (sCache != null) {
            sCache.emptyCache();
        }
    }
}
