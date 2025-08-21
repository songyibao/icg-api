package com.yb.icgapi.manager.websocket.disruptor;

import cn.hutool.json.JSONUtil;
import com.yb.icgapi.manager.websocket.model.PictureEditMessageTypeEnum;
import com.yb.icgapi.manager.websocket.model.PictureEditRequestMessage;
import com.yb.icgapi.manager.websocket.model.PictureEditResponseMessage;
import com.yb.icgapi.icpic.domain.user.entity.User;
import com.yb.icgapi.icpic.interfaces.vo.user.UserVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import com.lmax.disruptor.WorkHandler;
import com.yb.icgapi.manager.websocket.PictureEditHandler;

import javax.annotation.Resource;

@Slf4j
@Component
public class PictureEditEventWorkHandler implements WorkHandler<PictureEditEvent> {

    @Resource
    @Lazy
    PictureEditHandler pictureEditHandler;


    @Override
    public void onEvent(PictureEditEvent pictureEditEvent) throws Exception {
        PictureEditRequestMessage pictureEditRequestMessage = pictureEditEvent.getPictureEditRequestMessage();
        Long pictureId = pictureEditEvent.getPictureId();
        User user = pictureEditEvent.getUser();
        WebSocketSession session = pictureEditEvent.getSession();
        // 获取消息类别
        String type = pictureEditRequestMessage.getType();
        PictureEditMessageTypeEnum pictureEditMessageTypeEnum =
                PictureEditMessageTypeEnum.getEnumByValue(type);

        switch(pictureEditMessageTypeEnum){
            case INFO:
                // 该类型消息由服务端发送
                break;
            case ERROR:
                // 该类型消息由服务端发送
                break;
            case ENTER_EDIT:
                pictureEditHandler.handleEnterEditMessage(pictureEditRequestMessage, session,
                        user,pictureId);
                break;
            case EXIT_EDIT:
                pictureEditHandler.handleExitEditMessage(pictureEditRequestMessage, session, user,pictureId);
                break;
            case EDIT_ACTION:
                pictureEditHandler.handleEditPictureMessage(pictureEditRequestMessage, session, user,pictureId);
                break;
            default:
                log.error("未知的消息类型: {}", type);
                PictureEditResponseMessage pictureEditResponseMessage = new PictureEditResponseMessage();
                pictureEditResponseMessage.setType(PictureEditMessageTypeEnum.ERROR.getValue());
                pictureEditResponseMessage.setMessage("未知的消息类型: " + type);
                pictureEditResponseMessage.setUser(User.toVO(user));
                // 向该用户发送错误消息
                session.sendMessage(new TextMessage(JSONUtil.toJsonStr(pictureEditResponseMessage)));
        }
    }
}
