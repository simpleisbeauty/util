package org.simple.example;

import java.io.ByteArrayOutputStream;
import java.util.function.Function;
import java.util.zip.GZIPOutputStream;

public class ChunkCompress implements Function<SplitInput.Chunk, SplitInput.Chunk> {

    public SplitInput.Chunk apply(SplitInput.Chunk in) {

        try (ByteArrayOutputStream bos = new ByteArrayOutputStream(in.pos);
             GZIPOutputStream gzipOS = new GZIPOutputStream(bos)) {
            gzipOS.write(in.data, 0, in.pos);

            gzipOS.close(); //close it first
            in.data=bos.toByteArray();// compressed data
            //in.pos is the uncompressed data length for seek
        } catch (Exception e) { // must catch all Exception, because run in ExecutorService
            in.status= -1;
            e.printStackTrace();
        }
        return in;
    }

}
