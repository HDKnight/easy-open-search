package org.dromara.easyos.toolkit;

import java.security.SecureRandom;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

/**
 * RFC 9562 UUIDv7：48-bit 毫秒时间戳 + 随机位，同一毫秒内单调递增。
 */
public final class UuidV7 {
    private static final SecureRandom RANDOM = new SecureRandom();
    /** 高 48 位时间戳 + 低 12 位同毫秒序列，保证有序。 */
    private static final AtomicLong LAST_TIMESTAMP_AND_SEQ = new AtomicLong();

    private UuidV7() {
    }

    public static UUID nextUuid() {
        long timestampAndSeq = nextTimestampAndSeq();
        long timestamp = timestampAndSeq >>> 12;
        long seq = timestampAndSeq & 0xFFFL;

        long msb = (timestamp << 16) | (0x7L << 12) | seq;
        long lsb = RANDOM.nextLong();
        // RFC variant: 10xxxxxx
        lsb = (lsb & 0x3FFFFFFFFFFFFFFFL) | 0x8000000000000000L;
        return new UUID(msb, lsb);
    }

    public static String next() {
        return nextUuid().toString();
    }

    private static long nextTimestampAndSeq() {
        while (true) {
            long now = System.currentTimeMillis();
            long previous = LAST_TIMESTAMP_AND_SEQ.get();
            long previousTs = previous >>> 12;
            long previousSeq = previous & 0xFFFL;

            long nextTs;
            long nextSeq;
            if (now > previousTs) {
                nextTs = now;
                nextSeq = RANDOM.nextInt(1 << 12) & 0xFFFL;
            } else if (now == previousTs) {
                nextTs = previousTs;
                nextSeq = (previousSeq + 1) & 0xFFFL;
                if (nextSeq == 0) {
                    // 同毫秒序列耗尽，等到下一毫秒
                    nextTs = waitNextMillis(previousTs);
                    nextSeq = RANDOM.nextInt(1 << 12) & 0xFFFL;
                }
            } else {
                // 时钟回拨：继续沿用 previousTs 递增序列
                nextTs = previousTs;
                nextSeq = (previousSeq + 1) & 0xFFFL;
                if (nextSeq == 0) {
                    nextTs = previousTs + 1;
                    nextSeq = RANDOM.nextInt(1 << 12) & 0xFFFL;
                }
            }

            long next = (nextTs << 12) | nextSeq;
            if (LAST_TIMESTAMP_AND_SEQ.compareAndSet(previous, next)) {
                return next;
            }
        }
    }

    private static long waitNextMillis(long previousTs) {
        long now;
        do {
            now = System.currentTimeMillis();
        } while (now <= previousTs);
        return now;
    }
}
