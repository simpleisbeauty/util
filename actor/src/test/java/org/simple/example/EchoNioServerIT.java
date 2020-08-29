package org.simple.example;

import static org.junit.Assert.*;

import org.junit.Test;
import org.simple.nio.AbstractNioChannel;
import org.simple.nio.NioSelect;
import org.simple.example.EchoNioClient.EchoNioClientChannel;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetAddress;

/**
 * Created by johnny on 7/25/18.
 */
public class EchoNioServerIT {
    @Test
    public void echoClient() throws IOException, Exception{

        System.out.println("I am Integration test");

        NioSelect nio= new NioSelect();

        new Thread(nio).start();

        EchoNioServer echoSrv= new EchoNioServer();
        echoSrv.listen(nio, new int[]{9001, 9000, 9002, 9003});

        int[] ports={9000, 9001, 9002, 9000, 9003, 9001}; // 9000,9001 started echo server at pre-integration phase
        int l= ports.length;

        AbstractNioChannel[] nchs= new AbstractNioChannel[l];
        String[] sends={"1jaojsjgjdgjjajdsjgdjgjdsgjllllaadsfsafjgdsdsj", "2joajdsojgosjsjdjd102werewerw",
                "3dsfdsafsadfdssjnnanjdsjgjgd", "4janjjqoejoifijfljfl;kjfkjfssjsajdsfjdfjjjfdsj", "5jaodsfosfdsofsdahfdshf", "6klagaaa23jjtjqjjthhqhthh"};
        InetAddress addr= InetAddress.getByName("127.0.0.1");
        EchoNioClient cli= new EchoNioClient();
        ByteArrayOutputStream[] blds= new ByteArrayOutputStream[l];
        for (int i=0; i<l; i++){
            nchs[i]= nio.connect(addr, ports[i], cli);
            blds[i]= new ByteArrayOutputStream();
            byte[] bts=sends[i].getBytes();
            for(int q=0; q<1000; q++) {
                nchs[i].write(bts);
                blds[i].write(bts);// backup
            }
            nchs[i].flush();

        }

        Thread.sleep(10000);

        for(int i=0; i<l; i++) {
            nchs[i].flush();

            blds[i].flush();
            ((EchoNioClientChannel)nchs[i]).echo.flush();
            assertArrayEquals(blds[i].toByteArray(), ((EchoNioClientChannel)nchs[i]).echo.toByteArray());
            System.out.println(i);
        }

        nio.close();

    }
}