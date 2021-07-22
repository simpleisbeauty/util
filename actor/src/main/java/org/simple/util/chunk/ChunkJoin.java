package org.simple.util.chunk;

import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.Phaser;
import java.util.function.Consumer;
import static org.simple.util.chunk.ChunkStore.Chunk;



public class ChunkJoin implements Consumer<Chunk> {

    public ChunkJoin(OutputStream osm, Phaser phaser) {
        this.phaser= phaser;
        this.osm = osm;
    }

    public void accept(Chunk c) {

        try {
            //data might be compressed, c.pos is uncompressed length
            osm.write(c.data);

            size= size+ c.data.length;
            c.status=1;
        }
        catch(Exception e){// must catch all Exception, because run in ExecutorService
            c.status=-1;
            e.printStackTrace();
        }
        phaser.arrive(); // must signal after status set
        c.data=null; // for GC to use less memory
    }

    public long getSize() throws IOException {
        osm.close();
        return size;
    }

    private final Phaser phaser;
    private final OutputStream osm;
    private long size=0;
}
