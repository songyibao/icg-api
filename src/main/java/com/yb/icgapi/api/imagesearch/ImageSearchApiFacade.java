package com.yb.icgapi.api.imagesearch;

import com.yb.icgapi.api.imagesearch.model.ImageSearchResult;
import com.yb.icgapi.api.imagesearch.sub.GetImageListAjaxUrlApi;
import com.yb.icgapi.api.imagesearch.sub.GetImagePageUrlApi;
import com.yb.icgapi.api.imagesearch.sub.GetImageUrlListApi;

import java.util.List;

public class ImageSearchApiFacade {
    /**
     * 搜索图片
     * @param imageUrl 图片地址
     */
    public static List<ImageSearchResult> searchImage(String imageUrl) {
        String imagePageUrl = GetImagePageUrlApi.getImagePageUrl(imageUrl);
        String imageListAjaxUrl = GetImageListAjaxUrlApi.getImageListAjaxUrl(imagePageUrl);
        List<ImageSearchResult> imageSearchResultList =
                GetImageUrlListApi.getImageListFromAjaxUrl(imageListAjaxUrl);
        return imageSearchResultList;
    }

    public static void main(String[] args) {
        ImageSearchApiFacade imageSearchApiFacade = new ImageSearchApiFacade();
        List<ImageSearchResult> results = imageSearchApiFacade.searchImage("https://ypic-1304998734.cos.ap-beijing.myqcloud.com/public/1930604499762831362/2025-06-15_48RZClmxsBYHxdzv.jpeg");
        for (ImageSearchResult result : results) {
            System.out.println("Thumb URL: " + result.getThumbUrl());
            System.out.println("From URL: " + result.getFromUrl());
            System.out.println("From Site Name: " + result.getFromSiteName());
        }
    }
}
