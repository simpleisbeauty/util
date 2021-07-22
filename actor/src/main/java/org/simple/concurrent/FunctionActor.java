package org.simple.concurrent;

import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;
import java.util.function.Function;

public class FunctionActor<T,R> extends Actor<T> {

    public FunctionActor(ExecutorService exec, Function<T, R> f, Consumer<R> next){
        super(exec);
        fun=f;
        nextActor= next;
    }

    public FunctionActor(ExecutorService exec, Function<T, R> f){
        this(exec, f, null);
    }

    public <N> FunctionActor<R, N> append(Function<R, N> fun){
        FunctionActor<R, N> next= new FunctionActor<R, N>(this.exec, fun);
        this.nextActor= (Consumer<R>)  next;
        return next;
    }

    // next actor is Consumer = end of actor chain
    public void end(Consumer<R> next){
        this.nextActor= next;
    }

    public void process(T t){
        R rst= fun.apply(t);
        // allow function no output for specific input t
        if (rst != null) {
            nextActor.accept(rst);
        }
    }

    // must catch all Exception and set status inside func apply(), because run in ExecutorService
    private final Function<T,R> fun;
    private Consumer<R> nextActor;
}
