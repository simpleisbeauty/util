package org.simple.example;

import org.junit.Test;

import java.io.FileInputStream;
import static org.junit.Assert.*;

import java.io.FileOutputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SplitInputTest {
    @Test
    public void SplitTest() {
        try(FileInputStream fins= new FileInputStream("/home/johnny/test.jpg"); ){

            byte[] buf= new byte[8192];
            int chunkSize= 1024*1024*2;
            ExecutorService exec= Executors.newCachedThreadPool();
            SplitInput split= new SplitInput(chunkSize, exec, "/tmp/c");

            int len;
            int fileSize=0;
            while((len=fins.read(buf))!= -1){
                fileSize= fileSize+ len;

                    byte[] bf = new byte[len];
                    System.arraycopy(buf, 0, bf, 0, len);
                    split.write(bf);

            }
            split.close();
            System.out.println("fileSize:"+ fileSize);

            int[] seqs= new int[split.getChunks()];
            for(int i=0; i< seqs.length; i++){
                seqs[i]=i;
            }
            JoinInput jit= new JoinInput(exec);
            assertEquals(jit.read(seqs), fileSize);

            ChunkInputStream cins= new ChunkInputStream(exec, seqs);
            FileOutputStream osm= new FileOutputStream("/tmp/2.jpg");
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
}
