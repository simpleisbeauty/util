package org.simple.example;

import org.simple.util.DynamicPool;
import org.simple.util.LastModified;
import org.simple.util.LatestConfig;

import java.io.Closeable;
import java.io.IOException;

public class DynamicPoolFoo extends DynamicPool<DynamicPoolFoo.FoolSession> {
    public DynamicPoolFoo() {
        super(new FoolConfig());
    }

    @Override
    protected FoolSession newObject() {
        return new FoolSession();
    }

    @Override
    protected boolean isClosed(FoolSession obj) {
        return false;
    }

    public static class FoolSession implements Closeable {

        @Override
        public void close() throws IOException {

        }
    }

    public static class FoolConfig extends LatestConfig<Config>{

        @Override
        protected LastModified<Config> refresh(long lastModified) {
            return poolConfig;
        }
    }

    public Config getPoolConfig(){
        return latestConf.getConfig();
    }
    // change config to verify auto config refreshing
    public void changeConfig(int min_conn, int max_conn, long max_busy_time, long max_idle_time){
        poolConfig= new LastModified<>(new Config(min_conn, max_conn, max_busy_time, max_idle_time),
                System.currentTimeMillis(), pollInterval);
    }
    public final static long pollInterval= 60000;
    private static LastModified<Config> poolConfig=
            new LastModified<>(new Config(1, 2, 20000, 40000),
            System.currentTimeMillis(), pollInterval);

}
