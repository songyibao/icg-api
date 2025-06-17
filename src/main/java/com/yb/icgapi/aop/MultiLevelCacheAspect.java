package com.yb.icgapi.aop;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.RandomUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.yb.icgapi.annotation.CacheKeyIgnore;
import com.yb.icgapi.annotation.MultiLevelCache;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;

import javax.annotation.Resource;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Aspect
@Component
@Slf4j
public class MultiLevelCacheAspect {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    // Jackson ObjectMapper for JSON serialization/deserialization
    @Resource
    private ObjectMapper objectMapper;

    // 本地缓存,Caffeine实现
    private final Cache<String,String> LOCAL_CACHE = Caffeine.newBuilder()
            .maximumSize(10_000L) // 设置最大缓存条目数
            // 缓存 5 分钟后移除
            .expireAfterWrite(Duration.ofMinutes(5))
            .build();

    @Around("@annotation(multiLevelCache)")
    public Object doCache(ProceedingJoinPoint joinPoint, MultiLevelCache multiLevelCache) throws Throwable {
        // 1. 获取方法签名和返回类型，为后续反序列化作准备
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Type returnType = signature.getReturnType();

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
        String cachedValue = LOCAL_CACHE.getIfPresent(cacheKey);
        if (cachedValue != null) {
            log.info("缓存命中:[Local] key: {}", cacheKey);
            log.info("cachedValue:{}", cachedValue);
            return objectMapper.readValue(cachedValue, new TypeReference<Object>() {
                @Override
                public Type getType() {
                    return returnType;
                }
            });
        }

        // 4. 查Redis缓存
        cachedValue = stringRedisTemplate.opsForValue().get(cacheKey);
        if (cachedValue != null) {
            log.info("缓存命中:[Redis] key: {}", cacheKey);
            log.info("cachedValue:{}", cachedValue);
            // 将Redis缓存的值放入本地缓存
            LOCAL_CACHE.put(cacheKey, cachedValue);
            return objectMapper.readValue(cachedValue, new TypeReference<Object>() {
                @Override
                public Type getType() {
                    return returnType;
                }
            });
        }

        // 5. 缓存未命中,执行原方法（查询数据库）
        log.info("缓存未命中,执行原方法: {}", method.getName());
        Object dbResult = joinPoint.proceed();

        // 6. 将结果写入Redis和本地缓存
        if(ObjectUtil.isNotEmpty(dbResult)){
            String cacheValue = objectMapper.writeValueAsString(dbResult);
            // 写入Redis缓存,设置过期时间为5-10分钟,防止缓存雪崩
            int baseExpireTime = multiLevelCache.expireTime();
            int randomRange = multiLevelCache.randomRange();
            stringRedisTemplate.opsForValue().set(cacheKey, cacheValue, baseExpireTime+ RandomUtil.randomInt(0,randomRange));
            // 写入本地缓存
            LOCAL_CACHE.put(cacheKey, cacheValue);
            log.info("缓存已更新, key: {}, value: {}", cacheKey, cacheValue);
        }
        return dbResult;
    }
}
