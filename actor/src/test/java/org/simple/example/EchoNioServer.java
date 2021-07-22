package org.simple.example;

import org.simple.nio.AbstractNioChannel;
import org.simple.nio.NioChannelContext;
import org.simple.nio.NioChannelHandler;
import org.simple.nio.NioSelect;
import org.simple.concurrent.ConsumerActor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class EchoNioServer implements NioChannelHandler{

    public void listen(NioSelect nio, int[] ports) throws IOException{
        for (int p: ports){
            int key= nio.listen(null, p, this);
            System.out.println("Echo Server listen on: " + p+ "; handle key:" + key);
        }
    }

    public AbstractNioChannel newChannel(NioChannelContext context) {
        EchoNioChannel ch= new EchoNioChannel(context);
        channels.add(ch);
        return ch;
    }

    private class EchoNioChannel extends AbstractNioChannel{

        public EchoNioChannel(NioChannelContext context) {

            super(context);
            echo= new ConsumerActor<byte[]>(exec, in -> {
                System.out.println("Server got " + getRemoteAddress() + ": " + new String(in));
                write(in); // ensure sequence in queue, because in might be partial message
                flush();
            });
        }

        public void read(byte[] in) {

            /*CompletableFuture.supplyAsync(()->{
                System.out.println("Server got "+ getRemoteAddress()+ ": "+ new String(in));
                return in;
            }).thenAccept(bts -> {write(bts); flush();});*/

            echo.accept(in);
        }

        private final ConsumerActor<byte[]> echo;
    }

    public static void main(String[] args){

        try {
            NioSelect nio= new NioSelect();
            EchoNioServer srv = new EchoNioServer();
            srv.listen(nio, new int[]{9000, 9001});
            Thread s= new Thread(nio);
            s.start();

        }catch (IOException e){
            e.printStackTrace();
        }
    }

    private ArrayList<EchoNioChannel> channels= new ArrayList<EchoNioChannel>();
    private static ExecutorService exec= Executors.newCachedThreadPool();
}
