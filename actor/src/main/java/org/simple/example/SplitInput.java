package org.simple.example;

import org.simple.concurrent.FunctionActor;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Phaser;

import java.util.function.Function;

public class SplitInput implements Function<byte[], SplitInput.Chunk>, AutoCloseable {

    // key = dir or storage key
    public SplitInput(int chunkSize, ExecutorService exec, String key) {
        this.chunkSize = chunkSize;
        phaser= new Phaser();
        chunk = new Chunk(0, this.chunkSize);
        chunks= new ArrayList<Chunk>();

        actor= new FunctionActor<byte[], Chunk>(exec, this);
        actor.append(new ChunkCompress()).end(new Chunk2File(phaser, key));

    }

    public void write(byte[] in){
        actor.accept(in);
    }

    @Override
    public void close(){
        actor.accept(new byte[0]);
        actor.close();

        phaser.awaitAdvance(0);
        for(Chunk c: chunks){
            if (c.status!= 1){
                throw new IllegalStateException("Chunk: "+ c.sequence+ "; status: "+ c.status);
            }
        }
    }

    public Chunk apply(byte[] in) {

        Chunk fullChunk = null;
        // EOF
        if (in.length==0 ){
            phaser.register();
            chunks.add(chunk);
            fullChunk= chunk;
        }
        else {
            if (chunkSize < in.length) {
                throw new IllegalArgumentException("input data size larger than chunk size");
            }

            int remain = chunk.fill(in, 0);
            if (chunk.full()) {
                totalSequence++;
                phaser.register(); //  next actor call arrive() when complete
                fullChunk = chunk;
                chunks.add(chunk);
                chunk = new Chunk(totalSequence, chunkSize);
            }
            if (remain > 0) { // must be full
                System.out.println(chunk.fill(in, in.length - remain));
                // remain should be 0
            }
        }

        return fullChunk;
    }

    public static class Chunk {
        public Chunk(int sequence, int chunkSize) {
            this.sequence = sequence;
            this.data = new byte[chunkSize];
        }

        public int fill(byte[] content, int offset) {
            int remain=0;

            int len= this.data.length - this.pos;
            int contentLen= content.length - offset;
            if (contentLen > len){
                System.arraycopy(content, offset, this.data, this.pos, len);
                remain= contentLen- len;
                pos= data.length;
            }
            else {
                System.arraycopy(content, offset, this.data, this.pos, contentLen);
                this.pos= this.pos+ contentLen;
            }

            return remain;
        }



        public boolean full() {
            return pos == data.length;
        }

        protected int sequence;
        protected byte[] data;
        protected volatile int pos = 0;
        protected volatile int status=0;
    }

    public int getChunks(){
        return totalSequence+1;
    }

    private final int chunkSize;
    private volatile int totalSequence = 0;
    private volatile Chunk chunk;

    private ArrayList<Chunk> chunks;

    private FunctionActor<byte[], Chunk> actor;
    private Phaser phaser;

}
