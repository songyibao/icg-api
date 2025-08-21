package com.yb.icgapi.icpic.infrastructure.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
public @interface MultiLevelCache {

    /**
     * 缓存的Key前缀
     * @return key前缀
     */
    String KeyPrefix();

    /**
     * Redis缓存的过期时间，单位为秒
     * 默认5分钟
     * @return 过期时间
     */
    int expireTime();

    /**
     * Redis缓存过期时间的随机范围，单位秒
     * 最终过期时间 = expireTime + Random(0, randomRange)
     * 用于防止缓存雪崩
     * @return time unit
     */
    int randomRange();
}
