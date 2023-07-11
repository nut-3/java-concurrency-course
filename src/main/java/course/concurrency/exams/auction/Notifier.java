package course.concurrency.exams.auction;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

public class Notifier {

    private final ExecutorService executor = Executors.newFixedThreadPool(1_000);

    public Notifier() {
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
        executor.shutdownNow();
    }
}
