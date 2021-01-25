package atomic;

import concurrent.unsafe.MUnsafe;
import sun.misc.Unsafe;

public class MAtomicStampedReference {
    private static final Unsafe UNSAFE = MUnsafe.getUnsafe();
    private static final long valueOffset;
    private volatile Pair pair;


    static {
        try {
            valueOffset = UNSAFE.objectFieldOffset(MAtomicInteger.class.getDeclaredField("pair"));
        } catch (Exception e) {
            //TODO
            e.printStackTrace();
            throw new Error(e);
            //throw new Error(e);
        }
    }

    public MAtomicStampedReference(int val, int stamp) {
        pair = Pair.of(val, stamp);
    }

    public boolean compareAndSet(Integer expectVal, int expectStamp, Integer newVal, int newStamp) {
        Pair current = pair;
        return expectStamp == current.stamp && expectVal == current.val
                && ((newStamp == pair.stamp && newVal == pair.stamp) || casPair(current, Pair.of(newVal, newStamp)));
    }

    private boolean casPair(Pair current, Pair newPair) {
        return UNSAFE.compareAndSwapObject(this, valueOffset, current, newPair);
    }

    private static class Pair{
        final Integer val;
        final int stamp;

        private Pair(Integer val, int stamp) {
            this.val = val;
            this.stamp = stamp;
        }
        static Pair of(Integer v, int st) {
            return new Pair(v, st);
        }
    }
}
