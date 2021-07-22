package org.simple.util.chunk;

import java.util.Date;
import java.util.UUID;
import java.util.concurrent.Phaser;
import java.util.function.Consumer;


public class ChunkStore implements Consumer<ChunkStore.Chunk> {

    public ChunkStore(Phaser phaser,  Consumer<Chunk> dest) {
        this.phaser = phaser;
        this.dest= dest;
    }

    public void accept(Chunk c) {

        try {
            dest.accept(c);
            //compressed data
            c.status=1;
        }
        catch(Exception e){ // must catch all Exception, because run in ExecutorService
            c.status=-1;
            e.printStackTrace();
        }
        phaser.arrive(); // must signal after status set
        c.data=null; // for GC to use less memory
    }

    public static class Chunk {
        public Chunk(int sequence, int chunkSize) {
            this.sequence = sequence;
            this.data = new byte[chunkSize];
        }

        public int sequence;
        public byte[] data;
        public volatile int pos = 0;
        public volatile int status=0;
    }


    private final Phaser phaser;
    private  Consumer<Chunk> dest;
}
