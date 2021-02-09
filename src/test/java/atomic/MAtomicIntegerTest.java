package atomic;

import junit.framework.TestCase;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

public class MAtomicIntegerTest extends TestCase {

    public void testSet() throws InterruptedException {
        MAtomicInteger atomicInteger = new MAtomicInteger();
        CyclicBarrier cyclicBarrier = new CyclicBarrier(3);
        //int n = 0;
        Runnable runnable = () -> {
            //Thread.currentThread().yield();
            try {
                cyclicBarrier.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (BrokenBarrierException e) {
                e.printStackTrace();
            }
            for (int i = 0; i < 10000000; i++) {
                atomicInteger.getAndIncrement(1);
                //n++;
            }
        };
        Thread thread1 = new Thread(runnable);
        Thread thread2 = new Thread(runnable);
        Thread thread3 = new Thread(runnable);
        thread1.start();
        thread2.start();
        thread3.start();
        thread1.join();
        thread2.join();
        thread3.join();
        //System.out.println("n = " + n);
        System.out.println(atomicInteger.get());
    }
}