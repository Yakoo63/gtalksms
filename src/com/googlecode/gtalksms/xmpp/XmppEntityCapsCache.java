package com.googlecode.gtalksms.xmpp;

import java.io.File;
import java.io.IOException;

import org.jivesoftware.smack.util.Base32Encoder;
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
        
        if (!cacheDir.exists())
            if (!cacheDir.mkdir())
                throw new IllegalStateException("Can not create entity caps cache dir");

        sCache = new SimpleDirectoryPersistentCache(cacheDir, Base32Encoder.getInstance());
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
