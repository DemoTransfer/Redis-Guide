import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

@Slf4j
public class RedisLock {

    private RedisTemplate redisTemplate;

    public RedisLock(RedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 默认超时时间3分钟
     */
    private static final long DEFAULT_LOCK_TIMEOUT = 3L * 1000 * 60;

    /**
     * 默认轮休获取锁间隔时间0.1秒
     */
    private static final long DEFAULT_ACQUIRE_RESOLUTION_MILLIS = 100L;

    /**
     * 获取锁的等待时间1s
     */
    private static final long ACQUIRE_TIMEOUT_IN_MILLIS = DEFAULT_ACQUIRE_RESOLUTION_MILLIS * 10;

    /**
     * 锁的key前缀
     */
    private static final String LOCK_KEY = "LOCK_%s";

    /**
     * 获取锁，默认等待时间1s，默认释放锁时间3分钟
     *
     * @param key 锁的key
     * @return Lock
     */
    public Lock lock(String key) {
        return lock(key, ACQUIRE_TIMEOUT_IN_MILLIS, DEFAULT_LOCK_TIMEOUT);
    }

    /**
     * 获取锁，默认等待时间1s
     *
     * @param lockKey 锁的key
     * @param timeout 自动释放锁的时间
     * @return Lock
     */
    public Lock lock(String lockKey, long timeout) {
        return lock(lockKey, ACQUIRE_TIMEOUT_IN_MILLIS, timeout);
    }

    /**
     * 获取锁
     *
     * @param key                    锁的key
     * @param acquireTimeoutInMillis 等待时间
     * @param lockExpiryInMillis     自动释放锁的时间
     * @return Lock
     */
    public Lock lock(String key, long acquireTimeoutInMillis, long lockExpiryInMillis) {
        if (lockExpiryInMillis <= 0) {
            throw new RuntimeException("The lock release time must be greater than zero");
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");
        String value = LocalDateTime.now().format(formatter);
        String lockKey = String.format(LOCK_KEY, key);
        long timeout = 0L;
        while (timeout < acquireTimeoutInMillis) {
            if (redisTemplate.opsForValue().setIfAbsent(lockKey, value) && redisTemplate.expire(lockKey, lockExpiryInMillis, TimeUnit.MILLISECONDS)) {
                return new Lock(lockKey);
            } else {
                //删除
                if (redisTemplate.getExpire(lockKey) < 0) {
                    redisTemplate.delete(lockKey);
                }
            }
            try {
                TimeUnit.MILLISECONDS.sleep(DEFAULT_ACQUIRE_RESOLUTION_MILLIS);
            } catch (InterruptedException e) {
                log.error("InterruptedException error", e);
            }
            timeout += DEFAULT_ACQUIRE_RESOLUTION_MILLIS;
        }
        //acquireTimeoutInMillis时间内获取不到锁，返回null
        return null;
    }

    /**
     * 解锁
     *
     * @param lock 锁
     */
    public void unlock(Lock lock) {
        if (lock != null) {
            redisTemplate.delete(lock.getLockKey());
        }
    }
}
