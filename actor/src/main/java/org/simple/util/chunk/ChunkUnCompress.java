package org.simple.util.chunk;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.function.Function;
import java.util.zip.GZIPInputStream;


public class ChunkUnCompress implements Function<ChunkStore.Chunk, ChunkStore.Chunk> {

    public ChunkStore.Chunk apply(ChunkStore.Chunk in) {

        try (ByteArrayInputStream bis = new ByteArrayInputStream(in.data);
             ByteArrayOutputStream bos = new ByteArrayOutputStream();
             GZIPInputStream gzipIS = new GZIPInputStream(bis)) {
            byte[] buffer = new byte[8192];
            int len;
            while ((len = gzipIS.read(buffer)) != -1) {
                bos.write(buffer, 0, len);
            }
            in.data= bos.toByteArray();
            in.pos= in.data.length;
        } catch (Exception e) {// must catch all Exception, because run in ExecutorService
            in.status= -1;
            e.printStackTrace();
        }

        return in;
    }

}
