package org.simple.example;

import org.simple.concurrent.FunctionActor;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Phaser;
import java.util.function.Function;

public class JoinInput{

    public JoinInput(ExecutorService exec) throws FileNotFoundException {
        phaser= new Phaser();

        actor= new FunctionActor<Integer, SplitInput.Chunk>(exec, new ReadChunkFile());
        chunkJoin= new ChunkJoin(new FileOutputStream("/tmp/1.jpg"), phaser);
        actor.append(new ChunkUnCompress()).end(chunkJoin);
    }

    public long read(int [] seqs) throws IOException {

        for (int i: seqs){
            phaser.register();
            actor.accept(i);
        }
        phaser.awaitAdvance(0);
        actor.close();
        return chunkJoin.getSize();
    }

    private FunctionActor<Integer, SplitInput.Chunk> actor;
    private ChunkJoin chunkJoin;
    private Phaser phaser;
}
