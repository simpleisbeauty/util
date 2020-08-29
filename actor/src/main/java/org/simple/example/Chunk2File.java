package org.simple.example;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.Phaser;
import java.util.function.Consumer;


public class Chunk2File implements Consumer<SplitInput.Chunk> {

    public Chunk2File(Phaser phaser, String key) {
        this.phaser = phaser;
        this.key= key;
    }

    public void accept(SplitInput.Chunk c) {

        try (FileOutputStream fos= new FileOutputStream(this.key+c.sequence+ ".gz")){
            System.out.println(c.sequence+ "; split pos:"+ c.pos);
            //compressed data
            fos.write(c.data);
            fos.flush();
            c.status=1;
        }
        catch(Exception e){ // must catch all Exception, because run in ExecutorService
            c.status=-1;
            e.printStackTrace();
        }
        phaser.arrive(); // must signal after status set
        c.data=null; // for GC to use less memory
    }

    private final Phaser phaser;
    private final String key;
}
