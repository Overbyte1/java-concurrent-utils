package lock;

import sun.misc.Unsafe;
import unsafe.MUnsafe;

import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
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
            Node curNode = new Node(Thread.currentThread());
            //将代表当前线程的节点node添加到队列的尾部
            addNodeToTail(curNode);
            spinToGetLock(count, curNode);
            //获取锁成功后返回
            currentThread = Thread.currentThread();


            //TODO：head.next得不到释放，无法被回收
        } else { //获取锁成功
            if(currentThread == null) {
                currentThread = Thread.currentThread();
            }
        }

    }

    private void removeFirstNode() {
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
    }

    /**
     * 清理已经取消等待的节点，只有当锁被获取后被调用
     * 因为只有获取锁的线程修改除了tail以外的节点，其他线程会通过addTail()修改tail，所以需要考虑这种情况
     * */
    private void cleanCancelNode() {

        Node t = tail, node = head.next, pre = head;
        while(node.next != t) {
            //addTail()执行时可能会出现这种情况：node.next == null -> node.next == tail，或者是链表除头节点只有一个节点
            if(node.next == null) {
                break;
            }
            if(node.waitStatus == Node.CANCEL) {
                pre.next = node.next;
                node = node.next;
            } else {
                pre = node;
                node = node.next;
            }
        }
    }

    public boolean tryAcquireNanos(int count, long timeout, TimeUnit timeUnit) {
        if(timeout < 0 || count <= 0) {
            return false;
        }
        Node node = new Node(Thread.currentThread());
        addNodeToTail(node);
        long nanoTime = timeUnit.toNanos(timeout);
        long deadline = System.nanoTime() + nanoTime;
        long lastTime = deadline - System.nanoTime();
        while(lastTime > 0) {
            if(/*node == head.next && */tryAccquire(1)) {
                //清理已经取消等待的节点
                cleanCancelNode();
                //除去首节点
                removeFirstNode();
                return true;
            }
            LockSupport.parkNanos(lastTime);
            lastTime = deadline - System.nanoTime();
        }
        //TODO:将节点状态修改为 CANCEL
        cancelAcquire(node);

        return false;
    }

    //将节点的状态修改为取消状态CANCEL
    private void cancelAcquire(Node node) {
        node.waitStatus = Node.CANCEL;
        node.thread = null;

    }

    /**
     * 唤醒下一个等待锁（队列首）的线程
     * @param count
     */
    public void release(int count) {
        if(currentThread != Thread.currentThread() || count > state) {
            throw new IllegalMonitorStateException();
        }
        if(count <= 0) {
            throw new IllegalArgumentException("argument can not be negative");
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
//        Node node = head.next, pre = head;
//        while (node != null) {
//            if(node.waitStatus == Node.CANCEL) {
//                pre.next = node.next;
//                node = node.next;
//                pre = node;
//                continue;
//            }
//            LockSupport.unpark(node.thread);
//        }

        //有些线程可能已经取消等待，所以等待状态是CNACEL，找到第一个等待状态不为CANCEL的节点进行唤醒
        Node node = head.next;
        while(node != null) {
            if(node.waitStatus > Node.CANCEL) {
                LockSupport.unpark(node.thread);
                break;
            }
            node = node.next;
        }
    }

    protected abstract boolean tryAccquire(int count);
    protected abstract boolean tryRelease(int count);

    private void spinToGetLock(int count, Node node) {


        while (true) {
            //如果node的前一个节点是头节点就尝试CAS加锁，否则继续阻塞
            if(/*head.next == node && */compareAndSetState(0, count)) {
                //清理已经取消等待的节点
                //TODO:时间负责度为O（n），性能需要优化，考虑将单向链表改成双向链表
                cleanCancelNode();
                //除去首节点
                removeFirstNode();
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
    private int getState() {
        return state;
    }

    protected boolean compareAndSetState(int expect, int newVal) {
        return UNSAFE.compareAndSwapInt(this, stateOffset, expect, newVal);
    }
    protected boolean compareAndSetTail(Node expect, Node newTail) {
        return UNSAFE.compareAndSwapObject(this, tailOffset, expect, newTail);
    }



    private static class Node {
        volatile Node next;
        //用于condition queue
        volatile Node nextWaiter;
        volatile boolean inConditionQueue;
        volatile int waitStatus;
        //该节点已经被取消等待锁
        static int CANCEL = -1;
        static final int INIT = 0;
        //等待被唤醒
        static final int SIGNAL = 1;
        //volatile Node pre;
        volatile Thread thread;


        public Node() {
            inConditionQueue = false;
        }
        public Node(Thread thread) {
            this.thread = thread;
        }
        public Node(Thread thread, boolean inConditionQueue) {
            this.thread = thread;
            this.inConditionQueue = inConditionQueue;
        }
    }

    private class ConditionObject implements Condition {
        private volatile Node head = new Node();
        private volatile Node tail = head;

        @Override
        public void await() throws InterruptedException {
            //检查当前线程是否持有锁，只有持有锁才能调用await()
            if(getState() == 0 || currentThread != Thread.currentThread()) {
                throw new IllegalMonitorStateException();
            }

            //将节点添加到条件队列 condition queue
            Node node = addWaiter();
            //释放当前线程持有的所有锁
            int saveState = getState();
            release(saveState);
            //判断是否被唤醒，如果没被唤醒则一直阻塞
            while(node.inConditionQueue) {
                LockSupport.park();
            }
            //此时节点已经被转移到同步队列中，尝试获取锁
            spinToGetLock(saveState, node);

        }
        private Node addWaiter() {
            Node node = new Node(Thread.currentThread(), true);
            tail.nextWaiter = node;
            tail = node;
            return node;
        }

        @Override
        public void awaitUninterruptibly() {

        }

        @Override
        public long awaitNanos(long nanosTimeout) throws InterruptedException {
            return 0;
        }

        @Override
        public boolean await(long time, TimeUnit unit) throws InterruptedException {
            return false;
        }

        @Override
        public boolean awaitUntil(Date deadline) throws InterruptedException {
            return false;
        }

        @Override
        public void signal() {

        }
        private void doSignal() {

        }

        @Override
        public void signalAll() {

        }
    }

}
