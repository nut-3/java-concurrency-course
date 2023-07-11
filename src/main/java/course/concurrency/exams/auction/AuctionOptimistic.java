package course.concurrency.exams.auction;

import java.util.concurrent.atomic.AtomicStampedReference;

public class AuctionOptimistic implements Auction {

    private Notifier notifier;

    public AuctionOptimistic(Notifier notifier) {
        this.notifier = notifier;
    }

    private final AtomicStampedReference<Bid> latestBid = new AtomicStampedReference<>(null, 0);

    public boolean propose(Bid bid) {
        boolean bidSuccessful = true;
        latestBid.compareAndSet(null, bid, 0, 1);
        int[] stamp = new int[1];
        Bid prev;
        do {
            prev = latestBid.get(stamp);
            if (bid.getPrice() <= prev.getPrice()) {
                bidSuccessful = false;
                break;
            }
        } while (!latestBid.compareAndSet(prev, bid, stamp[0], stamp[0] + 1));
        if (bidSuccessful) {
            notifier.sendOutdatedMessage(prev);
        }
        return bidSuccessful;
    }

    public Bid getLatestBid() {
        return latestBid.getReference();
    }
}
