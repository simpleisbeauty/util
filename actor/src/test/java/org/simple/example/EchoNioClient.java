package org.simple.example;

import org.simple.nio.AbstractNioChannel;
import org.simple.nio.NioChannelContext;
import org.simple.nio.NioChannelHandler;
import org.simple.concurrent.ConsumerActor;
import org.simple.concurrent.FunctionActor;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class EchoNioClient  implements NioChannelHandler{

    public AbstractNioChannel newChannel(NioChannelContext context) {
        return new EchoNioClientChannel(context);
    }

    public class EchoNioClientChannel extends AbstractNioChannel {

        public EchoNioClientChannel(NioChannelContext context) {
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
            // dummy pass to test function chain
            func.append(in -> {return "dummy pass:"+ in;}).end(new ConsumerActor<String>(exec, s -> System.out.println(s)));
        }

        @Override
        public void read(byte[] in) {

            /*System.out.println("Client "+ this.hashCode()+ " got "+ new String(in) + "from:" + getRemoteAddress());
            try {
                echo.write(in);
            }catch(IOException e){
                e.printStackTrace();
            }*/

            func.accept(in);
        }

        public void flush(){
            super.flush();
            func.close();
        }

        public ByteArrayOutputStream echo= new ByteArrayOutputStream();

        private final FunctionActor<byte[], String> func;
    }

    private static ExecutorService exec= Executors.newCachedThreadPool();
}
