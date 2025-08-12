package com.yb.icgapi.manager.websocket;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.yb.icgapi.constant.SpaceUserPermissionConstant;
import com.yb.icgapi.manager.auth.SpaceUserAuthManager;
import com.yb.icgapi.model.entity.Picture;
import com.yb.icgapi.model.entity.Space;
import com.yb.icgapi.model.entity.User;
import com.yb.icgapi.model.enums.SpaceTypeEnum;
import com.yb.icgapi.service.PictureService;
import com.yb.icgapi.service.SpaceService;
import com.yb.icgapi.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

/**
 * WebSocket拦截器，建立连接前要校验
 */
@Component
@Slf4j
public class WsHandshakeInterceptor implements HandshakeInterceptor {
    @Resource
    UserService userService;

    @Resource
    PictureService pictureService;

    @Resource
    SpaceService spaceService;

    @Resource
    SpaceUserAuthManager spaceUserAuthManager;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
        if(request instanceof ServletServerHttpRequest){
            HttpServletRequest servletRequest = ((ServletServerHttpRequest) request).getServletRequest();
            String pictureId = servletRequest.getParameter("pictureId");
            if(StrUtil.isBlank(pictureId)){
                log.error("缺少图片参数，拒绝握手");
                return false;
            }
            // 获取当前用户
            User loginUser = userService.getLoginUser(servletRequest);
            if(ObjUtil.isEmpty(loginUser)){
                log.error("用户未登录，拒绝握手");
                return false;
            }

            Picture picture = pictureService.getById(pictureId);
            if(ObjUtil.isEmpty(picture)){
                log.error("图片不存在，拒绝握手");
                return false;
            }
            Long spaceId = picture.getSpaceId();

            if(spaceId == null){
                log.error("图片为公共空间图片，拒绝握手");
                return false;
            }
            Space space = spaceService.getById(spaceId);
            if(ObjUtil.isEmpty(space)){
                log.error("对应的空间不存在，内部异常");
                return false;
            }
            if(!space.getSpaceType().equals(SpaceTypeEnum.SHARE.getValue())){
                log.error("非团队空间，不允许协同编辑");
                return false;
            }
            List<String> permissionList = spaceUserAuthManager.getPermissionList(space,loginUser);
            if(!permissionList.contains(SpaceUserPermissionConstant.PICTURE_EDIT)){
                log.error("用户无权限编辑图片");
                return false;
            }
            // 设置信息到会话中
            attributes.put("user",loginUser);
            attributes.put("userId",loginUser.getId());
            // pictureId是从请求参数重获取的需要转换为Long类型
            attributes.put("pictureId",Long.valueOf(pictureId));
            return true;
        }
        return false;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Exception exception) {

    }
}
