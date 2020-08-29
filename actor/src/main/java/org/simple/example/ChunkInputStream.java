package org.simple.example;

import org.simple.concurrent.FunctionActor;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class ChunkInputStream extends InputStream implements Consumer<SplitInput.Chunk> {

    public ChunkInputStream(ExecutorService exec, int[] seqs) {
        actor = new FunctionActor<Integer, SplitInput.Chunk>(exec, new ReadChunkFile());
        actor.append(new ChunkUnCompress()).end(this);
        count= new AtomicInteger(seqs.length);
        for (int i : seqs) {
            actor.accept(i);
        }
    }

    public void accept(SplitInput.Chunk c) {

        inQue.offer(c);
        c.status = 1;
        count.decrementAndGet();
    }


    @Override
    public int read() throws IOException {

        while (chunk == null) {
            chunk = inQue.poll();
            if (chunk == null) {
                if (count.get()== 0) {
                    return -1; // EOF
                }
            }
            else {
                pos = 0;
            }
        }

        int r= chunk.data[pos++] & 255;
        if (pos== chunk.data.length){
            chunk.data=null;
            chunk=null; // gc and next chunk
        }
        return r;

    }

    private ConcurrentLinkedQueue<SplitInput.Chunk> inQue = new ConcurrentLinkedQueue<SplitInput.Chunk>();

    private SplitInput.Chunk chunk; // current chunk read from inQue
    private int pos;

    private FunctionActor<Integer, SplitInput.Chunk> actor;

    private AtomicInteger count;
}
