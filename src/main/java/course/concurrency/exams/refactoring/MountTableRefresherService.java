package course.concurrency.exams.refactoring;

import org.springframework.util.StringUtils;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;


public class MountTableRefresherService {

    private final AtomicLong threadCounter = new AtomicLong();
    private final ExecutorService mountTableRefresherExecutor = Executors.newFixedThreadPool(
            Runtime.getRuntime().availableProcessors(),
            runnable -> {
                Thread thread = new Thread(runnable);
                thread.setName("MountTableRefresh_" + threadCounter.incrementAndGet());
                thread.setDaemon(true);
                return thread;
            });
    private final AtomicBoolean cacheUpdateInProgressIndicator = new AtomicBoolean();
    private Others.RouterStore routerStore = new Others.RouterStore();
    private long cacheUpdateTimeout;

    /**
     * All router admin clients cached. So no need to create the client again and
     * again. Router admin address(host:port) is used as key to cache RouterClient
     * objects.
     */
    private Others.LoadingCache<String, Others.RouterClient> routerClientsCache;

    /**
     * Removes expired RouterClient from routerClientsCache.
     */
    private ScheduledExecutorService clientCacheCleanerScheduler;

    public void serviceInit() {
        long routerClientMaxLiveTime = 15L;
        this.cacheUpdateTimeout = 10L;
        routerClientsCache = new Others.LoadingCache<String, Others.RouterClient>();
        routerStore.getCachedRecords().stream().map(Others.RouterState::getAdminAddress)
                .forEach(addr -> routerClientsCache.add(addr, new Others.RouterClient()));

        initClientCacheCleaner(routerClientMaxLiveTime);
    }

    public void serviceStop() {
        clientCacheCleanerScheduler.shutdown();
        // remove and close all admin clients
        routerClientsCache.cleanUp();
    }

    private void initClientCacheCleaner(long routerClientMaxLiveTime) {
        ThreadFactory tf = runnable -> {
            Thread t = new Thread(runnable);
            t.setName("MountTableRefresh_ClientsCacheCleaner");
            t.setDaemon(true);
            return t;
        };

        clientCacheCleanerScheduler =
                Executors.newSingleThreadScheduledExecutor(tf);
        /*
         * When cleanUp() method is called, expired RouterClient will be removed and
         * closed.
         */
        clientCacheCleanerScheduler.scheduleWithFixedDelay(
                () -> routerClientsCache.cleanUp(), routerClientMaxLiveTime,
                routerClientMaxLiveTime, TimeUnit.MILLISECONDS);
    }

    /**
     * Refresh mount table cache of this router as well as all other routers.
     */
    public void refresh() {
        if (!cacheUpdateInProgressIndicator.compareAndSet(false, true)) {
            return;
        }
        try {
            List<MountTableRefresher> refreshers = routerStore.getCachedRecords().stream()
                    .map(Others.RouterState::getAdminAddress)
                    .filter(StringUtils::hasText)
                    .map(this::getRefresher)
                    .collect(Collectors.toUnmodifiableList());
            if (!refreshers.isEmpty()) {
                invokeRefresh(refreshers);
            }
        } finally {
            cacheUpdateInProgressIndicator.set(false);
        }
    }

    protected MountTableRefresher getRefresher(String adminAddress) {
        return new MountTableRefresher(getManager(adminAddress), adminAddress);
    }

    protected Others.MountTableManager getManager(String adminAddress) {
        if (isLocalAdmin(adminAddress)) {
            return new Others.MountTableManager("local");
        }
        return new Others.MountTableManager(adminAddress);
    }

    private void removeFromCache(String adminAddress) {
        routerClientsCache.invalidate(adminAddress);
    }

    protected void invokeRefresh(List<MountTableRefresher> refreshers) {
        try {
            CompletableFuture.allOf(refreshers.stream()
                            .map(this::refresherToFuture)
                            .toArray(CompletableFuture[]::new))
                    .get();
        } catch (ExecutionException e) {
            if (e.getCause() instanceof TimeoutException) {
                log("Some cache updates timed out");
            } else {
                log("Some cache updates completed with error");
            }
        } catch (InterruptedException e) {
            log("Mount table cache refresher was interrupted.");
        }
        logResult(refreshers);
    }

    protected CompletableFuture<Void> refresherToFuture(MountTableRefresher refresher) {
        return CompletableFuture.runAsync(refresher::refresh, mountTableRefresherExecutor)
                .orTimeout(cacheUpdateTimeout, TimeUnit.MILLISECONDS);
    }

    private boolean isLocalAdmin(String adminAddress) {
        return adminAddress.contains("local");
    }

    private void logResult(List<MountTableRefresher> refreshers) {
        int successCount = 0;
        int failureCount = 0;
        for (MountTableRefresher mountTableRefresher : refreshers) {
            if (mountTableRefresher.isSuccess()) {
                successCount++;
            } else {
                failureCount++;
                // remove RouterClient from cache so that new client is created
                removeFromCache(mountTableRefresher.getAdminAddress());
            }
        }
        log(String.format(
                "Mount table entries cache refresh successCount=%d,failureCount=%d",
                successCount, failureCount));
    }

    public void log(String message) {
        System.out.println(message);
    }

    public void setCacheUpdateTimeout(long cacheUpdateTimeout) {
        this.cacheUpdateTimeout = cacheUpdateTimeout;
    }

    public void setRouterClientsCache(Others.LoadingCache cache) {
        this.routerClientsCache = cache;
    }

    public void setRouterStore(Others.RouterStore routerStore) {
        this.routerStore = routerStore;
    }
}