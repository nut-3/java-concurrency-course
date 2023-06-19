package course.concurrency.m2_async.cf.min_price;

import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class PriceAggregator {

    private static final Executor EXECUTOR = Executors.newCachedThreadPool();
    private PriceRetriever priceRetriever = new PriceRetriever();

    public void setPriceRetriever(PriceRetriever priceRetriever) {
        this.priceRetriever = priceRetriever;
    }

    private Collection<Long> shopIds = Set.of(10l, 45l, 66l, 345l, 234l, 333l, 67l, 123l, 768l);

    public void setShops(Collection<Long> shopIds) {
        this.shopIds = shopIds;
    }

    public double getMinPrice(long itemId) {
        var futures = shopIds.stream()
                .map(shopId -> CompletableFuture.supplyAsync(() -> priceRetriever.getPrice(itemId, shopId), EXECUTOR)
                        .exceptionally(th -> Double.NaN)
                        .completeOnTimeout(Double.NaN, 2_900, TimeUnit.MILLISECONDS))
                .toArray(CompletableFuture[]::new);
        CompletableFuture.allOf(futures).join();
        return Arrays.stream(futures)
                .map(future -> (Double) future.join())
                .filter(res -> !Double.isNaN(res))
                .min(Double::compareTo)
                .orElse(Double.NaN);
    }
}
