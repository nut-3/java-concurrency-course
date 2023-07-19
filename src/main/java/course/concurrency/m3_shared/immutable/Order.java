package course.concurrency.m3_shared.immutable;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static course.concurrency.m3_shared.immutable.Order.Status.DELIVERED;
import static course.concurrency.m3_shared.immutable.Order.Status.NEW;

public final class Order {

    public enum Status {NEW, IN_PROGRESS, DELIVERED}

    private final long id;
    private final List<Item> items;
    private final PaymentInfo paymentInfo;
    private final boolean isPacked;
    private final Status status;

    private Order(long id, List<Item> items, PaymentInfo paymentInfo, boolean isPacked, Status status) {
        this.id = id;
        this.items = Objects.requireNonNullElse(items, List.of());
        this.paymentInfo = paymentInfo;
        this.isPacked = isPacked;
        this.status = status;
    }

    public Order(long id, List<Item> items) {
        this(id, items, null, false, NEW);
    }

    public boolean checkStatus() {
        return !items.isEmpty()
                && paymentInfo != null
                && isPacked
                && !DELIVERED.equals(status);
    }

    public long getId() {
        return id;
    }

    public List<Item> getItems() {
        return items.stream()
                .map(Item::copy)
                .collect(Collectors.toUnmodifiableList());
    }

    public PaymentInfo getPaymentInfo() {
        if (this.paymentInfo == null) {
            return null;
        }
        return paymentInfo.copy();
    }

    public Order withPaymentInfo(PaymentInfo paymentInfo) {
        return new Order(this.id, this.items, paymentInfo, this.isPacked, Status.IN_PROGRESS);
    }

    public boolean isPacked() {
        return isPacked;
    }

    public Order withPacked(boolean packed) {
        return new Order(this.id, this.items, this.paymentInfo, packed, Status.IN_PROGRESS);
    }

    public Status getStatus() {
        return status;
    }

    public Order withStatus(Status status) {
        return new Order(this.id, this.items, this.paymentInfo, this.isPacked, status);
    }
}
