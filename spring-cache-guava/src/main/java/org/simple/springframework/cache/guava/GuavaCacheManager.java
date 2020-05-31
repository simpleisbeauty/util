package org.simple.springframework.cache.guava;

import com.google.common.cache.CacheBuilder;
import org.springframework.cache.Cache;
import org.springframework.cache.support.AbstractCacheManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class GuavaCacheManager extends AbstractCacheManager{

    public GuavaCacheManager(List<GuavaCacheConfig> guavaConfigs){
        this.guavaConfigs= guavaConfigs;
    }

    @Override
    protected Collection<? extends Cache> loadCaches() {
        ArrayList<GuavaCache> gcaLst= new ArrayList<GuavaCache>(guavaConfigs.size());
        for(GuavaCacheConfig config : guavaConfigs){
            gcaLst.add(new GuavaCache( config.getName(), CacheBuilder.newBuilder()
                    .maximumSize(config.getMaxItemSize())
                    .expireAfterAccess(config.getExpiration(), TimeUnit.MINUTES).build()));
        }
        return gcaLst;
    }
    /**
     * Guava Cache Configuration
     */
    public static class GuavaCacheConfig {

        public GuavaCacheConfig(String name, int maxItemSize, int expiration){
            this.name= name;
            this.expiration= expiration;
            this.maxItemSize=maxItemSize;
        }

        public int getMaxItemSize() {
            return maxItemSize;
        }

        public int getExpiration() {
            return expiration;
        }

        public String getName() { return name; }

        private final int maxItemSize;
        private final int expiration;
        private final String name;
    }
    private final List<GuavaCacheConfig> guavaConfigs;
}
