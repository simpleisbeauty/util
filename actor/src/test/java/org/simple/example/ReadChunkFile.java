package org.simple.example;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.function.Function;
import static org.simple.util.chunk.ChunkStore.Chunk;

public class ReadChunkFile implements Function<Integer, Chunk> {

    public Chunk apply(Integer sequence) {

        Chunk chunk= new Chunk(sequence.intValue(), 0);;

        try {
            chunk.data= Files.readAllBytes(Paths.get("/tmp/c"+ sequence+ ".gz"));
            chunk.pos= chunk.data.length;
            //compressed data
            chunk.status=0;
        }
        catch(Exception ioe){
            chunk.status=-1;
            ioe.printStackTrace();
        }
        return chunk;
    }

}
