package com.googlecode.gtalksms.xmpp;

import java.io.File;
import java.io.IOException;

import org.jivesoftware.smackx.entitycaps.Base32Encoder;
import org.jivesoftware.smackx.entitycaps.EntityCapsManager;
import org.jivesoftware.smackx.entitycaps.EntityCapsPersistentCache;
import org.jivesoftware.smackx.entitycaps.SimpleDirectoryPersistentCache;

import com.googlecode.gtalksms.Log;

import android.content.Context;

public class XmppEntityCapsCache {
    private static final String CACHE_DIR = "EntityCapsCacheBase32";
    private static EntityCapsPersistentCache sCache;
    
    public static void enableEntityCapsCache(Context ctx) {
        File cacheDir = new File(ctx.getFilesDir(), CACHE_DIR);
        
        if (!cacheDir.exists())
            if (!cacheDir.mkdir())
                throw new IllegalStateException("Can not create entity caps cache dir");

        sCache = new SimpleDirectoryPersistentCache(cacheDir, new Base32Encoder());
        try {
            EntityCapsManager.setPersistentCache(sCache);
        } catch (IOException e) {
            Log.w("XmppEntityCapsCache: Could not set persistent cache", e);
        }
    }
    
    public static void emptyCache() {
        if (sCache != null)
            sCache.emptyCache();
    }
}
