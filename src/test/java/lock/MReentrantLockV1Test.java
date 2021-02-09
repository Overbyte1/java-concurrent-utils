package lock;

import junit.framework.TestCase;
import org.junit.Test;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;

public class MReentrantLockV1Test extends TestCase {
    private static int count = 0;
    @Test
    public void testLock() throws InterruptedException {
        MReentrantLock mReentrantLock = new MReentrantLock();
        CyclicBarrier cyclicBarrier = new CyclicBarrier(10);
        Runnable runnable = () -> {
            try {
                cyclicBarrier.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (BrokenBarrierException e) {
                e.printStackTrace();
            }
            for (int i = 0; i < 1000000; i++) {
                mReentrantLock.lock();
                //logger.debug("Thread id {}", Thread.currentThread());
                count++;
                mReentrantLock.unlock();
            }
        };
        Thread thread1 = new Thread(runnable, "thread1");
        Thread thread2 = new Thread(runnable, "thread2");
        Thread thread3 = new Thread(runnable, "thread3");
        Thread thread4 = new Thread(runnable, "thread4");
        Thread thread5 = new Thread(runnable, "thread5");
        Thread thread6 = new Thread(runnable, "thread6");
        Thread thread7 = new Thread(runnable, "thread7");
        Thread thread8 = new Thread(runnable, "thread8");
        Thread thread9 = new Thread(runnable, "thread9");
        Thread thread10 = new Thread(runnable, "thread10");

        thread1.start();
        thread2.start();
        thread3.start();
        thread4.start();
        thread5.start();
        thread6.start();
        thread7.start();
        thread8.start();
        thread9.start();
        thread10.start();
        System.out.println("ok");
        thread1.join();
        thread2.join();
        thread3.join();
        thread4.join();
        thread5.join();
        thread6.join();
        thread7.join();
        thread8.join();
        thread9.join();
        thread10.join();
        System.out.println("count" + count);
    }


    public void testLockInterruptibly() {
    }

    public void testTryLock() throws InterruptedException {
        MReentrantLock reentrantLock = new MReentrantLock();
        Thread thread = new Thread(() -> {
            reentrantLock.lock();
            System.out.println(Thread.currentThread() + "locked");
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            reentrantLock.unlock();
            System.out.println(Thread.currentThread() + " unlock");
        });
        thread.start();
        Thread.sleep(500);
        boolean b = reentrantLock.tryLock();
        System.out.println("b = " + b);
        Thread.sleep(6000);
        b = reentrantLock.tryLock();
        System.out.println("b = " + b);
    }

    public void testTryLockTimeout() throws InterruptedException {
        MReentrantLock reentrantLock = new MReentrantLock();
        Thread thread = new Thread(() -> {
            reentrantLock.lock();
            System.out.println(Thread.currentThread() + "locked, time = " + System.currentTimeMillis());
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            reentrantLock.unlock();
            System.out.println(Thread.currentThread() + " unlock, time = " + System.currentTimeMillis());
            System.out.println("=====================================================");
        });
        thread.start();

        Runnable runnable = () -> {
            while (true) {
                System.out.println(Thread.currentThread() + " start locking, time = " + System.currentTimeMillis());
                boolean b = false;
                try {
                    b = reentrantLock.tryLock(1000, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                System.out.println(Thread.currentThread() + " return, result = " + b + ", time = " + System.currentTimeMillis());
                if (b) {
                    break;
                }
            }
        };
        Thread[] threads = new Thread[10];
        for (int i = 0; i < threads.length; i++) {
            threads[i] = new Thread(runnable);
            threads[i].start();
        }
        for (int i = 0; i < threads.length; i++) {
            threads[i].join();
        }
        thread.join();
    }

    public void testUnlock() {
    }

    public void testNewCondition() {
    }
}