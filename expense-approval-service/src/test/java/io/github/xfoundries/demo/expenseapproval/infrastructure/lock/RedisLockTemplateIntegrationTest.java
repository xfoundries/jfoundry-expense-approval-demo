package io.github.xfoundries.demo.expenseapproval.infrastructure.lock;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import io.github.xfoundries.demo.expenseapproval.boot.ExpenseApprovalApplication;
import io.github.xfoundries.demo.expenseapproval.support.PostgreSqlIntegrationTest;
import org.jfoundry.application.lock.DistributedLockUnavailableException;
import org.jfoundry.application.lock.LockOptions;
import org.jfoundry.application.lock.LockTemplate;
import org.jfoundry.infrastructure.lock.redisson.RedissonDistributedLockClient;
import org.junit.jupiter.api.Test;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(classes = ExpenseApprovalApplication.class)
class RedisLockTemplateIntegrationTest extends PostgreSqlIntegrationTest {

    private static final String LOCK_KEY = "expense-approval:test:monthly-limit";

    @Autowired LockTemplate applicationLocks;

    @Test
    void sameKeyIsMutuallyExclusiveAcrossTwoRedissonClients() throws Exception {
        RedissonClient secondRedisson = secondRedissonClient();
        LockTemplate secondLocks = new LockTemplate(new RedissonDistributedLockClient(secondRedisson));
        ExecutorService executor = Executors.newSingleThreadExecutor();
        CountDownLatch firstEntered = new CountDownLatch(1);
        CountDownLatch releaseFirst = new CountDownLatch(1);

        try {
            Future<?> holder = executor.submit(() -> {
                applicationLocks.execute(LOCK_KEY, waitUpToOneSecond(), () -> {
                    firstEntered.countDown();
                    await(releaseFirst);
                    return null;
                });
                return null;
            });
            assertThat(firstEntered.await(5, TimeUnit.SECONDS)).isTrue();

            assertThatThrownBy(() -> secondLocks.execute(LOCK_KEY, () -> null))
                    .isInstanceOf(DistributedLockUnavailableException.class);

            releaseFirst.countDown();
            holder.get(5, TimeUnit.SECONDS);
            assertThat(secondLocks.execute(LOCK_KEY, () -> "acquired")).isEqualTo("acquired");
        } finally {
            releaseFirst.countDown();
            executor.shutdownNow();
            secondRedisson.shutdown();
        }
    }

    private static LockOptions waitUpToOneSecond() {
        return LockOptions.builder().waitTime(Duration.ofSeconds(1)).build();
    }

    private static RedissonClient secondRedissonClient() {
        Config config = new Config();
        config.useSingleServer().setAddress("redis://" + REDIS.getHost() + ':' + REDIS.getMappedPort(6379));
        return Redisson.create(config);
    }

    private static void await(CountDownLatch latch) throws InterruptedException {
        if (!latch.await(5, TimeUnit.SECONDS)) {
            throw new IllegalStateException("Timed out waiting to release the first lock holder");
        }
    }
}
