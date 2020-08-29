package org.simple.nio;

import java.io.IOException;
import java.net.InetSocketAddress;

public abstract class AbstractNioChannel implements NioChannel{

    public AbstractNioChannel(NioChannelContext context){
        this.context= context;
    }

    public void write(byte[] out){

        context.write(out);
    }

    public void flush(){
        context.flush();
    }

    public boolean isOpen(){ return context.isOpen();}

    public void close() throws IOException {
        context.close();
    }

    public InetSocketAddress getRemoteAddress(){

        return context.getRemoteAddress();
    }

    protected final NioChannelContext context;

}
