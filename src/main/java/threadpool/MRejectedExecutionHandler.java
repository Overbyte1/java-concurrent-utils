package threadpool;

public interface MRejectedExecutionHandler {
    void rejectExecution(Runnable runnable, MThreadPoolExecutor executor);
}
