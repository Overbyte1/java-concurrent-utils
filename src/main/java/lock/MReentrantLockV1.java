package lock;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

public class MReentrantLockV1 implements Lock {

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

    public static void main(String[] args) throws InterruptedException {
        MReentrantLockV1 mReentrantLock = new MReentrantLockV1();
        CyclicBarrier cyclicBarrier = new CyclicBarrier(10);
        Runnable runnable = () -> {
            try {
                cyclicBarrier.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (BrokenBarrierException e) {
                e.printStackTrace();
            }
            for (int i = 0; i < 10000; i++) {
                mReentrantLock.lock();
                //logger.debug("Thread id {}", Thread.currentThread());
                MReentrantLock.count++;
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
        System.out.println("count" + MReentrantLock.count);

    }

}
