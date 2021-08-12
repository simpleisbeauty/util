package org.simple.nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.Channel;

public abstract class NioChannel implements Channel {

    public NioChannel(NioChannelContext context){
        this.context= context;
    }

    public void write(byte[] out){
        context.write(out);
    }

    public abstract void read(byte[] in);

    public void flush(){
        context.flush();
    }

    @Override
    public boolean isOpen(){ return context.isOpen();}

    @Override
    public void close() throws IOException {
        context.close();
    }

    public InetSocketAddress getRemoteAddress(){

        return context.getRemoteAddress();
    }

    protected final NioChannelContext context;

}
