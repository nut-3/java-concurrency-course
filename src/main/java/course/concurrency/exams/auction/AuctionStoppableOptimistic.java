package course.concurrency.exams.auction;

import java.util.concurrent.atomic.AtomicMarkableReference;

public class AuctionStoppableOptimistic implements AuctionStoppable {

    private final Notifier notifier;
    private final AtomicMarkableReference<Bid> latestBid =
            new AtomicMarkableReference<>(new Bid(-1L, null, -1L), false);
    private final Object lock = new Object();
    private volatile boolean stopInitiated = false;

    public AuctionStoppableOptimistic(Notifier notifier) {
        this.notifier = notifier;
    }


    public boolean propose(Bid bid) {
        if (latestBid.isMarked()) {
            return false;
        }

        Bid prev;
        synchronized (stopInitiated ? lock : new Object()) {
            do {
                prev = latestBid.getReference();
                if (latestBid.isMarked() || bid.getPrice() <= prev.getPrice()) {
                    return false;
                }
            } while (!latestBid.compareAndSet(prev, bid, false, false));
        }

        notifier.sendOutdatedMessage(prev);
        return true;
    }

    @Override
    public String toString() {
        return super.toString();
    }

    public Bid getLatestBid() {
        return latestBid.getReference();
    }

    public Bid stopAuction() {
        stopInitiated = true;
        try {
            synchronized (lock) {
                notifier.shutdown();
                Bid prev;
                do {
                    prev = latestBid.getReference();
                } while (!latestBid.attemptMark(prev, true));
                return prev;
            }
        } finally {
            stopInitiated = false;
        }
    }
}
