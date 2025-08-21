package com.yb.icgapi.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yb.icgapi.icpic.application.service.UserApplicationService;
import com.yb.icgapi.icpic.infrastructure.constant.DatabaseConstant;
import com.yb.icgapi.icpic.infrastructure.exception.BusinessException;
import com.yb.icgapi.icpic.infrastructure.exception.ErrorCode;
import com.yb.icgapi.icpic.infrastructure.exception.ThrowUtils;
import com.yb.icgapi.manager.auth.SpaceUserAuthManager;
import com.yb.icgapi.manager.sharding.DynamicShardingManager;
import com.yb.icgapi.model.dto.space.SpaceAddRequest;
import com.yb.icgapi.model.dto.space.SpaceQueryRequest;
import com.yb.icgapi.model.entity.Space;
import com.yb.icgapi.model.entity.SpaceUser;
import com.yb.icgapi.icpic.domain.user.entity.User;
import com.yb.icgapi.model.enums.SpaceLevelEnum;
import com.yb.icgapi.model.enums.SpaceRoleEnum;
import com.yb.icgapi.model.enums.SpaceTypeEnum;
import com.yb.icgapi.model.vo.SpaceVO;
import com.yb.icgapi.icpic.interfaces.vo.user.UserVO;
import com.yb.icgapi.service.SpaceService;
import com.yb.icgapi.icpic.infrastructure.mapper.SpaceMapper;
import com.yb.icgapi.service.SpaceUserService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * @author songyibao
 * @description 针对表【space(空间)】的数据库操作Service实现
 * @createDate 2025-07-17 15:01:35
 */
