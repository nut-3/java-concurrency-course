package course.concurrency.exams.auction;

public class AuctionStoppablePessimistic implements AuctionStoppable {

    private final Object lock = new Object();
    private volatile Bid latestBid = new Bid(-1L, null, -1L);
    private volatile boolean stopped = false;
    private Notifier notifier;

    public AuctionStoppablePessimistic(Notifier notifier) {
        this.notifier = notifier;
    }


    public boolean propose(Bid bid) {
        if (stopped) {
            return false;
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

    public Bid stopAuction() {
        synchronized (lock) {
            stopped = true;
            notifier.shutdown();
            return latestBid;
        }
    }

    private boolean updateBid(Bid bid) {
        synchronized (lock) {
            if (!stopped && bid.getPrice() > latestBid.getPrice()) {
                latestBid = bid;
                return true;
            }
            return false;
        }
    }
}
