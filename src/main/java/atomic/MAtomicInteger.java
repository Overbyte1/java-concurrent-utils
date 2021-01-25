package atomic;

import sun.misc.Unsafe;
import unsafe.MUnsafe;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

public class MAtomicInteger {
    private volatile int val;
    private static final Unsafe UNSAFE = MUnsafe.getUnsafe();
    private static final long valueOffset;


    static {
        try {
            valueOffset = UNSAFE.objectFieldOffset(MAtomicInteger.class.getDeclaredField("val"));
        } catch (Exception e) {
            //TODO
            e.printStackTrace();
            throw new Error(e);
            //throw new Error(e);
        }
    }
    public MAtomicInteger() {
        val = 0;
    }
    public MAtomicInteger(int v) {
        val = v;
    }

    public boolean compareAndSet(int expect, int newVal) {
        return UNSAFE.compareAndSwapInt(this, valueOffset, expect, newVal);
    }

    public int get() {
        return val;
    }

    public void set(int v) {
        val = v;
    }

    public int getAndIncrement() {
        //int ret = 0;
        return UNSAFE.getAndAddInt(this, valueOffset, 1);
    }

    public static void main(String[] args) throws InterruptedException {
        //AtomicStampedReference
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
                atomicInteger.getAndIncrement();
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
