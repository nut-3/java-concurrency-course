package course.concurrency.exams.refactoring;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;

import java.util.List;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.toList;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class MountTableRefresherServiceTests {

    private MountTableRefresherService service;

    private Others.RouterStore routerStore;
    private Others.MountTableManager manager;
    private Others.LoadingCache routerClientsCache;

    @BeforeEach
    public void setUpStreams() {
        service = new MountTableRefresherService();
        service.setCacheUpdateTimeout(1000);
        routerStore = mock(Others.RouterStore.class);
        manager = mock(Others.MountTableManager.class);
        service.setRouterStore(routerStore);
        routerClientsCache = mock(Others.LoadingCache.class);
        service.setRouterClientsCache(routerClientsCache);
        // service.serviceInit(); // needed for complex class testing, not for now
    }

    @AfterEach
    public void restoreStreams() {
        // service.serviceStop();
    }

    @Test
    @DisplayName("All tasks are completed successfully")
    public void allDone() {
        // given
        MountTableRefresherService mockedService = Mockito.spy(service);
        List<String> addresses = List.of("123", "local6", "789", "local");

        when(manager.refresh()).thenReturn(true);

        List<Others.RouterState> states = addresses.stream()
                .map(Others.RouterState::new).collect(toList());
        when(routerStore.getCachedRecords()).thenReturn(states);
        // smth more
        when(mockedService.getManager(anyString())).thenReturn(manager);

        // when
        mockedService.refresh();

        // then
        verify(mockedService).log(String.format("Mount table entries cache refresh successCount=%s,failureCount=0", addresses.size()));
        verify(routerClientsCache, never()).invalidate(anyString());
    }

    @Test
    @DisplayName("All tasks failed")
    public void noSuccessfulTasks() {
        // given
        MountTableRefresherService mockedService = Mockito.spy(service);
        List<String> addresses = List.of("123", "local6", "789", "local");

        when(manager.refresh()).thenReturn(false);

        List<Others.RouterState> states = addresses.stream()
                .map(Others.RouterState::new).collect(toList());
        when(routerStore.getCachedRecords()).thenReturn(states);
        // smth more
        when(mockedService.getManager(anyString())).thenReturn(manager);

        // when
        mockedService.refresh();

        // then
        verify(mockedService).log(String.format("Mount table entries cache refresh successCount=0,failureCount=%s", addresses.size()));
        verify(routerClientsCache, times(addresses.size())).invalidate(anyString());
    }

    @Test
    @DisplayName("Some tasks failed")
    public void halfSuccessedTasks() {
        // given
        MountTableRefresherService mockedService = Mockito.spy(service);
        List<String> addresses = List.of("123", "local6", "789", "local");
        int halfOfAddresses = addresses.size() / 2;

        AtomicLong managerCounter = new AtomicLong();
        when(manager.refresh()).thenAnswer(invocationOnMock -> managerCounter.getAndIncrement() < halfOfAddresses);

        List<Others.RouterState> states = addresses.stream()
                .map(Others.RouterState::new).collect(toList());
        when(routerStore.getCachedRecords()).thenReturn(states);
        // smth more
        when(mockedService.getManager(anyString())).thenReturn(manager);

        // when
        mockedService.refresh();

        // then
        verify(mockedService).log(String.format("Mount table entries cache refresh successCount=%s,failureCount=%s",
                halfOfAddresses, halfOfAddresses));
        verify(routerClientsCache, times(halfOfAddresses)).invalidate(anyString());
    }

    @Test
    @DisplayName("One task completed with exception")
    public void exceptionInOneTask() {
        // given
        MountTableRefresherService mockedService = Mockito.spy(service);
        List<String> addresses = List.of("123", "local6", "789", "local");

        when(manager.refresh()).thenReturn(true);

        List<Others.RouterState> states = addresses.stream()
                .map(Others.RouterState::new).collect(toList());
        when(routerStore.getCachedRecords()).thenReturn(states);
        // smth more
        when(mockedService.getManager(anyString())).thenReturn(manager);

        doAnswer(invocationOnMock -> CompletableFuture.failedFuture(new RuntimeException()))
                .doAnswer(InvocationOnMock::callRealMethod)
                .when(mockedService).refresherToFuture(notNull());

        // when
        mockedService.refresh();

        // then
        verify(mockedService).log("Some cache updates completed with error");
        verify(mockedService).log(String.format("Mount table entries cache refresh successCount=%s,failureCount=1", addresses.size() - 1));
        verify(routerClientsCache, times(1)).invalidate(anyString());
    }

    @Test
    @DisplayName("One task exceeds timeout")
    public void oneTaskExceedTimeout() {
        // given
        MountTableRefresherService mockedService = Mockito.spy(service);
        List<String> addresses = List.of("123", "local6", "789", "local");

        when(manager.refresh()).thenReturn(true);

        List<Others.RouterState> states = addresses.stream()
                .map(Others.RouterState::new).collect(toList());
        when(routerStore.getCachedRecords()).thenReturn(states);
        // smth more
        when(mockedService.getManager(anyString())).thenReturn(manager);

        doAnswer(invocationOnMock -> CompletableFuture.runAsync(() -> {
                            try {
                                Thread.sleep(5);
                            } catch (InterruptedException e) {
                                throw new RuntimeException(e);
                            }
                        })
                        .orTimeout(1, TimeUnit.MILLISECONDS)
        )
                .doAnswer(InvocationOnMock::callRealMethod)
                .when(mockedService).refresherToFuture(notNull());

        // when
        mockedService.refresh();

        // then
        verify(mockedService).log("Some cache updates timed out");
        verify(mockedService).log(String.format("Mount table entries cache refresh successCount=%s,failureCount=1", addresses.size() - 1));
        verify(routerClientsCache, times(1)).invalidate(anyString());
    }

    @Test
    @DisplayName("Several refresh() invocations from different threads")
    public void severalInvocations() {
        // given
        MountTableRefresherService mockedService = Mockito.spy(service);
        List<String> addresses = List.of("123", "local6", "789", "local");

        when(manager.refresh()).thenReturn(true);

        List<Others.RouterState> states = addresses.stream()
                .map(Others.RouterState::new).collect(toList());
        when(routerStore.getCachedRecords()).thenReturn(states);
        // smth more
        when(mockedService.getManager(anyString())).thenReturn(manager);

        // when
        CyclicBarrier barrier = new CyclicBarrier(5);
        CompletableFuture.allOf(
                IntStream.range(0, 5).mapToObj(i ->
                                CompletableFuture.runAsync(() -> {
                                    try {
                                        barrier.await();
                                    } catch (InterruptedException | BrokenBarrierException e) {
                                        throw new RuntimeException(e);
                                    }
                                    mockedService.refresh();
                                }))
                        .toArray(CompletableFuture[]::new)
        ).join();

        // then
        verify(mockedService, times(1)).invokeRefresh(anyList());
        verify(mockedService, times(1)).log(String.format("Mount table entries cache refresh successCount=%s,failureCount=0", addresses.size()));
        verify(routerClientsCache, never()).invalidate(anyString());
    }
}
