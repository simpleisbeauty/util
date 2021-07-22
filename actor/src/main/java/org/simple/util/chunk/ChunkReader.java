package org.simple.util.chunk;

import org.simple.concurrent.FunctionActor;

import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Phaser;
import java.util.function.Function;
import static org.simple.util.chunk.ChunkStore.Chunk;

public class ChunkReader {

    public ChunkReader(ExecutorService exec, int chunks, Function<Integer, Chunk> chunReader,
                       OutputStream dest)  {
        phaser= new Phaser();
        this.chunks= chunks;
        actor= new FunctionActor<Integer, Chunk>(exec, chunReader);
        chunkJoin= new ChunkJoin(dest, phaser);
        actor.append(new ChunkUnCompress()).end(chunkJoin);

    }

    public long read() throws IOException {

        for (int i=0; i< chunks; i++){
            phaser.register();
            actor.accept(i);
        }

        phaser.awaitAdvance(0);
        actor.close();
        return chunkJoin.getSize();
    }

    protected final int chunks;

    private final FunctionActor<Integer, Chunk> actor;
    private final ChunkJoin chunkJoin;
    private final Phaser phaser;

}
