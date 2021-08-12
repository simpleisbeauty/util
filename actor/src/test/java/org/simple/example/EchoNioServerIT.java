package org.simple.example;

import org.junit.Test;
import org.simple.nio.NioChannel;
import org.simple.nio.NioChannelHandler;
import org.simple.nio.NioSelect;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.Assert.*;

public class EchoNioServerIT {

    @Test
    public void echoClient() throws IOException, Exception {

        System.out.println("I am Integration test");

        NioSelect nio = new NioSelect();

        new Thread(nio).start();

        EchoNioServer echoSrv = new EchoNioServer();
        echoSrv.listen(nio, new int[]{9002, 9003});

        int[] ports = {9000, 9001, 9002, 9000, 9003, 9001}; // 9000,9001 started echo server at pre-integration phase
        int l = ports.length;

        ExecutorService exec = Executors.newCachedThreadPool();

        NioChannel[] nchs = new NioChannel[l];
        String[] sends = {"1jaojsjgjdgjjajdsjgdjgjdsgjllllaadsfsafjgdsdsj", "2joajdsojgosjsjdjd102werewerw",
                "3dsfdsafsadfdssjnnanjdsjgjgd", "4janjjqoejoifijfljfl;kjfkjfssjsajdsfjdfjjjfdsj", "5jaodsfosfdsofsdahfdshf", "6klagaaa23jjtjqjjthhqhthh"};
        InetAddress addr = InetAddress.getByName("127.0.0.1");

        ByteArrayOutputStream[] blds = new ByteArrayOutputStream[l];
        NioChannelHandler echoCli = (context) -> new EchoClientChannel(exec, context);
        for (int i = 0; i < l; i++) {
            nchs[i] = nio.connect(addr, ports[i], echoCli);
            blds[i] = new ByteArrayOutputStream();
            byte[] bts = sends[i].getBytes();
            for (int q = 0; q < 1000; q++) {
                nchs[i].write(bts);
                blds[i].write(bts);// backup
            }
            nchs[i].flush();

        }

        Thread.sleep(10000);

        for (int i = 0; i < l; i++) {
            nchs[i].flush();

            blds[i].flush();
            ((EchoClientChannel) nchs[i]).echo.flush();
            assertArrayEquals(blds[i].toByteArray(), ((EchoClientChannel) nchs[i]).echo.toByteArray());
            System.out.println(i);
        }

        nio.close();

    }
}