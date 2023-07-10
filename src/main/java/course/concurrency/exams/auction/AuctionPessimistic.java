package course.concurrency.exams.auction;

public class AuctionPessimistic implements Auction {

    private final Object lock = new Object();
    private final Notifier notifier;

    public AuctionPessimistic(Notifier notifier) {
        this.notifier = notifier;
    }

    private Bid latestBid;

    public boolean propose(Bid bid) {
        synchronized (lock) {
            if (latestBid == null || bid.getPrice() > latestBid.getPrice()) {
                latestBid = bid;
                notifier.sendOutdatedMessage(latestBid);
                return true;
            }
            return false;
        }
    }

    public Bid getLatestBid() {
        synchronized (lock) {
            return latestBid;
        }
    }
}
