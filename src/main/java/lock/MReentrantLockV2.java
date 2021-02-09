package lock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.misc.Unsafe;
import unsafe.MUnsafe;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.LockSupport;
/*
1. 非公平、可重入、独占锁
2. 通过CAS+volatile实现原子性、可见性、有序性，并搭配LockSupport的park和unpark实现线程阻塞以及线程唤醒
3. 通过链表实现队列，保存获取锁失败的线程

TODO：需要解决的问题：
1. 当线程A持有锁，线程B获取锁时会失败并尝试加入队列中，但是在加入队列之前，线程A释放了锁后，线程B才加入队列，如果后续没有其他线程获得锁，
    则导致线程B无限阻塞。（已解决：在线程B加入队列后再次尝试CAS，失败才调用park方法阻塞）
2. 优化性能问题
3. 实现Condition

 */

public class MReentrantLockV2 implements Lock {

    private static Logger logger = LoggerFactory.getLogger(MReentrantLockV2.class);

    public static  long count = 0;

    private volatile int state = 0;
    private volatile Node head = new Node();
    private volatile Node tail = head;
    private volatile Thread currentThread = null;

    private static final Unsafe UNSAFE = MUnsafe.getUnsafe();
    private static final long stateOffset;
    private static final long tailOffset;

    static {
        try {
            stateOffset = UNSAFE.objectFieldOffset(MReentrantLockV2.class.getDeclaredField("state"));
            //headOffset = UNSAFE.objectFieldOffset(MReentrantLock.class.getDeclaredField("head"));
            tailOffset = UNSAFE.objectFieldOffset(MReentrantLockV2.class.getDeclaredField("tail"));
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
            //如果tail == node，有可能在方法addNodeToTail中会发生冲突 head.next == null tail &&  != null

            //head.next = node.next;
            /*当tail == node，当前线程执行到这里，如果另一个线程A执行addNodeToTail方法修改了tail，则下面这条CAS操作执行失败，
            此时head.next == null，但是tail != null
            导致线程A永远得不到唤醒
             */
            //compareAndSetTail(node, head);

            if(!compareAndSetTail(node, head)) {
                /*代码执行到此处说明 tail != node，有两种情况：
                    1. 此前 tail == node ，但当前方法执行CAS之前，其他线程执行addNodeToTail中的CAS成功修改tail，
                       但是还未执行 t.next = node 语句，因此node.next == null，所以需要等待该赋值语句完成，
                       否则导致head.next == null，后续线程无法被唤醒

                    2. 此前tail != node，还有其他线程在队列中排队，不需要额外处理

                 */
                //TODO:此处需要优化
                while(node.next == null) {
                    Thread.yield();
                }

                head.next = node.next;
            }
            //TODO：head.next得不到释放，无法被回收

        }
        //System.out.println(currentThread.getName() + " lock");

    }

    private void spinToGetLock() {
        Node node = new Node(Thread.currentThread());
        //将代表当前线程的节点node添加到队列的尾部
        addNodeToTail(node);

        while (true) {
            //如果node的前一个节点是头节点就尝试CAS加锁，否则继续阻塞
            if(head.next == node && compareAndSetState(0, 1)) {
                break;
            }
            /*
            park()不能放到上面的CAS之前，否则存在以下执行顺序有可能导致线程不会被唤醒：
            A加锁成功->B尝试加锁失败->B开始加入队列，但还没有完成->A释放锁，尝试唤醒下一个线程，但是队列中没有其他线程等待->B加入队列完成，等待唤醒
            如果后续没有其他线程获取锁，那么B线程不会被唤醒
            所以在线程加入队列之后还需要CAS后再阻塞
             */
            LockSupport.park();
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
        //System.out.println(currentThread.getName() + " unlock");
        currentThread = null;
        state--;
        if(state == 0) {
            //唤醒下一个线程
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
//        ReentrantLock lock = null;
//        lock.lock();
        logger.info("begin to record");

        //Thread.sleep(10000);

        MReentrantLockV2 mReentrantLock = new MReentrantLockV2();
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
}
