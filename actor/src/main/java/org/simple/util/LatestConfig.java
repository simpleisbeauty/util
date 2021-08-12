package org.simple.util;

import org.simple.util.LastModified;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicBoolean;

public abstract class LatestConfig<T> {

    public LatestConfig(){
        this.config= refresh(0);
        if(config== null){
            throw new IllegalArgumentException("init LatestConfig");
        }
        logger= LoggerFactory.getLogger(getClass());
    }

    // thread safe
    public T getConfig(){

        if(lock.compareAndSet(false, true)) {
            // check local refresh interval - can be changed on server config
            // default 0
            if(System.currentTimeMillis()- config.lastModified> config.pollInterval) {
                // server side check refreshed or not
                LastModified<T> t = refresh(config.lastModified);
                if (t != null) {
                    config = t; // volatile good enough?
                    logger.info("config refreshed every "+ config.pollInterval+ " at: "+ config.lastModified);
                }
            }
            lock.compareAndSet(true, false);
        }
        return config.obj;
    }

    protected abstract LastModified<T> refresh(long lastModified);

    protected  volatile LastModified<T> config;

    private AtomicBoolean lock = new AtomicBoolean(false);
    private Logger logger;
}
