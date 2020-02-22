
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;

@Configuration
public class LockConfig {

    @Bean
    public RedisLock redisLock(RedisTemplate<String, Object> redisTemplate) {
        return new RedisLock(redisTemplate);
    }
	
}
