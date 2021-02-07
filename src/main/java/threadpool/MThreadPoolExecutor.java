package threadpool;

import atomic.MAtomicInteger;
import lock.MReentrantLock;

import java.util.HashSet;
import java.util.concurrent.*;

/**
 * 线程池，任务单元与执行单元分离，暂不考虑返回结果
 * 参数：
 * 1. 核心线程数量
 * 2. 最大线程数量
 * 3. 阻塞队列类型
 * 4. 非核心线程空闲时间
 * 5. 时间的单元
 * 6. 拒绝策略
 * 7. 线程工厂
 *
 * 提供的方法：
 * execute()
 * stop()
 * shutdown()
 * Worker类负责执行任务队列中的任务
 */
public class MThreadPoolExecutor implements MExecutorService {
    private volatile int maxThreadSize;
    private volatile int maxCoreThreadSize;
    private volatile BlockingQueue<Runnable> taskQueue;
    private volatile long keepAliveTime;
    private TimeUnit timeUnit;
    private MRejectedExecutionHandler rejectedExecutionHandler = new AbortPolicy();
    private ThreadFactory threadFactory;
    //当前worker数量
    private MAtomicInteger workerCount = new MAtomicInteger();
    //private MAtomicInteger threadCount = new MAtomicInteger();

    private static final int RUNNING = 1;
    private static final int SHUTDOWN = 2;
    private static final int STOP = 3;

    private MReentrantLock thisLock = new MReentrantLock();
    //记录所有的worker
    private HashSet<Worker> workerHashSet = new HashSet<>();


    private volatile int state = RUNNING;

    public MThreadPoolExecutor(int maxThreadSize, int maxCoreThreadSize, BlockingQueue<Runnable> taskQueue) {
        if(maxThreadSize < maxCoreThreadSize) {
            new IllegalArgumentException("maxThreadSize must be greater or equal than maxCoreThreadSize");
        }
        this.maxThreadSize = maxThreadSize;
        this.maxCoreThreadSize = maxCoreThreadSize;
        this.taskQueue = taskQueue;
    }

    public MThreadPoolExecutor(int maxThreadSize, int maxCoreThreadSize, BlockingQueue<Runnable> taskQueue,
                               long keepAliveTime, TimeUnit timeUnit) {
        this(maxThreadSize, maxCoreThreadSize, taskQueue);
        this.keepAliveTime = keepAliveTime;
        this.timeUnit = timeUnit;
    }

    public MThreadPoolExecutor(int maxThreadSize, int maxCoreThreadSize, BlockingQueue<Runnable> taskQueue,
                               long keepAliveTime, TimeUnit timeUnit, MRejectedExecutionHandler rejectedExecutionHandler,
                               ThreadFactory threadFactory) {
        this(maxThreadSize, maxCoreThreadSize, taskQueue, keepAliveTime, timeUnit);

        this.rejectedExecutionHandler = rejectedExecutionHandler;
        this.threadFactory = threadFactory;
    }

    @Override
    public void execute(Runnable task) {
        if(task == null) {
            throw new IllegalArgumentException("task is null");
        }
        MReentrantLock mainLock = thisLock;
        for(;;) {
            //已调用shutdown()或stop()
            if (!isRunning()) {
                reject(task);
            }
            //判断线程数量，如果线程数量小于coreThreadSize，就创建核心线程来处理任务
            //需要保证threadCount不超过maxCoreThreadSize，使用双重检查锁定
            if (workerCount.get() < maxCoreThreadSize) {
                try {
                    thisLock.lock();
                    if(workerCount.get() < maxCoreThreadSize) {
                        addWorker(task, true);
                        return;
                    } else {
                        continue;
                    }
                } finally {
                    thisLock.unlock();
                }
            }
            //运行到此处说明核心线程数已经达到最大值，无法创建核心线程，此时尝试加入任务队列taskQueue
            boolean res = taskQueue.offer(task);
            if(res) return;
            //如果加入任务队列失败，尝试创建非核心线程处理任务
            if (workerCount.get() < maxThreadSize) {
                addWorker(task, false);
            } else {    //线程数量达到最大，拒绝执行
                reject(task);
                return;
            }
        }

    }
    private boolean isRunning() {
        return state == RUNNING;
    }

    /**
     * 添加 Worker 线程，使用MReentrantLock保证workerHashSet操作的安全
     * @param task
     * @param isCoreThread
     */
    private void addWorker(Runnable task, boolean isCoreThread) {
        MReentrantLock mainLock = thisLock;
        Thread thread = null;
        boolean added = false;
        try {
            thisLock.lock();
            Worker worker = new Worker(task, isCoreThread);
            thread = new Thread(worker);

            workerHashSet.add(worker);
            //worker数量加1
            workerCount.increment();
            added = true;
        } finally {
            thisLock.unlock();
            if(added) {
                thread.start();
            }
        }

    }

    /**
     * 移除worker
     * @param worker
     */
    private void removeWorker(Worker worker) {
        try {
            thisLock.lock();
            workerHashSet.remove(worker);
            workerCount.decrease();
        } finally {
            thisLock.unlock();
        }
    }

    private void reject(Runnable runnable) {
        rejectedExecutionHandler.rejectExecution(runnable, this);
    }

    @Override
    public void stop() {
        state = STOP;
        //TODO：中断所有线程

    }

    @Override
    public void shutdown() {
        state = SHUTDOWN;
    }

    private class Worker implements Runnable {
        //是否是核心线程
        boolean isCoreWorker;
        //需要执行的任务
        Runnable task;

        public Worker(Runnable runnable, boolean coreWorker) {
            task = runnable;
            isCoreWorker = coreWorker;
        }

        @Override
        public void run() {
            //尝试从阻塞队列中获取任务
            while(isRunning()) {
                if(isCoreWorker) {
                    try {
                        if(task != null) {
                            task.run();
                            task = null;
                            continue;
                        }
                        Runnable taskInQueue = taskQueue.take();
                        taskInQueue.run();
                    } catch (InterruptedException e) {}

                } else {    //非核心线程
                    try {
                        Runnable task = taskQueue.poll(keepAliveTime, timeUnit);
                        if(task == null) {
                            //非核心线程空闲时间达到keepAliveTime，需要移除worker后关闭线程
                            removeWorker(this);
                            return;
                        }
                        task.run();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
    public static class AbortPolicy implements MRejectedExecutionHandler {
        @Override
        public void rejectExecution(Runnable runnable, MThreadPoolExecutor executor) {
            System.out.println(runnable + " was rejected by " + executor);
        }
    }

}
