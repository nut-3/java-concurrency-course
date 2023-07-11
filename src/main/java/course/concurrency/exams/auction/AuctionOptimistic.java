package course.concurrency.exams.auction;

import java.util.concurrent.atomic.AtomicReference;

public class AuctionOptimistic implements Auction {

    private Notifier notifier;

    public AuctionOptimistic(Notifier notifier) {
        this.notifier = notifier;
    }

    private final AtomicReference<Bid> latestBid = new AtomicReference<>(new Bid(-1L, null, -1L));

    public boolean propose(Bid bid) {
        Bid prev;
        do {
            prev = latestBid.get();
            if (bid.getPrice() <= prev.getPrice()) {
                return false;
            }
        } while (!trySet(prev, bid));
        return true;
    }

    public Bid getLatestBid() {
        return latestBid.get();
    }

    private boolean trySet(Bid prev, Bid bid) {
        boolean result = latestBid.compareAndSet(prev, bid);
        if (result) {
            notifier.sendOutdatedMessage(prev);
        }
        return result;
    }
}
