package com.ramlg.blockingcache;

import net.sf.ehcache.Ehcache;
import net.sf.ehcache.constructs.CacheDecoratorFactory;
import net.sf.ehcache.constructs.blocking.BlockingCache;

import java.util.Properties;

/**
 * Created by ramlalgoel on 16/06/2017.
 */
public class BlockingCacheDecoratorFactory extends CacheDecoratorFactory {
    @Override
    public Ehcache createDecoratedEhcache(Ehcache ehcache, Properties properties) {
        final BlockingCache blockingCache = new BlockingCache(ehcache);
        blockingCache.setTimeoutMillis(2000);
        return blockingCache;
    }

    @Override
    public Ehcache createDefaultDecoratedEhcache(Ehcache ehcache, Properties properties) {
        return this.createDecoratedEhcache(ehcache, properties);
    }
}
