package course.concurrency.exams.auction;

import java.util.concurrent.atomic.AtomicReference;

public class AuctionStoppableOptimistic implements AuctionStoppable {

    private Notifier notifier;
    private AtomicReference<Bid> latestBid = new AtomicReference<>(new Bid(-1L, null, -1L));
    private volatile boolean stopped = false;

    public AuctionStoppableOptimistic(Notifier notifier) {
        this.notifier = notifier;
    }


    public boolean propose(Bid bid) {
        if (stopped) {
            return false;
        }
        Bid prev;
        do {
            prev = latestBid.get();
            if (bid.getPrice() <= prev.getPrice()) {
                return false;
            }
        } while (!stopped && !latestBid.compareAndSet(prev, bid));
        if (!stopped) {
            notifier.sendOutdatedMessage(prev);
        }
        return !stopped;
    }

    public Bid getLatestBid() {
        return latestBid.get();
    }

    public Bid stopAuction() {
        stopped = true;
        notifier.shutdown();
        return latestBid.get();
    }
}
