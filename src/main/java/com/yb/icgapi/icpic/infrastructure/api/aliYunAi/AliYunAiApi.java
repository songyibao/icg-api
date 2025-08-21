package com.yb.icgapi.icpic.infrastructure.api.aliYunAi;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.ContentType;
import cn.hutool.http.Header;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONUtil;
import com.yb.icgapi.icpic.infrastructure.api.aliYunAi.model.CreateOutPaintingTaskResponse;
import com.yb.icgapi.icpic.infrastructure.api.aliYunAi.model.CreateOutPaintingTaskRequest;
import com.yb.icgapi.icpic.infrastructure.api.aliYunAi.model.GetOutPaintingTaskResponse;
import com.yb.icgapi.icpic.infrastructure.exception.BusinessException;
import com.yb.icgapi.icpic.infrastructure.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class AliYunAiApi {
    // 读取配置文件
    @Value("${aliYunAi.apiKey}")
    private String apikey;

    // 创建任务地址
    private static final String CREATE_OUT_PAINTINGTASK_URL = "https://dashscope.aliyuncs" +
            ".com/api/v1/services/aigc/image2image/out-painting";

    // 查询任务状态地址（GET参数模板：任务ID）
    private static final String GET_OUT_PAINTING_TASK_URL = "https://dashscope.aliyuncs" +
            ".com/api/v1/tasks/%s";

    /**
     * 创建任务
     */
    public CreateOutPaintingTaskResponse createOutPaintingTask(CreateOutPaintingTaskRequest createOutpaintingTaskRequest){
        if(createOutpaintingTaskRequest == null){
            throw new BusinessException(ErrorCode.PARAM_BLANK);
        }
        // 发送请求
        String body = JSONUtil.toJsonStr(createOutpaintingTaskRequest);
        HttpRequest httpRequest = HttpRequest.post(CREATE_OUT_PAINTINGTASK_URL)
                .header(Header.AUTHORIZATION,"Bearer "+ apikey)
                // 必须开启异步处理
                .header("X-DashScope-Async","enable")
                .header(Header.CONTENT_TYPE, ContentType.JSON.getValue())
                .body(body);
        try(HttpResponse httpResponse = httpRequest.execute()){
            if(!httpResponse.isOk()){
                log.error("请求异常，状态码：{}，响应体：{}", httpResponse.getStatus(), httpResponse.body());
                throw new BusinessException(ErrorCode.OPERATION_ERROR,"AI扩图失败");
            }
            CreateOutPaintingTaskResponse response = JSONUtil.toBean(httpResponse.body(),
                    CreateOutPaintingTaskResponse.class);
            String errorCode = response.getCode();
            if(StrUtil.isNotBlank(errorCode)){
                log.error("AI扩图失败，错误码：{}，错误信息：{}", errorCode, response.getMessage());
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "AI扩图失败：" + response.getMessage());
            }
            return response;
        }
    }
    /**
     * 查询任务状态
     * curl -X GET \
     * --header "Authorization: Bearer $DASHSCOPE_API_KEY" \
     * https://dashscope.aliyuncs.com/api/v1/tasks/86ecf553-d340-4e21-xxxxxxxxx
     */
    public GetOutPaintingTaskResponse getOutPaintingTask(String taskId) {
        if(StrUtil.isBlank(taskId)){
            throw new BusinessException(ErrorCode.PARAM_BLANK, "任务ID不能为空");
        }
        HttpRequest httpRequest = HttpRequest.get(String.format(GET_OUT_PAINTING_TASK_URL, taskId))
                .header(Header.AUTHORIZATION, "Bearer " + apikey);
        try (HttpResponse httpResponse = httpRequest.execute()) {
            if (!httpResponse.isOk()) {
                log.error("请求异常，状态码：{}，响应体：{}", httpResponse.getStatus(), httpResponse.body());
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "查询任务状态失败");
            }
            return JSONUtil.toBean(httpResponse.body(),
                    GetOutPaintingTaskResponse.class);
        }
    }
}
