package atomic;

import java.util.concurrent.atomic.LongAccumulator;
import java.util.concurrent.atomic.LongAdder;
import java.util.concurrent.locks.ReentrantLock;

public class MLongAdder {
    public static void main(String[] args) {
        ReentrantLock lock;
        LongAdder longAdder = new LongAdder();
        longAdder.add(1);
    }
}
