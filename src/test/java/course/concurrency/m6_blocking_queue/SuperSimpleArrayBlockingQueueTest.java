package course.concurrency.m6_blocking_queue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class SuperSimpleArrayBlockingQueueTest {

    private final Queue<Thread> threads = new ConcurrentLinkedQueue<>();
    private ExecutorService executorService;


    @BeforeEach
    public void beforeEach() {
        threads.clear();
        executorService = Executors.newFixedThreadPool(
                Runtime.getRuntime().availableProcessors() * 2,
                runnable -> {
                    Thread thread = new Thread(runnable);
                    threads.offer(thread);
                    return thread;
                });
    }

    @Test
    @DisplayName("dequeue() waits for enqueue()")
    void dequeueInEmptyQueue() {
        SuperSimpleArrayBlockingQueue<Integer> queue = new SuperSimpleArrayBlockingQueue<>(1);

        executorService.submit(queue::dequeue);

        while (!threads.isEmpty()) {
            assertThat(threads.poll())
                    .extracting(Thread::getState)
                    .isEqualTo(Thread.State.WAITING);
        }

        executorService.submit(() -> {
            try {
                queue.enqueue(1);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });

        while (threads.isEmpty()) {
            assertThat(threads.poll())
                    .extracting(Thread::getState)
                    .isEqualTo(Thread.State.TERMINATED);
        }

        executorService.shutdown();
        try {
            executorService.awaitTermination(5, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("enqueue() until full")
    void enqueueInFullQueue() {
        SuperSimpleArrayBlockingQueue<String> queue = new SuperSimpleArrayBlockingQueue<>(1);
        assertThatCode(() -> queue.enqueue("test"))
                .doesNotThrowAnyException();

        assertThat(queue.size())
                .isOne();

        executorService.submit(() -> {
            try {
                queue.enqueue("test2");
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });

        assertThat(queue.size())
                .isOne();

        while (!threads.isEmpty()) {
            Thread thread = threads.poll();
            assertThat(thread)
                    .extracting(Thread::getState)
                    .isEqualTo(Thread.State.WAITING);
            thread.interrupt();
        }

        executorService.shutdownNow();
        try {
            executorService.awaitTermination(5, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("enqueue() in parallel 1_000 elements")
    void enqueueInParallel() {
        SuperSimpleArrayBlockingQueue<Integer> queue = new SuperSimpleArrayBlockingQueue<>(1_000);
        CountDownLatch latch = new CountDownLatch(1);

        IntStream.range(0, 1_000).forEach(i ->
                executorService.submit(() -> {
                    try {
                        latch.await();
                        queue.enqueue(i);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }));

        latch.countDown();

        executorService.shutdown();
        try {
            executorService.awaitTermination(5, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        assertThat(queue.size())
                .isEqualTo(1_000);
    }

    @Test
    @DisplayName("Concurrent enqueue() and dequeue()")
    void concurrentEnqueueDequeue() {
        SuperSimpleArrayBlockingQueue<Integer> queue = new SuperSimpleArrayBlockingQueue<>(1_000);
        CountDownLatch latch = new CountDownLatch(1);

        IntStream.range(0, 200).forEach(i -> executorService.submit(() -> {
            try {
                queue.enqueue(i);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }));

        IntStream.range(0, 8).forEach(i -> executorService.submit(() -> {
            try {
                latch.await();
                for (int j = 0; j < 10_000; j++) {
                    if ((j + i) % 2 == 0) {
                        queue.dequeue();
                    } else {
                        queue.enqueue(j);
                    }
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }));

        latch.countDown();

        executorService.shutdown();

        try {
            executorService.awaitTermination(5, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        assertThat(queue.size())
                .isEqualTo(200);
//                .isZero();
    }
}