package course.concurrency.m3_shared.immutable;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiFunction;

public class OrderService {

    private final Map<Long, Order> currentOrders = new ConcurrentHashMap<>();
    private final AtomicLong nextId = new AtomicLong();

    public long createOrder(List<Item> items) {
        long id = nextId.getAndIncrement();
        Order order = new Order(id, items);
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
        if (currentOrders.computeIfPresent(orderId, (id, order) -> operation.apply(order, parameter))
                .checkStatus()) {
            deliver(currentOrders.compute(orderId, (id, order) -> order.withStatus(Order.Status.SENT)));
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
