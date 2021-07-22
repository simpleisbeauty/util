package org.simple.util.chunk;

import org.simple.concurrent.FunctionActor;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;

public class ChunkInputStream extends InputStream implements Consumer<ChunkStore.Chunk> {

    public ChunkInputStream(ExecutorService exec, int chunks, Function<Integer, ChunkStore.Chunk> chunReader) {
        actor = new FunctionActor<Integer, ChunkStore.Chunk>(exec, chunReader);
        actor.append(new ChunkUnCompress()).end(this);
        count= new AtomicInteger(chunks);
        for (int i= 0; i< chunks; i++) {
            actor.accept(i);
        }
    }

    public void accept(ChunkStore.Chunk c) {

        inQue.offer(c);
        c.status = 1;
        count.decrementAndGet();
    }


    @Override
    public int read() throws IOException {

        try {
            while (chunk == null) {
                chunk = inQue.poll();
                if (chunk == null) {
                    if (count.get() == 0) {
                        return -1; // EOF
                    }
                } else {
                    pos = 0;
                }
            }

            // return byte as int
            int r = chunk.data[pos++] & 255;
            if (pos == chunk.data.length) {
                chunk.data = null;
                chunk = null; // gc and next chunk
            }
            return r;
        }
        catch (Exception e){
            throw new IOException("ChunkInputStream read:", e);
        }

    }

    private ConcurrentLinkedQueue<ChunkStore.Chunk> inQue = new ConcurrentLinkedQueue<ChunkStore.Chunk>();

    private ChunkStore.Chunk chunk; // current chunk read from inQue
    private int pos;

    private FunctionActor<Integer, ChunkStore.Chunk> actor;

    protected AtomicInteger count;

}
