package org.simple.nio;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class NioSelect implements Runnable, AutoCloseable {

    public NioSelect() throws IOException {
        this(4096);
    }

    public NioSelect(int bufSize) throws IOException {

        readBuff= ByteBuffer.allocate(bufSize);
        select = Selector.open();
        servers= new ConcurrentHashMap<Integer, NioChannelHandler>();
    }

    public void run() {
        while (loop) {
            try {
                // Wait for an event one of the registered channels
                wait = true;
                while (!wakeup2do.isEmpty()) {
                    wakeup2do.poll().run();
                }

                if (select.select(60000) == 0) continue;
                wait = false;

                if (Thread.interrupted()) {
                    break;
                }

                // Iterate over the set of keys for which events are available
                Iterator selectedKeys = select.selectedKeys().iterator();
                while (selectedKeys.hasNext()) {
                    SelectionKey key = (SelectionKey) selectedKeys.next();
                    selectedKeys.remove();

                    if (!key.isValid()) {
                        continue;
                    }

                    // Check what event is available and deal with it
                    if (key.isAcceptable()) {
                        accept(key);
                    } else if (key.isReadable()) {
                        read(key);
                    } else if (key.isWritable()) {
                        write(key);
                    } else if (key.isConnectable()) {
                        finishConnection(key);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void close() throws Exception {
        loop = false;
        servers.clear();
    }

    public int listen(InetAddress addr, int port, NioChannelHandler handler) throws IOException {

        ServerSocketChannel sch = ServerSocketChannel.open();
        sch.configureBlocking(false);
        sch.socket().bind(new InetSocketAddress(addr, port));
        register(sch, SelectionKey.OP_ACCEPT, null);
        int key= sch.hashCode();
        servers.put(key, handler);
        return key;
    }

    public AbstractNioChannel connect(InetAddress addr, int port, NioChannelHandler connHandler) throws IOException {
        SocketChannel sch = SocketChannel.open();
        sch.configureBlocking(false);
        sch.connect(new InetSocketAddress(addr, port));
        MyChannelContext chContext = new MyChannelContext(sch);
        AbstractNioChannel nch = connHandler.newChannel(chContext);
       register(sch, SelectionKey.OP_CONNECT, nch);

        return nch;
    }

    private void register(SelectableChannel sch, int interestOps, Object attach) {
        wakeup2do.offer(() -> {
            try {
                sch.register(select, interestOps, attach);
            } catch (ClosedChannelException e) {
                e.printStackTrace();
            }
        });
        if (wait) select.wakeup();
    }

    private void accept(SelectionKey key) throws IOException {
        // For an accept to be pending the channel must be a server socket channel.
        ServerSocketChannel srvSocketCh = (ServerSocketChannel) key.channel();

        // Accept the connection and make it non-blocking
        SocketChannel sch = srvSocketCh.accept();
        sch.configureBlocking(false);
        System.out.println("accepted new client from:" + sch.getRemoteAddress());
        System.out.println("local address:" + sch.getLocalAddress());
        MyChannelContext chContext = new MyChannelContext(sch);

        AbstractNioChannel nch = servers.get(srvSocketCh.hashCode()).newChannel(chContext);
        sch.register(select, SelectionKey.OP_READ, nch);
    }

    private void read(SelectionKey key) throws IOException {

        SocketChannel ch = (SocketChannel) key.channel();
        AbstractNioChannel nch = (AbstractNioChannel) key.attachment();
        int numRead;

        try {
            while(true) {
                readBuff.clear();
                numRead = ch.read(readBuff);
                if(numRead == 0) {
                    return;
                }
                else if(numRead == -1){
                    break;
                }
                else{
                    byte[] arry = new byte[numRead];
                    readBuff.flip();
                    readBuff.get(arry);
                    //System.arraycopy(readBuff.array(), 0, arry, 0, numRead);
                    nch.read(arry); // call back read
                    if(numRead < readBuff.capacity()){
                        nch.flush();
                        break; // no more to read from channel
                    }
                    else{
                        System.out.println("numRead:"+ numRead+ ", capacity:"+ readBuff.capacity());
                    }

                }
            }

        } catch (IOException e) {
            // The remote closed the connection, cancel
            // the selection key and close the channel.
            e.printStackTrace();
            numRead= -1;
        }
        if (numRead == -1) {
            // close the socket channel for remote socket already shutdown.
            ch.close();
            key.cancel();
        }
    }

    private void write(SelectionKey key) throws IOException {

        SocketChannel socketChannel = (SocketChannel) key.channel();
        AbstractNioChannel nch = (AbstractNioChannel) key.attachment();

        ConcurrentLinkedQueue<ByteBuffer> que = ((MyChannelContext) nch.context).outQueue;

        while (!que.isEmpty()) {
            ByteBuffer ot = que.peek();
            socketChannel.write(ot);
            if (ot.remaining() > 0) {
                // socket's buffer full, break and keep key in OP_WRITE
                break;
            }
            que.poll();
        }

        if (que.isEmpty()) {
            // write all data, switched to interested read mode
            key.interestOps(SelectionKey.OP_READ);
        }

    }

    private void finishConnection(SelectionKey key) throws IOException {

        SocketChannel ch = (SocketChannel) key.channel();
        try {
            ch.finishConnect();
            // Write mode after connection success
            key.interestOps(SelectionKey.OP_WRITE);
        } catch (IOException e) {
            e.printStackTrace();
            key.cancel(); // cancel the connection
        }

    }

    private class MyChannelContext implements NioChannelContext {

        MyChannelContext(SocketChannel sch) {
            this.sch = sch;
        }

        public InetSocketAddress getRemoteAddress(){
            try {
                return (InetSocketAddress) sch.getRemoteAddress();
            }catch (IOException e){
                return null;
            }
        }

        public void flush() {

            if(outQueue.size()> 0) {
                wakeup2do.offer(() -> {
                    SelectionKey key = sch.keyFor(select);
                    int cur_ops = key.interestOps();
                    if (cur_ops != SelectionKey.OP_CONNECT) {
                        key.interestOps(SelectionKey.OP_WRITE);
                    }
                });
            }
            if (wait) {
                select.wakeup(); // wake up Selector thread to handle it
            }
        }

        public void write(byte[] out) {
            outQueue.offer(ByteBuffer.wrap(out));
        }

        public boolean isOpen(){return sch.isOpen();}

        public void close() throws IOException {
            sch.close();
        }

        private final SocketChannel sch;
        private ConcurrentLinkedQueue<ByteBuffer> outQueue = new ConcurrentLinkedQueue<ByteBuffer>();

    }

    private Selector select;
    private transient volatile boolean loop = true;
    private transient volatile boolean wait = false;

    private final ConcurrentHashMap<Integer, NioChannelHandler> servers;

    private ByteBuffer readBuff;
    private ConcurrentLinkedQueue<Runnable> wakeup2do = new ConcurrentLinkedQueue<Runnable>();
}
