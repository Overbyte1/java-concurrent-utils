package lock;

import sun.misc.Unsafe;
import unsafe.MUnsafe;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.LockSupport;

public class MReentrantLock implements Lock {

    private static  long count = 0;

    private volatile int state = 0;
    private volatile Node head = new Node();
    private volatile Node tail = head;
    private volatile Thread currentThread = null;

    private static final Unsafe UNSAFE = MUnsafe.getUnsafe();
    private static final long stateOffset;
    private static final long tailOffset;

    static {
        try {
            stateOffset = UNSAFE.objectFieldOffset(MReentrantLock.class.getDeclaredField("state"));
            //headOffset = UNSAFE.objectFieldOffset(MReentrantLock.class.getDeclaredField("head"));
            tailOffset = UNSAFE.objectFieldOffset(MReentrantLock.class.getDeclaredField("tail"));
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
            throw new Error(e);
        }
    }



    private boolean compareAndSetState(int expect, int newVal) {
        return UNSAFE.compareAndSwapInt(this, stateOffset, expect, newVal);
    }
    private boolean compareAndSetTail(Node expect, Node newTail) {
        return UNSAFE.compareAndSwapObject(this, tailOffset, expect, newTail);
    }

    @Override
    public void lock() {
        //1. 判断state是否为0，如果不为0并且是持有锁的线程，则重入次数加1
        if(state != 0) {
            if(currentThread == Thread.currentThread()) {
                state++;
                return;
            }
        }
        if(compareAndSetState(0, 1)) {
            currentThread = Thread.currentThread();
        } else {
            spinToGetLock();
            //获取锁成功后返回
            currentThread = Thread.currentThread();

            Node node = head.next;

            //将当前节点从链表中移出

            if(node == null) {
                System.out.println("null");
            }
            head.next = node.next;
            compareAndSetTail(node, head);

        }
        System.out.println(currentThread.getName() + " lock");

    }

    private void spinToGetLock() {
        Node node = new Node(Thread.currentThread());
        addNodeToTail(node);

        while (true) {
            LockSupport.park();
            if(head.next == node && compareAndSetState(0, 1)) {
                break;
            }
        }
    }

    //将代表线程的节点添加到链表尾部
    private void addNodeToTail(Node node) {
        for(;;) {
            Node t = tail;
            //t.next = node; 该赋值语句不能放此处
            if(compareAndSetTail(t, node)) {
                t.next = node;
                return;
            }
        }
    }

    @Override
    public void lockInterruptibly() throws InterruptedException {

    }

    @Override
    public boolean tryLock() {
        return false;
    }

    @Override
    public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
        return false;
    }

    @Override
    public void unlock() {
        //判断当前线程是否是持有锁的线程
        if(state == 0 && Thread.currentThread() != currentThread) {
            throw new IllegalMonitorStateException();
        }
        System.out.println(currentThread.getName() + " unlock");
        currentThread = null;
        state--;
        if(state == 0) {
            wakeUpNext();
        }

    }
    private void wakeUpNext() {
        if(head.next != null) {
            LockSupport.unpark(head.next.thread);
        }
    }

    @Override
    public Condition newCondition() {
        return null;
    }

    private static class Node {
        volatile Node next;
        //volatile Node pre;
        volatile Thread thread;
        public Node() {}
        public Node(Thread thread) {
            this.thread = thread;
        }

    }

    public static void main(String[] args) throws InterruptedException {
        MReentrantLock mReentrantLock = new MReentrantLock();
        CyclicBarrier cyclicBarrier = new CyclicBarrier(8);
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

        thread1.start();
        thread2.start();
        thread3.start();
        thread4.start();
        thread5.start();
        thread6.start();
        thread7.start();
        thread8.start();
        System.out.println("ok");
        thread1.join();
        thread2.join();
        thread3.join();
        thread4.join();
        thread5.join();
        thread6.join();
        thread7.join();
        thread8.join();
        System.out.println("count" + count);

    }
}
