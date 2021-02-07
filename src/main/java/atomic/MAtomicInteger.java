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

    public void increment() {
        getAndIncrement(1);
    }

    public void decrease() {
        getAndIncrement(-1);
    }

    public int getAndIncrement(int added) {
        //int ret = 0;
        return UNSAFE.getAndAddInt(this, valueOffset, added);
    }


}
