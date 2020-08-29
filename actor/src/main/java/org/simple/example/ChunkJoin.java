package org.simple.example;

import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.Phaser;
import java.util.function.Consumer;


public class ChunkJoin implements Consumer<SplitInput.Chunk> {

    public ChunkJoin(OutputStream osm, Phaser phaser) {
        this.phaser= phaser;
        this.osm = osm;
    }

    public void accept(SplitInput.Chunk c) {

        try {
            System.out.println(c.sequence+ "; pos:"+ c.pos);
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
