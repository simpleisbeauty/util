package org.simple.nio;


public interface NioChannelHandler {

    AbstractNioChannel newChannel(NioChannelContext context);
}
