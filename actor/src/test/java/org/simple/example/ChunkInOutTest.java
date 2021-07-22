package org.simple.example;

import org.junit.Test;
import org.simple.util.chunk.ChunkInputStream;
import org.simple.util.chunk.ChunkOutputStream;
import org.simple.util.chunk.ChunkReader;
import org.simple.util.chunk.ChunkStore;

import java.io.FileInputStream;
import static org.junit.Assert.*;

import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;


public class ChunkInOutTest {
    @Test
    public void SplitTest() {
        try(FileInputStream fins= new FileInputStream("/home/johnny/Desktop/VID_20210102_210805.mp4"); ){
            UUID chunkId= UUID.randomUUID();

            byte[] buf= new byte[8192];
            int chunkSize= 1024*1024*2;
            ExecutorService exec= ForkJoinPool.commonPool();


            ChunkOutputStream cos= new ChunkOutputStream(chunkSize, exec, c -> {
                try (FileOutputStream fos = new FileOutputStream(getChunkName(chunkId, c.sequence))) {
                    //compressed data
                    fos.write(c.data);
                    fos.flush();
                } catch (Exception e) { // must catch all Exception, because run in ExecutorService
                    throw new RuntimeException(c.sequence + "; split pos:" + c.pos, e);
                }
            });

            int len;
            int fileSize=0;
            while((len=fins.read(buf))!= -1){
                fileSize= fileSize+ len;

                    //byte[] bf = new byte[len];
                    //System.arraycopy(buf, 0, bf, 0, len);
                    cos.write(buf, 0, len);
                    //cos.write(bf);

            }
            int chunks= cos.getChunks();
            cos.close();
            System.out.println("fileSize:"+ fileSize);


            ChunkReader jit= new ChunkReader(exec, chunks, seq ->{

                ChunkStore.Chunk chunk= new ChunkStore.Chunk(seq.intValue(), 0);;

                try {
                    chunk.data= Files.readAllBytes(Paths.get(getChunkName(chunkId, seq)));
                    chunk.pos= chunk.data.length;
                    //compressed data
                    chunk.status=0;
                }
                catch(Exception ioe){
                    chunk.status=-1;
                    ioe.printStackTrace();
                }
                return chunk;
            }, new FileOutputStream("/tmp/1.png"));
            assertEquals(jit.read(), fileSize);

            ChunkInputStream cins= new ChunkInputStream(exec, chunks, seq ->
            {
                ChunkStore.Chunk chunk= new ChunkStore.Chunk(seq.intValue(), 0);;

                try {
                    chunk.data= Files.readAllBytes(Paths.get(getChunkName(chunkId, seq)));
                    chunk.pos= chunk.data.length;
                    //compressed data
                    chunk.status=0;
                }
                catch(Exception ioe){
                    chunk.status=-1;
                    ioe.printStackTrace();
                }
                return chunk;
            });
            FileOutputStream osm= new FileOutputStream("/tmp/2.png");
            int size=0;

            while((len= cins.read(buf))!= -1){
                osm.write(buf, 0, len);
                size= size+ len;
            }
            osm.close();
            assertEquals(fileSize, size);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    final static private String getChunkName(UUID chunkId, Integer seq){
        return "/tmp/c_"+ chunkId+ '_'+ seq+ ".gz";
    }
}
