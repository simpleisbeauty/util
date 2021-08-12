package org.simple.example;

import org.junit.Test;
import static org.junit.Assert.*;
import org.simple.util.DynamicPool;


public class DynamicPoolTest {

    @Test
    public void poolDeadlock() throws InterruptedException {

        DynamicPoolFoo df= new DynamicPoolFoo();
        assertTrue(df.getPoolConfig().min_conn==1);
        final int run_times=12;
        DynamicPool.PooledSession[] ps= new DynamicPool.PooledSession[run_times];
        for(int i=0; i< run_times; i++) {
            final int ii= i;
            new Thread(()->
                    ps[ii] = df.get()).start();
        }
        df.changeConfig(2, 4, 40000, 70000);
        for(int i=0; i< run_times; i++) {
            final int ii= i;
            new Thread(()->{
                try {
                    ps[ii].conn.close();
                }
                catch (Exception e){
                }}).start();
        }
        assertTrue(df.getPoolConfig().max_conn==2);
        Thread.sleep(df.pollInterval);

        assertTrue(df.getPoolConfig().max_conn==4);
    }
}
