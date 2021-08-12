package org.simple.nio;


public interface NioChannelHandler {

    NioChannel newChannel(NioChannelContext context);
}
