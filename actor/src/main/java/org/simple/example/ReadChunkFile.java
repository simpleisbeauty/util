package org.simple.example;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.function.Function;

public class ReadChunkFile implements Function<Integer, SplitInput.Chunk> {

    public SplitInput.Chunk apply(Integer sequence) {

        SplitInput.Chunk chunk= new SplitInput.Chunk(sequence.intValue(), 0);;

        try {
            chunk.data= Files.readAllBytes(Paths.get("/tmp/c"+ sequence+ ".gz"));
            chunk.pos= chunk.data.length;
            System.out.println(sequence+ "; len:"+  chunk.pos);
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
