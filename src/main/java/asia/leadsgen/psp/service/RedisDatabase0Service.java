package asia.leadsgen.psp.service;

import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import org.springframework.data.redis.core.RedisTemplate;

public class RedisDatabase0Service {

	private static RedisTemplate redisTemplateDatabase0;

	private static long redisDefaultExprireTime;

	private static TimeUnit defaultTimeUnit = TimeUnit.MILLISECONDS;

	public static void delete(String key) {
		redisTemplateDatabase0.expire(key, 0, defaultTimeUnit);
	}

	public static void setRedisDefaultExprireTime(long redisDefaultExprireTime) {
		RedisDatabase0Service.redisDefaultExprireTime = redisDefaultExprireTime;
	}

	public static RedisTemplate getRedisTemplateDatabase0() {
		return redisTemplateDatabase0;
	}

	public static void setRedisTemplateDatabase0(RedisTemplate redisTemplateDatabase0) {
		RedisDatabase0Service.redisTemplateDatabase0 = redisTemplateDatabase0;
	}

	public static long getRedisDefaultExprireTime() {
		return redisDefaultExprireTime;
	}

	public static Object getObject(String key) {
		return redisTemplateDatabase0.opsForValue().get(key);
	}

	public static Object persist(String key, Object data) {
		redisTemplateDatabase0.opsForValue().set(key, data);
		redisTemplateDatabase0.persist(key);
		return redisTemplateDatabase0.opsForValue().get(key);
	}

	private static final Logger LOGGER = Logger.getLogger(RedisDatabase0Service.class.getName());
}
