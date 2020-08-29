package org.simple.nio;

import java.io.IOException;
import java.net.InetSocketAddress;

public interface NioChannelContext {

    InetSocketAddress getRemoteAddress();
    void write(byte[] out);
    void flush();
    boolean isOpen();
    void close() throws IOException;
}
