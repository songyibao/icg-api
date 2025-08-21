package com.yb.icgapi.manager.websocket.disruptor;

import com.yb.icgapi.manager.websocket.model.PictureEditRequestMessage;
import com.yb.icgapi.icpic.domain.user.entity.User;
import lombok.Data;
import org.springframework.web.socket.WebSocketSession;

@Data
public class PictureEditEvent {
    /**
     * 消息payload
     */
    private PictureEditRequestMessage pictureEditRequestMessage;

    /**
     * 图片 id
     */
    private Long pictureId;

    /**
     * 用户信息
     */
    private User user;

    /**
     * 当前用户的session
     */
    private WebSocketSession session;
}
