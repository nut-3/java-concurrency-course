package course.concurrency.m3_shared.immutable;

public final class PaymentInfo {

    public PaymentInfo copy() {
        return new PaymentInfo();
    }
}
