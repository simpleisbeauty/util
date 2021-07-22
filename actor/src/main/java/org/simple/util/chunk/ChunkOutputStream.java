package org.simple.util.chunk;

import org.simple.concurrent.FunctionActor;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Phaser;
import java.util.function.Consumer;

import static org.simple.util.chunk.ChunkStore.Chunk;

public class ChunkOutputStream extends OutputStream {

    public ChunkOutputStream(int chunkSize, ExecutorService exec, Consumer<Chunk> dest) {

        this.chunkSize= chunkSize;
        phaser= new Phaser();
        actor= new FunctionActor<>(exec, new ChunkCompress(), new ChunkStore(phaser, dest));
        chunks= new ArrayList<>();
        chunk= new Chunk(sequence, chunkSize);
    }

    @Override
    public void write(int i) throws IOException {

        try {
            chunk.data[chunk.pos++] = (byte) i;
            length++;
            if (chunk.pos == chunkSize) { // full
                chunks.add(chunk);
                sequence++;
                phaser.register(); //  next actor call arrive() when complete
                actor.accept(chunk);
                chunk = new Chunk(sequence, chunkSize);
            }
        }
        catch(Exception e){
            throw new IOException("chunk outputsteam", e);
        }
    }

    public void EOF() throws IOException{
        status= 0;
    }

    public boolean isClosed(){
        return status == 1;
    }

    // auto close
    @Override
    public void close() throws IOException{
        if(status== -1) {
            throw new IOException("NOT EOF");
        }
        else if(status== 1){ //already closed
            return;
        }
        try {
            phaser.register();
            actor.accept(chunk); // final chunk
            actor.close();
            phaser.awaitAdvance(0); // wait all chunks written to dest
            for (Chunk c : chunks) {
                if (c.status != 1) {
                    throw new IOException("Chunk: " + c.sequence + "; status: " + c.status);
                }
            }
        }
        finally {
            status=1;
        }

    }

    public int getChunks(){
        return sequence+1;
    }

    protected Chunk chunk;
    protected volatile int sequence= 0;

    protected final int chunkSize;

    protected int length= 0;
    private FunctionActor<Chunk, Chunk> actor;
    private Phaser phaser;
    private ArrayList<Chunk> chunks;

    private int status= -1;
}
