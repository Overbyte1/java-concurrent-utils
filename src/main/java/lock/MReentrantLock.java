package lock;

import java.util.concurrent.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

public class MReentrantLock implements Lock {

    private Sync sync = new Sync();

    private class Sync extends  MAbstractQueuedSynchronizer{
        @Override
        protected boolean tryAccquire(int count) {
            int s = state;
            if(s != 0 && Thread.currentThread() == currentThread) {
                state += count;
                return true;
            } else {
                if(compareAndSetState(0, count)) {
                    currentThread = Thread.currentThread();
                    return true;
                }
            }
            return false;
        }

        @Override
        protected boolean tryRelease(int count) {
            int expect = state - count;
            if(expect == 0) {
                currentThread = null;
            }
            state = expect;
            return true;
        }
    }

    @Override
    public void lock() {
        sync.accquire(1);
    }

    @Override
    public void lockInterruptibly() throws InterruptedException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean tryLock() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void unlock() {
        sync.release(1);
    }

    @Override
    public Condition newCondition() {
        return null;
    }


}
