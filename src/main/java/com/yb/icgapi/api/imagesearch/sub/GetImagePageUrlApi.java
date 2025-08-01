package com.yb.icgapi.api.imagesearch.sub;

import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONUtil;
import com.yb.icgapi.exception.BusinessException;
import com.yb.icgapi.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;

import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;

/**
 * 获取以图搜图页面地址 step 1
 */
@Slf4j
public class GetImagePageUrlApi {
    /**
     * 获取以图搜图页面地址
     *
     * @param imageUrl 图片Url
     * @return 页面地址
     */
    public static String getImagePageUrl(String imageUrl){
        // 1. 准备请求参数
        Map<String,Object> formData = new HashMap<>();
        formData.put("image", imageUrl);
        long upTime = System.currentTimeMillis();
        String url = "https://graph.baidu.com/upload?uptime=" + upTime;
        // 2. 发送POST请求
        try{
            HttpResponse response = cn.hutool.http.HttpUtil.createPost(url)
                    .form(formData)
                    .timeout(5000)
                    .execute();
            if(!response.isOk()){
                log.error("获取以图搜图页面地址失败，响应状态码: {}", response.getStatus());
                throw new BusinessException(ErrorCode.SERVER_ERROR,"接口调用失败");
            }
            String responseBody = response.body();
            Map<String,Object> result = JSONUtil.toBean(responseBody, Map.class);

            // 3. 处理响应结果
            if(result == null || !Integer.valueOf(0).equals(result.get("status"))){
                throw new BusinessException(ErrorCode.SERVER_ERROR,"接口调用失败");
            }

            Map<String,Object> data = (Map<String,Object>) result.get("data");
            String rawUrl = (String) data.get("url");
            // 对url进行解码
            String decodedUrl = URLDecoder.decode(rawUrl, "UTF-8");

            if(decodedUrl == null || decodedUrl.isEmpty()){
                throw new BusinessException(ErrorCode.SERVER_ERROR,"搜图接口未返回有效结果");
            }
            return decodedUrl;
        } catch (Exception e){
            log.error("搜索失败");
            throw new BusinessException(ErrorCode.SERVER_ERROR,"搜索失败: " + e.getMessage());
        }
    }

    public static void main(String[] args){
        // 测试以图搜图功能
        String imageUrl = "https://ypic-1304998734.cos.ap-beijing.myqcloud.com/public/1930604499762831362/2025-06-15_5o4oExnKo1Bdytda.jpeg"; // 替换为实际的图片URL
        String resultUrl = getImagePageUrl(imageUrl);
        System.out.println("以图搜图页面地址: " + resultUrl);
    }
}
