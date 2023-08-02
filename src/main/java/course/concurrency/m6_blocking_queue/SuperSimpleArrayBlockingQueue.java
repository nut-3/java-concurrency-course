package course.concurrency.m6_blocking_queue;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class SuperSimpleArrayBlockingQueue<T> {

    private final ReentrantLock lock = new ReentrantLock();
    private final Condition full = lock.newCondition();
    private final Condition empty = lock.newCondition();
    private final Object[] store;
    private int head;
    private int tail;
    private final AtomicInteger count = new AtomicInteger();

    public SuperSimpleArrayBlockingQueue(int queueSize) {
        if (queueSize < 1) {
            throw new IllegalArgumentException("Queue size must be greater then 0");
        }
        this.store = new Object[queueSize];
        this.head = 0;
        this.tail = -1;
    }

    private int getNextIndex(int idx) {
        if (idx + 1 >= store.length) {
            return 0;
        }
        return idx + 1;
    }

    public void enqueue(T t) throws InterruptedException {
        lock.lock();
        try {
            while (count.get() + 1 > store.length) {
                full.await();
            }
            tail = getNextIndex(tail);
            store[tail] = t;
            count.incrementAndGet();
            empty.signal();
        } finally {
            lock.unlock();
        }
    }

    @SuppressWarnings("unchecked")
    public T dequeue() throws InterruptedException {
        lock.lock();
        try {
            while (count.get() == 0) {
                empty.await();
            }
            T item = (T) store[head];
            store[head] = null;
            head = getNextIndex(head);
            count.decrementAndGet();
            full.signal();
            return item;
        } finally {
            lock.unlock();
        }
    }

    public int size() {
        return count.get();
    }
}
