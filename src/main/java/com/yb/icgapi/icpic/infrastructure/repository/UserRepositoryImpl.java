package com.yb.icgapi.icpic.infrastructure.repository;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yb.icgapi.icpic.domain.user.entity.User;
import com.yb.icgapi.icpic.domain.user.repository.UserRepository;
import com.yb.icgapi.icpic.infrastructure.mapper.UserMapper;
import org.springframework.stereotype.Service;

/**
 * 用户仓储实现
 */
@Service
public class UserRepositoryImpl extends ServiceImpl<UserMapper, User> implements UserRepository {
}
