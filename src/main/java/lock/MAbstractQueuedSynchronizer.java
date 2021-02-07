package lock;

import sun.misc.Unsafe;
import unsafe.MUnsafe;

import java.util.concurrent.locks.LockSupport;

/*
1. 非公平、可重入、独占锁
2. 通过CAS+volatile实现原子性、可见性、有序性，并搭配LockSupport的park和unpark实现线程阻塞以及线程唤醒
3. 通过链表实现队列，保存获取锁失败的线程
4. 不支持中断、尝试加锁，可通过添加 等待状态 来支持中断

TODO：需要解决的问题：
1. （已解决：在线程B加入队列后再次尝试CAS，失败才调用park方法阻塞）当线程A持有锁，线程B获取锁时会失败并尝试加入队列中，但是在加入队列之前，线程A释放了锁后，线程B才加入队列，如果后续没有其他线程获得锁，
    则导致线程B无限阻塞。
2. 优化性能问题
3. 实现Condition

 */

public abstract class MAbstractQueuedSynchronizer {
    protected volatile int state = 0;
    protected volatile Thread currentThread = null;
    private volatile Node head = new Node();
    private volatile Node tail = head;

    private static final Unsafe UNSAFE = MUnsafe.getUnsafe();
    private static final long stateOffset;
    private static final long tailOffset;

    static {
        try {
            stateOffset = UNSAFE.objectFieldOffset(MAbstractQueuedSynchronizer.class.getDeclaredField("state"));
            //headOffset = UNSAFE.objectFieldOffset(MReentrantLock.class.getDeclaredField("head"));
            tailOffset = UNSAFE.objectFieldOffset(MAbstractQueuedSynchronizer.class.getDeclaredField("tail"));
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
            throw new Error(e);
        }
    }

    public void accquire(int count) {
        if(!tryAccquire(count)) {
            //加入队列，并开始自旋
            //addNodeToTail(new Node(Thread.currentThread()));
            spinToGetLock(count);
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
        } else { //获取锁成功
            if(currentThread == null) {
                currentThread = Thread.currentThread();
            }
        }

    }

    /**
     * 唤醒下一个等待锁（队列首）的线程
     * @param count
     */
    public void release(int count) {
        if(currentThread != Thread.currentThread() || count > state) {
            throw new IllegalMonitorStateException();
        }
        if(tryRelease(count)) {
            //System.out.println(Thread.currentThread() + " unlock state = " + state);
            if(state == 0) {
                //currentThread = null;
                //唤醒下一个线程
                wakeUpNext();
            }
        }
    }

    private void wakeUpNext() {
        //System.out.println(Thread.currentThread() + " call wakeUpNext()");
        if(head.next != null) {
            LockSupport.unpark(head.next.thread);
        }
    }

    protected abstract boolean tryAccquire(int count);
    protected abstract boolean tryRelease(int count);

    private void spinToGetLock(int count) {
        Node node = new Node(Thread.currentThread());
        //将代表当前线程的节点node添加到队列的尾部
        addNodeToTail(node);

        while (true) {
            //如果node的前一个节点是头节点就尝试CAS加锁，否则继续阻塞
            if(head.next == node && compareAndSetState(0, count)) {
                break;
            }
            /*
            park()不能放到上面的CAS之前，否则存在以下执行顺序有可能导致线程不会被唤醒：
            A加锁成功->B尝试加锁失败->B开始加入队列，但还没有完成->A释放锁，尝试唤醒下一个线程，但是队列中没有其他线程等待->B加入队列完成，等待唤醒
            如果后续没有其他线程获取锁，那么B线程不会被唤醒
            所以在线程加入队列之后还需要CAS后再阻塞
             */
            LockSupport.park();
            //System.out.println(Thread.currentThread() + " was wakeup");
        }
    }
    //将代表线程的节点添加到链表尾部
    private void addNodeToTail(Node node) {
        for(;;) {
            //System.out.println(Thread.currentThread() + "add tail start------------------------");
            Node t = tail;
            //t.next = node; 该赋值语句不能放此处
            if(compareAndSetTail(t, node)) {
                t.next = node;
                //System.out.println(Thread.currentThread() + "add tail success------------------------");
                return;
            }
        }
    }

    protected boolean compareAndSetState(int expect, int newVal) {
        return UNSAFE.compareAndSwapInt(this, stateOffset, expect, newVal);
    }
    protected boolean compareAndSetTail(Node expect, Node newTail) {
        return UNSAFE.compareAndSwapObject(this, tailOffset, expect, newTail);
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

}
