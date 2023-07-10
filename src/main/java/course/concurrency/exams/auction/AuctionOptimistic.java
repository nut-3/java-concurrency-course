package course.concurrency.exams.auction;

import java.util.concurrent.atomic.AtomicReference;

public class AuctionOptimistic implements Auction {

    private Notifier notifier;

    public AuctionOptimistic(Notifier notifier) {
        this.notifier = notifier;
    }

    private final AtomicReference<Bid> latestBid = new AtomicReference<>();

    public boolean propose(Bid bid) {
        Bid[] prevBid = new Bid[1];
        boolean bidSuccessful = latestBid.accumulateAndGet(bid, (prev, curr) -> {
            if (prev == null || curr.getPrice() > prev.getPrice()) {
                prevBid[0] = prev;
                return curr;
            }
            return prev;
        }).equals(bid);
        if (bidSuccessful) {
            notifier.sendOutdatedMessage(prevBid[0]);
        }
        return bidSuccessful;
    }

    public Bid getLatestBid() {
        return latestBid.get();
    }
}
