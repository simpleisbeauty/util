package org.simple.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public abstract class DynamicPool<T extends Closeable> implements Closeable {

    public DynamicPool(LatestConfig<Config> latestConf) {
        this.latestConf = latestConf;

        lock = new ReentrantLock();
        available = lock.newCondition();
        availConn = new ArrayList<>();
        usedConn = new ArrayList<>();
        logger = LoggerFactory.getLogger(getClass());
        this.max_conn_times= 2;
    }

    abstract protected T newObject();

    abstract protected boolean isClosed(T obj);

    public void put(PooledSession pooled) {
        if (pooled == null)
            return;

        lock.lock();
        try {
            // need to prevent return same pooled multiple times by mistake
            if (!availConn.contains(pooled)) {
                availConn.add(pooled);
                usedConn.remove(pooled);
                pooled.timestamp = System.currentTimeMillis(); // new time in availConn
                available.signal();
            }
        } finally {
            lock.unlock();
        }
    }

    public PooledSession get() {
        PooledSession x = null;
        lock.lock();
        Config lcf = latestConf.getConfig(); // latest refreshed config
        long now = System.currentTimeMillis();
        try {
            while (x == null && availConn.size() > 0) {
                x = availConn.remove(0); // longest idle time
                if (!isClosed(x.conn)) {
                    // auto shrink pool if idle time >= max_idle_time
                    if ((now - x.timestamp) < lcf.max_idle_time &&
                            availConn.size() < lcf.min_conn) {
                        break;
                    } else { // discard
                        try {
                            x.conn.close(); // can't calll x.close() - put it back to avail queue again
                            x = null; // loop to get next avail
                        } catch (Exception e) {
                            logger.error("unexpected close idle connection", e);
                        }
                    }
                }// discard all closed conn
            }
            if (x == null) { // no available, check used
                int usedSize = usedConn.size();
                boolean newIntance= true;
                if (usedSize >= lcf.max_conn) { // pool maxed
                    int oldestIdx = usedSize - 1;
                    // check max_busy_time to prevent deadlock - tasks hold all the conn, and wait for each other to release
                    if (usedSize > lcf.max_conn * max_conn_times || now - usedConn.get(oldestIdx).timestamp > lcf.max_busy_time) {
                        logger.warn(lcf.max_conn + " max conn; move oldest conn from used pool back to available, current conn:"
                                + usedSize);
                        availConn.add(usedConn.remove(oldestIdx));
                        newIntance= false;
                        available.signal(); // signal deadlock waiter
                    } else {
                        logger.warn(lcf.max_conn + " max conn reached, however no busy and reach "+ max_conn_times+ " times of max conn, new instance.  " + usedSize);
                    }
                }
                if(newIntance){
                    try{
                        x = new PooledSession(newObject()); // new timestamp
                    }
                    catch (Exception e){
                        logger.error("Fatal, system create new conn failed, please fix it ASAP");
                    }
                }
                if(x== null) {
                    logger.debug(" pool maxed, awaiting... for conn to release");
                    while(availConn.size()==0) {
                        available.await();
                    }
                    x = availConn.remove(0);
                    logger.debug("got conn after await");
                }
            }
            usedConn.add(x);
            x.timestamp = now;
        }
        catch(InterruptedException ie){
            throw new RuntimeException("pool await", ie);
        } finally{
            lock.unlock();
        }
        return x;
    }

    @Override
    public void close() throws IOException {
        for (PooledSession p : availConn) {
            p.conn.close();
        }
        for (PooledSession p : usedConn) {
            p.conn.close();
        }
    }


    public class PooledSession implements Closeable {

        public PooledSession(T conn) {
            this.conn = conn;
        }

        @Override
        public void close() {
            put(this);
        }

        protected long timestamp;
        public final T conn;
    }

    public static class Config {

        public Config(int min_conn, int max_conn, long max_busy_time, long max_idle_time) {
            this.min_conn = min_conn;
            this.max_conn = max_conn;
            this.max_idle_time = max_idle_time;
            this.max_busy_time = max_busy_time;
        }

        public final int min_conn, max_conn;
        public final long max_idle_time, max_busy_time;

    }

    protected final LatestConfig<Config> latestConf;

    private final Lock lock;
    private final Condition available;

    // since locked, used as queue
    private volatile ArrayList<PooledSession> availConn, usedConn;
    private final Logger logger;
    public final int max_conn_times;
}
