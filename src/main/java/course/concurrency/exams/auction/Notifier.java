package course.concurrency.exams.auction;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;

public class Notifier {

    private static final ThreadFactory THREAD_FACTORY = Executors.defaultThreadFactory();
    private final ExecutorService executor;

    public Notifier() {
        this.executor = Executors.newFixedThreadPool(1_000, r -> {
            Thread thread = THREAD_FACTORY.newThread(r);
            thread.setPriority(Thread.MIN_PRIORITY);
            return thread;
        });
        ((ThreadPoolExecutor) this.executor).prestartAllCoreThreads();
    }


    public void sendOutdatedMessage(Bid bid) {
        executor.submit(this::imitateSending);
    }

    private void imitateSending() {
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
        }
    }

    public void shutdown() {
        executor.shutdown();
        ((ThreadPoolExecutor) executor).getQueue().clear();
    }
}
