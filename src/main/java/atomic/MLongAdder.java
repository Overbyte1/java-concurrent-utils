package atomic;

import sun.misc.Contended;
import sun.misc.Unsafe;
import unsafe.MUnsafe;

import java.util.concurrent.ThreadLocalRandom;
import java.util.function.LongBinaryOperator;

public class MLongAdder {
    private volatile long base;
    private volatile Cell[] cells;
    private volatile int cellBusy = 0;
    private static final Unsafe UNSAFE;
    private static final int NCPU = Runtime.getRuntime().availableProcessors();

    private static final long baseOffset;
    private static final long cellBusyOffset;
    private static final long threadLocalRandomOffset;

    static {
        UNSAFE = MUnsafe.getUnsafe();
        try {
            baseOffset = UNSAFE.objectFieldOffset(MLongAdder.class.getDeclaredField("base"));
            cellBusyOffset = UNSAFE.objectFieldOffset(MLongAdder.class.getDeclaredField("cellBusy"));
            threadLocalRandomOffset = UNSAFE.objectFieldOffset(Thread.class.getDeclaredField("threadLocalRandomProbe"));
        } catch (NoSuchFieldException e) {
            throw new Error(e);
        }
    }

    private boolean casBase(long expectVal, long newVal) {
        return UNSAFE.compareAndSwapLong(this, baseOffset, expectVal, newVal);
    }

    private boolean casCellBusy() {
        return UNSAFE.compareAndSwapInt(this, cellBusyOffset, 0, 1);
    }

    public void add(long x) {
        long v = 0;
        if (cells != null || !casBase(v = base, v + 1)) {
            int hash = getProbe();
            while (true) {
                int len, idx;
                if (cells != null) {
                    //hash = getProbe();
                    if (hash == 0) {
                        ThreadLocalRandom.current();
                        hash = getProbe();
                    }
                    //advanceProbe(hash);

                    len = cells.length;
                    idx = hash & (len - 1);
                    Cell[] cls = cells;
                    Cell cel = cls[idx];
                    if (cel == null) {
                        if (cellBusy != 0){
                            hash = advanceProbe(hash);
                            continue;
                        }
                        if (casCellBusy()) {
                            try {
                                cls = cells;
                                cel = cls[idx];
                                //再次检查
                                if (cel == null) {
                                    cls[idx] = new Cell(x);
                                    break;
                                }
                            } finally {
                                cellBusy = 0;
                            }
                            continue;
                        }
                        //如果正在扩容、迁移元素，则不能进行CAS
                    } else if (cellBusy == 0 && cel.casValue(v = cel.value, v + x)) {
                        break;
                        //NCPU关系着数组的最大长度，长度越大则意味着冲突越小，但是在统计总和的时候就更加耗时
                        //从减少冲突的角度上看，数组长度最好稍大于CPU核数，但是因为数组的扩容倍数是两倍，所以数组长度不好控制，因此是大于等于NCPU
                    } else if (len >= NCPU || cls != cells) {    //cells长度达到上限，或者已经被扩容了
                        hash = advanceProbe(hash);
                        continue;
                    } else if (cellBusy == 0 && casCellBusy()) {    //进行扩容
                        try {
                            if (cls != cells){
                                continue;
                            }
                            Cell[] tempCell = new Cell[len << 1];
                            for (int i = 0; i < len; i++) {
                                tempCell[i] = cls[i];
                            }
                            tempCell[0].value += x;
                            cells = tempCell;
                        } finally {
                            cellBusy = 0;
                        }
                        break;
                    }
                } else if (cellBusy == 0 && casCellBusy()) { //初始化cells
                    try {
                        Cell[] ts = new Cell[2];
                        ts[0] = new Cell(x);
                        cells = ts;
                    } finally {
                        cellBusy = 0;
                    }
                    break;

                } else if (casBase(v = base, v + x)) {
                    break;
                }
            }
        }
    }


    public long sum() {
        long sum = base;
        if (cells == null) {
            return sum;
        }
        Cell[] tempCells = cells;
        for (Cell cell : tempCells) {
            if (cell != null) {
                sum += cell.value;
            }
        }
        return sum;
    }

    public int getProbe() {
        return UNSAFE.getInt(Thread.currentThread(), threadLocalRandomOffset);
    }


    @Contended
    static final class Cell {
        volatile long value;
        private static final Unsafe UNSAFE;
        private static final long valueOffset;

        public Cell(long v) {
            value = v;
        }

        boolean casValue(long expectVal, long newValue) {
            return UNSAFE.compareAndSwapLong(this, valueOffset, expectVal, newValue);
        }

        static {
            UNSAFE = MUnsafe.getUnsafe();
            try {
                valueOffset = UNSAFE.objectFieldOffset(Cell.class.getDeclaredField("value"));
            } catch (NoSuchFieldException e) {
                throw new Error(e);
            }
        }
    }

    static final int advanceProbe(int probe) {
        probe ^= probe << 13;   // xorshift
        probe ^= probe >>> 17;
        probe ^= probe << 5;
        UNSAFE.putInt(Thread.currentThread(), threadLocalRandomOffset, probe);
        return probe;
    }

}
