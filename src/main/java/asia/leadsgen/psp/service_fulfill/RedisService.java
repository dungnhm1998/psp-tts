package asia.leadsgen.psp.service_fulfill;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import org.springframework.data.redis.core.RedisTemplate;

import asia.leadsgen.psp.data.type.RedisKeyEnum;

public class RedisService {

	private static RedisTemplate redisTemplate;

	private static long redisDefaultExprireTime;

	private static TimeUnit defaultTimeUnit = TimeUnit.MILLISECONDS;

	public static void setRedisDefaultExprireTime(long redisDefaultExprireTime) {
		RedisService.redisDefaultExprireTime = redisDefaultExprireTime;
	}

	public static Map getMap(RedisKeyEnum redisKey) {
		return (Map) redisTemplate.opsForValue().get(redisKey.getValue());
	}

	public static Map persistMap(RedisKeyEnum redisKey, Map data) {
		redisTemplate.opsForValue().set(redisKey.getValue(), data);
		redisTemplate.persist(redisKey.getValue());
		return (Map) redisTemplate.opsForValue().get(redisKey.getValue());
	}

	public static void setRedisTemplate(RedisTemplate redisTemplate) {
		RedisService.redisTemplate = redisTemplate;
	}

	public static Map save(String key, Map data) {
		return save(key, data, redisDefaultExprireTime, defaultTimeUnit);
	}

	public static Map save(String key, Map data, long expireTime, TimeUnit timeUnit) {
		redisTemplate.opsForValue().set(key, data);
		redisTemplate.expire(key, expireTime, timeUnit);
		return (Map) redisTemplate.opsForValue().get(key);
	}

	public static Map get(String key) {
		return (Map) redisTemplate.opsForValue().get(key);
	}

	public static Object getObject(String key) {
		return redisTemplate.opsForValue().get(key);
	}

	public static Map persist(String key, Map data) {
		redisTemplate.opsForValue().set(key, data);
		redisTemplate.persist(key);
		return (Map) redisTemplate.opsForValue().get(key);
	}

	public static Object persist(String key, Object data) {
		redisTemplate.opsForValue().set(key, data);
		redisTemplate.persist(key);
		return redisTemplate.opsForValue().get(key);
	}

	public static Map update(String key, Map data) {
		LOGGER.info("Redis update cache get(key): " + get(key));
		if (get(key) == null || get(key).isEmpty()) {
			return save(key, data);
		}

		long expire = redisTemplate.getExpire(key, defaultTimeUnit);
		LOGGER.info("Redis cache expire: " + expire);
		return save(key, data, expire, defaultTimeUnit);
	}

	public static void delete(String key) {
		redisTemplate.expire(key, 0, defaultTimeUnit);
//		redisTemplate.delete(key);
	}

	public static Set<String> getKeysMatch(String prefix) {
		return redisTemplate.keys(prefix + "*");
	}

	private static final Logger LOGGER = Logger.getLogger(RedisService.class.getName());
}