@Service
public class SpaceServiceImpl extends ServiceImpl<SpaceMapper, Space>
        implements SpaceService {

    @Resource
    private UserApplicationService userApplicationService;

    @Resource
    private SpaceUserService spaceUserService;

    @Resource
    private TransactionTemplate transactionTemplate;

    @Resource
    @Lazy
    private DynamicShardingManager dynamicShardingManager;

    // 并发map，用于锁
    private Map<Long, Object> lockMap = new ConcurrentHashMap<>();


    @Override
    public void validSpace(Space space, boolean add) {
        ThrowUtils.ThrowIf(space == null, ErrorCode.PARAM_BLANK, "空间不能为空");
        String spaceName = space.getSpaceName();
        Integer spaceLevel = space.getSpaceLevel();
        Integer spaceType = space.getSpaceType();
        ThrowUtils.ThrowIf(StringUtils.isBlank(spaceName), ErrorCode.PARAM_BLANK, "空间名称不能为空");
        ThrowUtils.ThrowIf(StrUtil.length(spaceName) > 30, ErrorCode.PARAMS_ERROR, "空间名称过长");
        if (add) {
            ThrowUtils.ThrowIf(spaceLevel == null, ErrorCode.PARAM_BLANK, "创建时空间级别不能为空");
            ThrowUtils.ThrowIf(spaceType == null, ErrorCode.PARAM_BLANK, "创建时空间类型不能为空");
        }
        ThrowUtils.ThrowIf(spaceLevel != null && SpaceLevelEnum.fromValue(spaceLevel) == null, ErrorCode.PARAMS_ERROR, "空间级别不合法");
        ThrowUtils.ThrowIf(spaceType != null && SpaceTypeEnum.fromValue(spaceType) == null,
                ErrorCode.PARAMS_ERROR,
                "空间类型不合法");
    }

    @Override
    public void fillSpaceBySpaceLevel(Space space) {
        SpaceLevelEnum spaceLevelEnum = SpaceLevelEnum.fromValue(space.getSpaceLevel());
        ThrowUtils.ThrowIf(spaceLevelEnum == null, ErrorCode.PARAMS_ERROR, "空间级别不合法");
        if (space.getMaxCount() == null) {
            space.setMaxCount(spaceLevelEnum.getMaxCount());
        }
        if (space.getMaxSize() == null) {
            space.setMaxSize(spaceLevelEnum.getMaxSize());
        }
    }

    @Override
    public SpaceVO getSpaceVO(Space space) {
        if (space == null) {
            return null;
        }
        SpaceVO spaceVO = SpaceVO.objToVo(space);
        // 填充user字段
        Long userId = space.getUserId();
        if (userId != null) {
            spaceVO.setUser(userApplicationService.getUserVOById(userId));
        }
        return spaceVO;
    }

    @Override
    public QueryWrapper<Space> getQueryWrapper(SpaceQueryRequest spaceQueryRequest) {


        if (spaceQueryRequest == null) {
            return null;
        }
        Long id = spaceQueryRequest.getId();
        String spaceName = spaceQueryRequest.getSpaceName();
        Integer spaceLevel = spaceQueryRequest.getSpaceLevel();
        Integer spaceType = spaceQueryRequest.getSpaceType();
        Long userId = spaceQueryRequest.getUserId();
        int currentPage = spaceQueryRequest.getCurrentPage();
        int pageSize = spaceQueryRequest.getPageSize();
        String sortField = spaceQueryRequest.getSortField();
        String sortOrder = spaceQueryRequest.getSortOrder();

        QueryWrapper<Space> queryWrapper = new QueryWrapper<>();
        // 添加查询条件
        queryWrapper.eq(id != null, "id", id)
                .eq(ObjUtil.isNotEmpty(userId), "userId", userId)
                .eq(ObjUtil.isNotEmpty(spaceLevel), "spaceLevel", spaceLevel)
                .eq(ObjUtil.isNotEmpty(spaceType), "spaceType", spaceType)
                .like(StringUtils.isNotBlank(spaceName), "spaceName", spaceName)
                .orderBy(StringUtils.isNotBlank(sortField) && StringUtils.isNotBlank(sortOrder),
                        sortOrder.equals(DatabaseConstant.ASC), sortField);

        return queryWrapper;
    }

    @Override
    public List<SpaceVO> getSpaceVOList(List<Space> spaceList) {
        if (CollUtil.isEmpty(spaceList)) {
            return Collections.emptyList();
        }
        // 对象列表 => 封装列表
        List<SpaceVO> spaceVOList = spaceList.stream()
                .map(SpaceVO::objToVo)
                .collect(Collectors.toList());
        // 查询并填充用户信息
        // 这里作一下优化，通过一次查询查出所需要的所有用户信息
        Set<Long> userIdSet = spaceList.stream()
                .map(Space::getUserId)
                .filter(ObjUtil::isNotEmpty)
                .collect(Collectors.toSet());
        Map<Long, List<User>> userIdUserListMap = userApplicationService.listByIds(userIdSet).stream().collect(Collectors.groupingBy(User::getId));
        // 填充用户信息
        spaceVOList.forEach(spaceVO -> {
            Long userId = spaceVO.getUserId();
            User user = null;
            if (userIdUserListMap.containsKey(userId)) {
                user = userIdUserListMap.get(userId).get(0);
            }
            spaceVO.setUser(User.toVO(user));
        });
        return spaceVOList;
    }

    public Page<SpaceVO> getSpaceVOPage(Page<Space> spacePage) {
        List<Space> spaceList = spacePage.getRecords();
        Page<SpaceVO> spaceVOPage = new Page<>(spacePage.getCurrent(), spacePage.getSize(), spacePage.getTotal());
        if (CollUtil.isEmpty(spaceList)) {
            return spaceVOPage;
        }
        // 对象列表 => 封装列表
        List<SpaceVO> spaceVOList = getSpaceVOList(spaceList);
        // 设置封装列表到分页对象
        spaceVOPage.setRecords(spaceVOList);
        return spaceVOPage;
    }

    @Override
    public Page<SpaceVO> listSpaceVOByPage(SpaceQueryRequest spaceQueryRequest) {
        long currentPage = spaceQueryRequest.getCurrentPage();
        long pageSize = spaceQueryRequest.getPageSize();

        Page<Space> page = this.page(
                new Page<>(currentPage, pageSize),
                this.getQueryWrapper(spaceQueryRequest)
        );
        return this.getSpaceVOPage(page);
    }

    @Override
    public long addSpace(SpaceAddRequest spaceAddRequest, User loginUser) {
        ThrowUtils.ThrowIf(spaceAddRequest == null, ErrorCode.PARAM_BLANK, "空间添加请求不能为空");
        ThrowUtils.ThrowIf(loginUser == null || loginUser.getId() == null, ErrorCode.NOT_LOGIN_ERROR, "用户未登录或登录状态异常");
        Long userId = loginUser.getId();
        Space space = new Space();
        BeanUtils.copyProperties(spaceAddRequest, space);
        if (StrUtil.isBlank(space.getSpaceName())) {
            // 设置空间名称默认值
            space.setSpaceName("默认空间");
        }
        if (ObjUtil.isEmpty(space.getSpaceLevel())) {
            // 设置空间级别默认值
            space.setSpaceLevel(SpaceLevelEnum.COMMON.getValue());
        }
        if(ObjUtil.isEmpty(space.getSpaceType())){
            // 设置空间类型默认值
            space.setSpaceType(SpaceTypeEnum.PRIVATE.getValue());
        }
        this.fillSpaceBySpaceLevel(space);
        // 1. 参数校验
        this.validSpace(space, true);
        // 2. 权限校验
        if (space.getSpaceLevel() != SpaceLevelEnum.COMMON.getValue() && !loginUser.isAdmin()) {
            throw new BusinessException(ErrorCode.NO_AUTHORIZED, "非管理员用户不能创建非普通空间");
        }
        space.setUserId(userId);
        // 设置创建时间
        space.setCreateTime(new Date());

        // 3. 控制同一用户只能创建一个空间
        // 使用ConcurrentMap来存储锁对象，仅适用于单机，无法解决分布式锁问题
        Object lock = lockMap.computeIfAbsent(userId, k -> new Object());
        synchronized (lock) {
            try {
                Long newSpaceId = transactionTemplate.execute(status -> {
                    // 每个用户每类空间只能创建一个
                    Space existingSpace = this.lambdaQuery()
                            .eq(Space::getSpaceType, spaceAddRequest.getSpaceType())
                            .eq(Space::getUserId, userId)
                            .one();
                    ThrowUtils.ThrowIf(existingSpace != null, ErrorCode.OPERATION_ERROR,
                            "用户已经创建该类空间，不能重复创建");
                    // 4. 执行添加操作
                    boolean saveResult = this.save(space);
                    ThrowUtils.ThrowIf(!saveResult, ErrorCode.OPERATION_ERROR, "空间创建失败");
                    // 5. 如果是团队空间,创建对应的空间用户关联
                    if(space.getSpaceType() == SpaceTypeEnum.SHARE.getValue()){
                        SpaceUser spaceUser = new SpaceUser();
                        spaceUser.setSpaceId(space.getId());
                        spaceUser.setUserId(userId);
                        spaceUser.setSpaceRole(SpaceRoleEnum.OWNER.getValue());
                        saveResult = spaceUserService.save(spaceUser);
                        ThrowUtils.ThrowIf(!saveResult, ErrorCode.OPERATION_ERROR, "空间用户关联创建失败");
                    }
                    // 6. 创建对应的图片分表(仅旗舰版&&团队空间)
                    dynamicShardingManager.createPictureSpaceTable(space);
                    return space.getId();
                });
                // 在返回前，检查是否为 null
                if (newSpaceId == null) {
                    // 如果为 null，说明创建失败或未能获取ID，这是一个严重的内部错误
                    // 抛出异常是合理的选择
                    throw new BusinessException(ErrorCode.SERVER_ERROR, "空间创建失败，未能获取新空间ID");
                }
                return newSpaceId;
            } finally {
                lockMap.remove(userId);
            }
        }

    }

    /**
     * 检查用户是否有权限访问指定空间
     *
     * @param loginUser
     * @param spaceId
     */
    @Override
    public void checkSpaceAuth(User loginUser, Long spaceId) {
        if (spaceId == null) {
            // 公共空间
            return;
        }
        Space space = this.getById(spaceId);
        ThrowUtils.ThrowIf(space == null, ErrorCode.NOT_FOUND, "空间不存在");
        ThrowUtils.ThrowIf(!space.getUserId().equals(loginUser.getId()), ErrorCode.NO_AUTHORIZED, "没有权限访问该空间");
    }

    @Override
    public void checkSpaceAuth(User user, Space space) {
        if (!space.getUserId().equals(user.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTHORIZED);
        }
    }
}




