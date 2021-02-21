package atomic;

import junit.framework.TestCase;
import org.junit.Test;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.LongAdder;

import static org.junit.Assert.assertEquals;

public class MLongAdderTest {
    private long loopTimes = 10000000;
    private int threadNum = 10;
    @Test
    public void testLongAdder() throws InterruptedException {
        //JUC包的LongAdder
        LongAdder longAdder = new LongAdder();

        Runnable runnable = () -> {
            for (int i = 0; i < loopTimes; i++) {
                longAdder.add(1);
            }
        };
        //开始时间
        long start = System.currentTimeMillis();
        Thread[] threads = new Thread[threadNum];
        for(int i = 0; i < threads.length; i++) {
            threads[i] = new Thread(runnable);
            threads[i].start();
        }
        for(int i = 0; i < threads.length; i++) {
            threads[i].join();
        }
        //结束时间
        long end = System.currentTimeMillis();
        System.out.println("LongAdder time = " + (end - start) + " ms");
        assertEquals(threadNum * loopTimes,  longAdder.sum());
    }
    @Test
    public void testMLongAdder() throws InterruptedException {
        //MLongAdder自己实现
        MLongAdder mLongAdder = new MLongAdder();

        Runnable runnable = () -> {
            for (int i = 0; i < loopTimes; i++) {
                mLongAdder.add(1);
            }
        };
        //开始时间
        long start = System.currentTimeMillis();
        Thread[] threads = new Thread[threadNum];
        for(int i = 0; i < threads.length; i++) {
            threads[i] = new Thread(runnable);
            threads[i].start();
        }
        for(int i = 0; i < threads.length; i++) {
            threads[i].join();
        }
        //结束时间
        long end = System.currentTimeMillis();
        System.out.println("MLongAdder time = " + (end - start) + " ms");
        assertEquals(threadNum * loopTimes, mLongAdder.sum());
    }
    @Test
    public void testLongAdder_2() throws InterruptedException {
        //LongAdder_2
        LongAdder_2 mLongAdder = new LongAdder_2();

        Runnable runnable = () -> {
            for (int i = 0; i < loopTimes; i++) {
                mLongAdder.add(1);
            }
        };
        //开始时间
        long start = System.currentTimeMillis();
        Thread[] threads = new Thread[threadNum];
        for(int i = 0; i < threads.length; i++) {
            threads[i] = new Thread(runnable);
            threads[i].start();
        }
        for(int i = 0; i < threads.length; i++) {
            threads[i].join();
        }
        //结束时间
        long end = System.currentTimeMillis();
        System.out.println("LongAdder_2 time = " + (end - start) + " ms");
        assertEquals(threadNum * loopTimes, mLongAdder.sum());
    }


}