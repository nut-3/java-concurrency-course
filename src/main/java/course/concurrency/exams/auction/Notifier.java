package course.concurrency.exams.auction;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Notifier {

    private final ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

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
        try {
            executor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException ignored) {
        }
        executor.shutdownNow();
    }
}
