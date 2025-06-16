package selsup.test.ctyptapi.token;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class RateLimiter {
    private final long windowNanos;
    private final int requestLimit;
    private final ConcurrentLinkedQueue<Long> requestTimestamps;
    private final Lock lock = new ReentrantLock();

    public RateLimiter(TimeUnit timeUnit, int requestLimit) {
        this.windowNanos = timeUnit.toNanos(1);
        this.requestLimit = requestLimit;
        this.requestTimestamps = new ConcurrentLinkedQueue<>();
    }

    public void acquire() throws InterruptedException {
        lock.lock();
        try {
            long now = System.nanoTime();
            // Удаляем устаревшие временные метки
            while (!requestTimestamps.isEmpty() && (now - requestTimestamps.peek()) > windowNanos) {
                requestTimestamps.poll();
            }

            // Ждем, если достигнут лимит
            while (requestTimestamps.size() >= requestLimit) {
                long oldest = requestTimestamps.peek();
                long waitNanos = windowNanos - (now - oldest);
                if (waitNanos > 0) {
                    long waitMillis = TimeUnit.NANOSECONDS.toMillis(waitNanos) + 1;
                    Thread.sleep(waitMillis);
                }
                now = System.nanoTime();
                while (!requestTimestamps.isEmpty() && (now - requestTimestamps.peek()) > windowNanos) {
                    requestTimestamps.poll();
                }
            }

            requestTimestamps.add(now);
        } finally {
            lock.unlock();
        }
    }
}

