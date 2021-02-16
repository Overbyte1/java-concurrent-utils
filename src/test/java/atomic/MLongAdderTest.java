package atomic;

import junit.framework.TestCase;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.LongAdder;

public class MLongAdderTest extends TestCase {

    public void testAdd() throws InterruptedException {

        MLongAdder mLongAdder = new MLongAdder();
        int loopTimes = 100000000, threadNum = 10;
        Runnable runnable = () -> {
            for (int i = 0; i < loopTimes; i++) {
                mLongAdder.add(1);
            }
        };
        long start = System.currentTimeMillis();
        Thread[] threads = new Thread[threadNum];
        for(int i = 0; i < threads.length; i++) {
            threads[i] = new Thread(runnable);
            threads[i].start();
        }
        for(int i = 0; i < threads.length; i++) {
            threads[i].join();
        }
        long end = System.currentTimeMillis();
        System.out.println("MLongAdder time = " + (end - start));
        assertEquals(threadNum * loopTimes, mLongAdder.sum());
    }
    public void testLongAdder() throws InterruptedException {
        LongAdder mLongAdder = new LongAdder();
        //MLongAdder mLongAdder = new MLongAdder();
        int loopTimes = 100000000, threadNum = 10;
        Runnable runnable = () -> {
            for (int i = 0; i < loopTimes; i++) {
                mLongAdder.add(1);
            }
        };
        long start = System.currentTimeMillis();
        Thread[] threads = new Thread[threadNum];
        for(int i = 0; i < threads.length; i++) {
            threads[i] = new Thread(runnable);
            threads[i].start();
        }
        for(int i = 0; i < threads.length; i++) {
            threads[i].join();
        }
        long end = System.currentTimeMillis();
        System.out.println("LongAdder time = " + (end - start));
        assertEquals(threadNum * loopTimes, mLongAdder.sum());
        ThreadLocalRandom random;
    }

}