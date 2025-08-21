package com.yb.icgapi.icpic.infrastructure.api.imagesearch.sub;

import cn.hutool.http.HttpException;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONObject;
import com.yb.icgapi.icpic.infrastructure.api.imagesearch.model.ImageSearchResult;
import com.yb.icgapi.icpic.infrastructure.exception.BusinessException;
import com.yb.icgapi.icpic.infrastructure.exception.ErrorCode;

import java.util.ArrayList;
import java.util.List;
// {
//   "status": 0,
//   "data": {
//     "list": [
//       {
//         "fromURL": "http://m.bilibili.com/search?keyword=王道长听了可能会摔手机",
//         "subTitle": "哔哩哔哩",
//         "thumbUrl": "https://mms1.baidu.com/it/u=2858857010,2405378539&fm=253&app=138&f=JPEGw=638&h=359"
//       }
//     ]
//   }
// }

//        public class ImageSearchResult {
//
//            /**
//             * 缩略图地址
//             */
//            private String thumbUrl;
//
//            /**
//             * 来源地址
//             */
//            private String fromUrl;
//
//            /**
//             * 图片来源网站的名称
//             */
//            private String fromSiteName;
//        }
public class GetImageUrlListApi {
    public static List<ImageSearchResult> getImageListFromAjaxUrl(String url) {
        try{
            HttpResponse response = HttpUtil.createGet(url).execute();
            // 检查响应状态码
            int statusCode = response.getStatus();
            if (statusCode != 200) {
                throw new BusinessException(ErrorCode.SERVER_ERROR,"图片搜索请求失败，请求状态码: " + statusCode);
            }
            String responseBody = response.body();
            // 检查响应内容是否为空
            if (responseBody == null || responseBody.isEmpty()) {
                throw new BusinessException(ErrorCode.SERVER_ERROR,"图片搜索请求返回内容为空");
            }
            JSONObject jsonObject = new JSONObject(responseBody);
            // 检查状态码是否为0
            if (!jsonObject.getInt("status").equals(0)) {
                throw new BusinessException(ErrorCode.SERVER_ERROR,
                        "图片搜索请求失败，返回状态码: " + jsonObject.getInt("status"));
            }
            // 获取数据列表
            List<ImageSearchResult> imageList = jsonObject.getByPath("data.list", List.class);
            // 检查数据列表是否为空
            if (imageList == null || imageList.isEmpty()) {
                // 返回一个空列表而不是抛出异常
                return new ArrayList<>();
            }
            // 根据实际数据结构转换为ImageSearchResult对象
            List<ImageSearchResult> results = new ArrayList<>();
            for (Object item : imageList) {
                if (item instanceof JSONObject) {
                    JSONObject itemJson = (JSONObject) item;
                    ImageSearchResult result = new ImageSearchResult();
                    result.setThumbUrl(itemJson.getStr("thumbUrl"));
                    result.setFromUrl(itemJson.getStr("fromURL"));
                    result.setFromSiteName(itemJson.getStr("subTitle"));
                    results.add(result);
                } else {
                    throw new BusinessException(ErrorCode.SERVER_ERROR, "图片搜索结果格式错误，无法转换为ImageSearchResult");
                }
            }
            return results;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SERVER_ERROR);
        }
    }

    public static void main(String[] args) {
        // 测试获取图片列表
        String testUrl = "https://graph.baidu.com/ajax/similardetailnew?card_key=material_sucai&carousel=1&contsign=&curAlbum=0&entrance=GENERAL&f=general&image=&index=0&inspire=material_sucai&jumpIndex=&next=2&page=2&pageFrom=graph_upload_wise&page_size=6&render_type=card_all&session_id=17751077263554410738&sign=02189bfab6c3a510532a901753966607&srcp=&wd=%E7%8E%8B%E4%B9%9F"; // 替换为实际的Ajax URL
        try {
            List<ImageSearchResult> results = getImageListFromAjaxUrl(testUrl);
            for (ImageSearchResult result : results) {
                System.out.println("缩略图: " + result.getThumbUrl());
                System.out.println("来源地址: " + result.getFromUrl());
                System.out.println("来源网站: " + result.getFromSiteName());
            }
        } catch (BusinessException e) {
            System.err.println("错误: " + e.getMessage());
        } catch (HttpException e) {
            System.err.println("HTTP错误: " + e.getMessage());
        }
    }
}
