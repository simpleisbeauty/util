package org.simple.util;

public class LastModified<T> {

    public LastModified(T t, long lastModified, long pollInterval){
        this.obj= t;
        this.lastModified= lastModified;
        this.pollInterval= pollInterval;
    }

    public long lastModified;
    public T obj;
    //milliseconds
    public volatile long pollInterval;

}
