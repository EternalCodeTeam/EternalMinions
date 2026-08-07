package com.eternalcode.minions.minion;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class MinionIdSequenceTest {

    @Test
    void shouldReturnStrictlyIncreasingIdsOnSingleThread() {
        MinionIdSequence sequence = new MinionIdSequence();

        long first = sequence.next().value();
        long second = sequence.next().value();
        long third = sequence.next().value();

        assertThat(second).isGreaterThan(first);
        assertThat(third).isGreaterThan(second);
    }

    /**
     * MinionManagementService (the public API) can be invoked by other plugins from arbitrary
     * threads, but MinionIdSequence.next() increments a plain, non-volatile long with no
     * synchronization. This reproduces the resulting lost-update race: concurrent callers can
     * receive the same id, which downstream fails minion creation with a duplicate primary key.
     */
    @Test
    void shouldNotHandOutDuplicateIdsUnderConcurrentAccess() throws InterruptedException {
        MinionIdSequence sequence = new MinionIdSequence();
        int threads = 16;
        int idsPerThread = 2_000;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch start = new CountDownLatch(1);
        Set<Long> issuedIds = ConcurrentHashMap.newKeySet(threads * idsPerThread);
        AtomicInteger duplicates = new AtomicInteger();

        for (int t = 0; t < threads; t++) {
            executor.submit(() -> {
                ready.countDown();
                try {
                    start.await();
                }
                catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    return;
                }
                for (int i = 0; i < idsPerThread; i++) {
                    long id = sequence.next().value();
                    if (!issuedIds.add(id)) {
                        duplicates.incrementAndGet();
                    }
                }
            });
        }

        ready.await();
        start.countDown();
        executor.shutdown();
        assertThat(executor.awaitTermination(30, TimeUnit.SECONDS)).isTrue();

        assertThat(duplicates.get())
                .as("MinionIdSequence.next() must never hand out the same id twice under concurrent access")
                .isZero();
        assertThat(issuedIds).hasSize(threads * idsPerThread);
    }
}
