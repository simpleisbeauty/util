package org.simple.example;

import org.simple.concurrent.ConsumerActor;
import org.simple.concurrent.FunctionActor;
import org.simple.nio.NioChannel;
import org.simple.nio.NioChannelContext;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.ExecutorService;

public class EchoClientChannel extends NioChannel {

    public EchoClientChannel(ExecutorService exec, NioChannelContext context) {
        super(context);

        func= new FunctionActor<byte[], String>( exec, in -> {
            try {
                echo.write(in);
                echo.flush();
            }catch(IOException e){
                e.printStackTrace();
            }
            return "Client "+ this.hashCode()+ " got "+ new String(in)+ "from:" + getRemoteAddress();
        });
        // function chain to buffer and transform
        System.out.println("echo client channel init");
        func.append(in -> {return "dummy pass:"+ in;}).end(new ConsumerActor<String>(exec, s -> System.out.println(s)));
    }

    @Override
    public void read(byte[] in) {
        func.accept(in);
    }

    @Override
    public void flush(){
        super.flush();
        func.close();
    }

    public ByteArrayOutputStream echo= new ByteArrayOutputStream();

    private final FunctionActor<byte[], String> func;
}
