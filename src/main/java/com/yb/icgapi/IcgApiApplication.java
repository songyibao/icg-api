package com.yb.icgapi;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@SpringBootApplication
@MapperScan("com.yb.icgapi.mapper")
@EnableAspectJAutoProxy(exposeProxy = true)
public class IcgApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(IcgApiApplication.class, args);
    }

}
