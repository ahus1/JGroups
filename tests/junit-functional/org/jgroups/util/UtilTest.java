package org.jgroups.util;

import com.beust.ah.A;
import org.jgroups.Global;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.net.BindException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Test(groups= Global.FUNCTIONAL)
public class UtilTest {

    /**
     * This test fails due to some "java.net.BindException: No available port to bind to in range [10000 .. 10010]".
     * The cause: The class {@link java.net.ServerSocket} has the following snippet:
     * <pre>
     * getImpl().bind(epoint.getAddress(), epoint.getPort());
     * getImpl().listen(backlog);
     * </pre>
     * When calling this concurrently, the bind() might succeed for multiple sockets, but then the listen fails.
     * When this happens, the socket implementation is already bound to the address and port, and this can't be changed in later calls.
     * In all later calls that try to change the port, it will fail with "Already bound".
     * A workaround could be to create a completely new socket, and discard the old one due to its inconsistent state.
     */
    public void testBindingToTwoPortsInRange() throws Exception {
        SocketFactory factory = new DefaultSocketFactory();
        AtomicInteger count = new AtomicInteger();
        List<Thread> threads = new LinkedList<Thread>();
        for (int i = 0; i < 1000; ++i) {
            Thread thread = new Thread(() -> {
                try {
                    bindToPortInRange(factory);
                } catch (Exception e) {
                    e.printStackTrace();
                    count.incrementAndGet();
                }
            }
            );
            threads.add(thread);
            thread.start();
        }
        threads.forEach(thread -> {
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
                count.incrementAndGet();
            }
        });
        Assert.assertEquals(count.get(), 0);
    }

    private static void bindToPortInRange(SocketFactory factory) throws Exception {
        Util.createServerSocket(factory, "service", InetAddress.getByName("0.0.0.0"), 10000, 11000, 100);
    }

}