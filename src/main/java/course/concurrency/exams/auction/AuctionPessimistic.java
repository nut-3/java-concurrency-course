package course.concurrency.exams.auction;

public class AuctionPessimistic implements Auction {

    private final Object lock = new Object();
    private final Notifier notifier;

    public AuctionPessimistic(Notifier notifier) {
        this.notifier = notifier;
    }

    private volatile Bid latestBid;

    public boolean propose(Bid bid) {
        if (latestBid == null) {
            initializeBid(bid);
            return true;
        }
        if (bid.getPrice() > latestBid.getPrice() && updateBid(bid)) {
            notifier.sendOutdatedMessage(latestBid);
            return true;
        }
        return false;
    }

    public Bid getLatestBid() {
        return latestBid;
    }

    private void initializeBid(Bid bid) {
        synchronized (lock) {
            if (latestBid == null) {
                latestBid = bid;
            }
        }

    }

    private boolean updateBid(Bid bid) {
        synchronized (lock) {
            if (bid.getPrice() > latestBid.getPrice()) {
                latestBid = bid;
                return true;
            }
            return false;
        }
    }
}
