package threadpool;


import org.junit.Test;

import java.util.concurrent.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class MThreadPoolExecutorTest {
    @Test
    public void testShutdown() throws InterruptedException {
        MThreadPoolExecutor threadPoolExecutor = new MThreadPoolExecutor(8, 5, new ArrayBlockingQueue<>(10));
        Runnable task = () -> {
            System.out.println(Thread.currentThread());
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        };

        for(int i = 0; i < 50; i++) {
            threadPoolExecutor.execute(task);
        }

        Thread.sleep(10000);
    }
    @Test
    public void testThreadPoolExecutor() throws InterruptedException {
//        ExecutorService executorService = new ThreadPoolExecutor(5, 10, 0,
//                TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(10));
//        //executorService.awaitTermination()
//        Future<?> future = executorService.submit(() -> {
//        });
//        ThreadPoolExecutor threadPoolExecutor = null;
        //threadPoolExecutor.shutdownNow()
        ReentrantLock reentrantLock = new ReentrantLock();
        reentrantLock.lock();
        Thread t = new Thread(() -> {reentrantLock.lock();});
        t.start();
        Condition condition = reentrantLock.newCondition();
        condition.await();
        Thread.sleep(1000000);
        reentrantLock.unlock();
        t.join();

    }
}