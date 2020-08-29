package org.simple.nio;

import java.net.InetSocketAddress;
import java.nio.channels.Channel;

public interface NioChannel extends Channel{

	void read(byte[] in);
	
	void write(byte[] out);

	void flush();

	InetSocketAddress getRemoteAddress();

}
