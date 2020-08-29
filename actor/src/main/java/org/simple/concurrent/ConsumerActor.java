package org.simple.concurrent;

import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

public class ConsumerActor<T> extends Actor<T>{

    public ConsumerActor(ExecutorService exec, Consumer<T> c){
        super(exec);
        consume=c;
    }

    public void process(T t){
        consume.accept(t);
    }

    // must catch all Exception and set status inside consume accept(), because run in ExecutorService
    private final Consumer<T> consume;
}
