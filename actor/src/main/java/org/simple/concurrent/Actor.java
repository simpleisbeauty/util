package org.simple.concurrent;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class Actor<T> implements Consumer<T>, AutoCloseable {

    public Actor(ExecutorService ex) {
        exec = ex;
        inQue = new ConcurrentLinkedQueue<T>();
    }

    // o is not duplicated, can't be shared
    public void accept(T o) {

        inQue.offer(o);
        syncRun();
    }

    private void syncRun() {
        // assure single thread running
        if (running.compareAndSet(false, true)) {
            exec.submit(() -> {
                T t;
                while ((t = inQue.poll()) != null) {
                    process(t);
                }
                running.set(false);
            });
        }
    }

    @Override
    public void close() {
        // loop until actor not running and empty in Queue
        while (running.get()== true || !inQue.isEmpty()) {
            try {
                syncRun();
                Thread.sleep(5);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // must catch all Exception inside, because run in ExecutorService
    public abstract void process(T t);

    private volatile AtomicBoolean running = new AtomicBoolean(false);

    protected final ExecutorService exec;
    private final ConcurrentLinkedQueue<T> inQue;

}
