package course.concurrency.m3_shared.immutable;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiFunction;

public class OrderService {

    private final Map<Long, Order> currentOrders = new ConcurrentHashMap<>();
    private final AtomicLong nextId = new AtomicLong();
    private final Map<Long, ReentrantLock> deliveryLocks = new ConcurrentHashMap<>();

    public long createOrder(List<Item> items) {
        long id = nextId.getAndIncrement();
        Order order = new Order(id, items);
        deliveryLocks.put(id, new ReentrantLock());
        currentOrders.put(id, order);
        return id;
    }

    public void updatePaymentInfo(long orderId, PaymentInfo paymentInfo) {
        modifyOrder(orderId, Order::withPaymentInfo, paymentInfo);
    }

    public void setPacked(long orderId) {
        modifyOrder(orderId, Order::withPacked, true);
    }

    private <T> void modifyOrder(long orderId, BiFunction<Order, T, Order> operation, T parameter) {
        Order modifiedOrder = currentOrders.computeIfPresent(orderId, (id, order) -> operation.apply(order, parameter));
        if (modifiedOrder != null && modifiedOrder.checkStatus()) {
            ReentrantLock lock = deliveryLocks.get(orderId);
            lock.lock();
            Order orderToDeliver = currentOrders.get(orderId);
            if (orderToDeliver.checkStatus()) {
                deliver(orderToDeliver);
            }
            lock.unlock();
        }
    }

    private void deliver(Order orderToSend) {
        /* ... */
        currentOrders.compute(orderToSend.getId(), (id, order) -> order.withStatus(Order.Status.DELIVERED));
    }

    public boolean isDelivered(long orderId) {
        return currentOrders.get(orderId).getStatus().equals(Order.Status.DELIVERED);
    }
}
