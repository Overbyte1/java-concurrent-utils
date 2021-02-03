package threadpool;

public interface MExecutorService extends MExecutor {
    /**
     * 停止接收任务，并且尝试中断所有线程
     */
    void stop();

    /**
     * 停止接收任务，但是任务队列中的任务都会执行完
     */
    void shutdown();
}
