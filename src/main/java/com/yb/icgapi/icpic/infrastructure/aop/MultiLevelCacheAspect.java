package com.yb.icgapi.icpic.infrastructure.aop;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.RandomUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.yb.icgapi.icpic.infrastructure.annotation.CacheKeyIgnore;
import com.yb.icgapi.icpic.infrastructure.annotation.MultiLevelCache;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;

import javax.annotation.Resource;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Aspect
@Component
@Slf4j
public class MultiLevelCacheAspect {

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    // Jackson ObjectMapper for JSON serialization/deserialization
    @Resource
    private ObjectMapper objectMapper;

    // 本地缓存,Caffeine实现
    private final Cache<String,Object> LOCAL_CACHE = Caffeine.newBuilder()
            .maximumSize(10_000L) // 设置最大缓存条目数
            // 缓存 5 分钟后移除
            .expireAfterWrite(Duration.ofMinutes(5))
            .build();

    @Around("@annotation(multiLevelCache)")
    public Object doCache(ProceedingJoinPoint joinPoint, MultiLevelCache multiLevelCache) throws Throwable {
        // 1. 获取方法签名和返回类型，为后续反序列化作准备
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        // 获取带有泛型信息的返回类型
        Type returnType = method.getGenericReturnType();

        // 2. 构建缓存Key (已包含忽略@CacheKeyIgnore参数的逻辑)
        Object[] args = joinPoint.getArgs();
        Annotation[][] parameterAnnotations = method.getParameterAnnotations();
        List<Object> cacheableArgs = new ArrayList<>();
        for (int i = 0; i < args.length; i++) {
            boolean ignore = false;
            for (Annotation annotation : parameterAnnotations[i]) {
                if (annotation.annotationType().equals(CacheKeyIgnore.class)) {
                    ignore = true;
                    break;
                }
            }
            if (!ignore) {
                cacheableArgs.add(args[i]);
            }
        }
        String keyPrefix = multiLevelCache.KeyPrefix();
        String argsJson = cacheableArgs.isEmpty() ? "" : objectMapper.writeValueAsString(cacheableArgs);
        log.info("argsJson:{}", argsJson);
        String hashKey = DigestUtils.md5DigestAsHex(argsJson.getBytes());
        String cacheKey = String.format("%s:%s", keyPrefix, hashKey);


        // 3. 查本地缓存
        Object cachedResult = LOCAL_CACHE.getIfPresent(cacheKey);
        if (cachedResult != null) {
            log.info("缓存命中:[Local] key: {}", cacheKey);
            return cachedResult; // 直接返回对象
        }

        // 4. 查Redis缓存
        Object redisCache = redisTemplate.opsForValue().get(cacheKey);
        if (redisCache != null) {
            log.info("缓存命中:[Redis] key: {}", cacheKey);

            // ========== 核心修改点 ==========
            // 将从Redis获取的LinkedHashMap转换为方法实际的返回类型
            cachedResult = objectMapper.convertValue(redisCache, objectMapper.constructType(returnType));
            // ================================

            LOCAL_CACHE.put(cacheKey, cachedResult); // 放入本地缓存
            return cachedResult; // 返回转换后、类型正确的对象
        }

        // 5. 缓存未命中,执行原方法
        log.info("缓存未命中,执行原方法: {}", signature.getMethod().getName());
        Object dbResult = joinPoint.proceed();

        // 6. 将结果写入Redis和本地缓存
        if (ObjectUtil.isNotEmpty(dbResult)) {
            int baseExpireTime = multiLevelCache.expireTime();
            int randomRange = multiLevelCache.randomRange();
            // 直接存对象，RedisTemplate会用配置好的Jackson序列化器处理
            redisTemplate.opsForValue().set(cacheKey, dbResult, Duration.ofSeconds(baseExpireTime + RandomUtil.randomInt(0, randomRange)));
            LOCAL_CACHE.put(cacheKey, dbResult); // 本地也存对象
            log.info("缓存已更新, key: {}", cacheKey);
        }
        return dbResult;
    }
}
