package course.concurrency.exams.auction;

import java.util.concurrent.atomic.AtomicMarkableReference;

public class AuctionStoppableOptimistic implements AuctionStoppable {

    private Notifier notifier;
    private final AtomicMarkableReference<Bid> latestBid =
            new AtomicMarkableReference<>(new Bid(-1L, null, -1L), false);

    public AuctionStoppableOptimistic(Notifier notifier) {
        this.notifier = notifier;
    }


    public boolean propose(Bid bid) {
        if (latestBid.isMarked()) {
            return false;
        }
        Bid prev;
        boolean[] mark = new boolean[1];
        do {
            prev = latestBid.get(mark);
            if (mark[0] || bid.getPrice() <= prev.getPrice()) {
                return false;
            }
        } while (!latestBid.compareAndSet(prev, bid, false, false));
        if (!mark[0]) {
            notifier.sendOutdatedMessage(prev);
        }
        return !mark[0];
    }

    public Bid getLatestBid() {
        return latestBid.getReference();
    }

    public Bid stopAuction() {
        notifier.shutdown();
        Bid prev;
        do {
            prev = latestBid.getReference();
        } while (!latestBid.attemptMark(prev, true));
        return prev;
    }
}
