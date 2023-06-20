package course.concurrency.exams.auction;

public class Notifier {

    public void sendOutdatedMessage(Bid bid) {
        imitateSending();
    }

    private void imitateSending() {
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {}
    }

    public void shutdown() {}
}
