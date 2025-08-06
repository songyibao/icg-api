package com.yb.icgapi.manager.auth;

import cn.dev33.satoken.stp.StpInterface;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.servlet.ServletUtil;
import cn.hutool.http.ContentType;
import cn.hutool.json.JSON;
import cn.hutool.json.JSONUtil;
import com.yb.icgapi.constant.SpaceUserPermissionConstant;
import com.yb.icgapi.exception.BusinessException;
import com.yb.icgapi.exception.ErrorCode;
import com.yb.icgapi.manager.auth.model.SpaceUserAuthConfig;
import com.yb.icgapi.model.entity.Picture;
import com.yb.icgapi.model.entity.Space;
import com.yb.icgapi.model.entity.SpaceUser;
import com.yb.icgapi.model.entity.User;
import com.yb.icgapi.model.enums.SpaceRoleEnum;
import com.yb.icgapi.model.enums.SpaceTypeEnum;
import com.yb.icgapi.service.PictureService;
import com.yb.icgapi.service.SpaceService;
import com.yb.icgapi.service.SpaceUserService;
import com.yb.icgapi.service.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import java.util.*;

import static com.yb.icgapi.constant.UserConstant.USER_LOGIN_STATE;

/**
 * 自定义权限加载接口实现类
 */
@Component    // 保证此类被 SpringBoot 扫描，完成 Sa-Token 的自定义权限验证扩展
public class StpInterfaceImpl implements StpInterface {

    // 获取应用的上下文路径，通常用于构建完整的 URL
    @Value("${server.servlet.context-path}")
    private String contextPath;

    @Resource
    SpaceService spaceService;

    @Resource
    UserService userService;

    @Resource
    SpaceUserAuthManager spaceUserAuthManager;

    @Resource
    SpaceUserService spaceUserService;

    @Resource
    PictureService pictureService;
    /**
     * 返回一个账号所拥有的权限码集合 
     */
    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        // 判断 loginType，仅对类型为 "space" 进行权限校验
        if (!StpKit.SPACE_TYPE.equals(loginType)) {
            return new ArrayList<>();
        }
        // 管理员权限，表示权限校验通过
        List<String> ALL_PERMISSIONS =
                spaceUserAuthManager.getPermissionsByRole(SpaceRoleEnum.OWNER.getValue());
        // 获取上下文对象
        SpaceUserAuthContext authContext = getAuthContextByRequest();
        // 如果所有字段都为空，表示查询公共图库，可以通过
        if (isAllFieldsNull(authContext)) {
            return ALL_PERMISSIONS;
        }
        // 获取当前登录用户的 userId
        User loginUser = (User) StpKit.SPACE.getSessionByLoginId(loginId).get(USER_LOGIN_STATE);
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "用户未登录");
        }
        Long loginUserId = loginUser.getId();
        // todo:理解为什么会有这个对象？
        // 优先从上下文中获取 SpaceUser 对象
        SpaceUser spaceUser = authContext.getSpaceUser();
        if (spaceUser != null) {
            return spaceUserAuthManager.getPermissionsByRole(spaceUser.getSpaceRole());
        }
        // 如果有 spaceUserId，必然是团队空间，通过数据库查询 SpaceUser 对象
        Long spaceUserId = authContext.getSpaceUserId();
        if (spaceUserId != null) {
            spaceUser = spaceUserService.getById(spaceUserId);
            if (spaceUser == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND, "未找到空间用户信息");
            }
            // 取出当前登录用户对应的 spaceUser
            SpaceUser loginSpaceUser = spaceUserService.lambdaQuery()
                    .eq(SpaceUser::getSpaceId, spaceUser.getSpaceId())
                    .eq(SpaceUser::getUserId, loginUserId)
                    .one();
            // 当前用户不属于该空间，返回空权限列表
            if (loginSpaceUser == null) {
                return new ArrayList<>();
            }
            // 这里会导致管理员在私有空间没有权限，可以再查一次库处理
            return spaceUserAuthManager.getPermissionsByRole(loginSpaceUser.getSpaceRole());
        }
        // 如果没有 spaceUserId，尝试通过 spaceId 或 pictureId 获取 Space 对象并处理
        Long spaceId = authContext.getSpaceId();
        if (spaceId == null) {
            // 如果没有 spaceId，通过 pictureId 获取 Picture 对象和 Space 对象
            Long pictureId = authContext.getPictureId();
            // 图片 id 也没有，表示没有侵入性操作，则默认通过权限校验
            if (pictureId == null) {
                return ALL_PERMISSIONS;
            }
            // 有图片id
            Picture picture = pictureService.lambdaQuery()
                    .eq(Picture::getId, pictureId)
                    .select(Picture::getId, Picture::getSpaceId, Picture::getUserId)
                    .one();
            if (picture == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND, "未找到图片信息");
            }
            spaceId = picture.getSpaceId();
            // 该图片属于公共图库
            if (spaceId == null) {
                if (picture.getUserId().equals(loginUserId) || userService.isAdmin(loginUser)) {
                    // 仅本人或管理员可操作
                    return ALL_PERMISSIONS;
                } else {
                    // 不是自己的图片，仅可查看
                    return Collections.singletonList(SpaceUserPermissionConstant.PICTURE_VIEW);
                }
            }
        }
        // sapceId不为空，只能是私有空间或者团队空间
        // 获取 Space 对象
        Space space = spaceService.getById(spaceId);
        if (space == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "未找到空间信息");
        }
        // 根据 Space 类型判断权限
        return spaceUserAuthManager.getPermissionList(space,loginUser);
