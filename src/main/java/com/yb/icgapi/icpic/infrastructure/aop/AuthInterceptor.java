package com.yb.icgapi.icpic.infrastructure.aop;

import com.yb.icgapi.icpic.application.service.UserApplicationService;
import com.yb.icgapi.icpic.infrastructure.annotation.AuthCheck;
import com.yb.icgapi.icpic.infrastructure.exception.ErrorCode;
import com.yb.icgapi.icpic.infrastructure.exception.ThrowUtils;
import com.yb.icgapi.icpic.domain.user.entity.User;
import com.yb.icgapi.icpic.domain.user.valueobject.UserRoleEnum;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

@Aspect
@Component
public class AuthInterceptor {

    @Resource
    private UserApplicationService userApplicationService;

    @Around("@annotation(authCheck)")
    public Object doInterceptor(ProceedingJoinPoint joinPoint, AuthCheck authCheck) throws Throwable {
        String mustRole = authCheck.mustRole();

        RequestAttributes requestAttributes = RequestContextHolder.currentRequestAttributes();
        HttpServletRequest request = ((ServletRequestAttributes)requestAttributes).getRequest();
        // 获取登录用户
        User loginUser = userApplicationService.getLoginUser(request);

        UserRoleEnum needRole = UserRoleEnum.getEnumByValue(mustRole);
        // 指定的权限为空，放行
        if(needRole == null) {
            return joinPoint.proceed();
        }
        // 指定了访问权限
        UserRoleEnum userRoleEnum = UserRoleEnum.getEnumByValue(loginUser.getUserRole());
        // 用户角色为空，抛出异常
        ThrowUtils.ThrowIf(userRoleEnum == null, ErrorCode.NO_AUTHORIZED);
        if(needRole.equals(UserRoleEnum.ADMIN)){
            // 只有管理员可以访问
            ThrowUtils.ThrowIf(!userRoleEnum.equals(UserRoleEnum.ADMIN), ErrorCode.NO_AUTHORIZED);
        }
        return joinPoint.proceed();
    }


}