//        if (space.getSpaceType() == SpaceTypeEnum.PRIVATE.getValue()) {
//            // 私有空间，仅本人或管理员有权限
//            if (space.getUserId().equals(loginUserId) || userService.isAdmin(loginUser)) {
//                return ALL_PERMISSIONS;
//            } else {
//                return new ArrayList<>();
//            }
//        } else {
//            // 团队空间，查询 SpaceUser 并获取角色和权限
//            spaceUser = spaceUserService.lambdaQuery()
//                    .eq(SpaceUser::getSpaceId, spaceId)
//                    .eq(SpaceUser::getUserId, loginUserId)
//                    .one();
//            if (spaceUser == null) {
//                return new ArrayList<>();
//            }
//            return spaceUserAuthManager.getPermissionsByRole(spaceUser.getSpaceRole());
//        }
    }
    private boolean isAllFieldsNull(Object object) {
        if (object == null) {
            return true; // 对象本身为空
        }
        // 获取所有字段并判断是否所有字段都为空
        return Arrays.stream(ReflectUtil.getFields(object.getClass()))
                // 获取字段值
                .map(field -> ReflectUtil.getFieldValue(object, field))
                // 检查是否所有字段都为空
                .allMatch(ObjectUtil::isEmpty);
    }


    /**
     * 返回一个账号所拥有的角色标识集合 (权限与角色可分开校验)
     */
    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        return new ArrayList<>();
    }

    /**
     * 从请求对象中获取上下文对象
     */
    public SpaceUserAuthContext getAuthContextByRequest() {
        // 这里可以根据实际需要从请求中获取 SpaceUserAuthContext
        // 例如从 ThreadLocal 或者 HttpServletRequest 中获取
        HttpServletRequest request =
                ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
        String contentType = request.getContentType();
        SpaceUserAuthContext spaceUserAuthContext = new SpaceUserAuthContext();
        // 获取请求参数
        if(ContentType.JSON.getValue().equals(contentType)) {
            String body = ServletUtil.getBody(request);
            spaceUserAuthContext = JSONUtil.toBean(body, SpaceUserAuthContext.class);
        }else{
            Map<String,String> paramsMap = ServletUtil.getParamMap(request);
            String debugStr = paramsMap.toString();
            spaceUserAuthContext = JSONUtil.toBean(JSONUtil.toJsonStr(paramsMap),
                    SpaceUserAuthContext.class);
        }
        Long id = spaceUserAuthContext.getId();
        // 根据请求路径判断id字段的含义
        if(ObjUtil.isNotNull(id)){
            String requestURI = request.getRequestURI();
            String path = requestURI.replace(contextPath+"/", "");
            // 获取第一个斜杠前的字符串
            String controllerName = StrUtil.subBefore(path, "/", false);
            switch (controllerName) {
                case "picture":
                    spaceUserAuthContext.setPictureId(id);
                    break;
                case "space":
                    spaceUserAuthContext.setSpaceId(id);
                    break;
                case "spaceUser":
                    spaceUserAuthContext.setSpaceUserId(id);
                    break;
                default:
                    // 其他情况可以根据需要处理
                    break;
            }
        }
        return new SpaceUserAuthContext();
    }
}
