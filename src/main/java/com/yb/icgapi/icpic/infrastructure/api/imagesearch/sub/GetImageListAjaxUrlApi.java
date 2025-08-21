package com.yb.icgapi.icpic.infrastructure.api.imagesearch.sub;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class GetImageListAjaxUrlApi {

    /**
     * 获取图片列表的Ajax URL。
     * 该方法通过解析指定URL的HTML内容，查找特定的JavaScript代码块，
     * 并从中提取出用于获取图片列表的Ajax请求地址。
     *
     * @param url 包含目标Ajax URL的网页地址
     * @return 提取到的图片列表Ajax URL；如果未找到或发生错误，则返回null。
     */
    public static String getImageListAjaxUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            System.err.println("输入的URL为空。");
            return null;
        }

        try {
            // 1. 使用Jsoup获取url内容
            // 设置一个合理的用户代理和超时时间，模拟浏览器行为
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                    .timeout(10000) // 10秒超时
                    .get();

            // 2. 获取所有<script>标签
            Elements scriptTags = doc.getElementsByTag("script");

            // 3. 定义用于匹配目标内容的正则表达式
            // 这个正则表达式会查找 "name": "全部", 后面紧跟着（允许有空格或换行） "ajaxUrl": "..." 的模式
            // 并捕获 "ajaxUrl" 字段的值
            Pattern pattern = Pattern.compile("\"name\":\\s*\"全部\",\\s*\"ajaxUrl\":\\s*\"([^\"]+)\"");

            // 4. 遍历所有<script>标签
            for (Element scriptTag : scriptTags) {
                // 获取<script>标签内的代码
                String scriptContent = scriptTag.data();

                // 检查脚本内容是否为空
                if (scriptContent != null && !scriptContent.isEmpty()) {
                    // 5. 使用正则表达式在脚本内容中查找匹配项
                    Matcher matcher = pattern.matcher(scriptContent);

                    // 如果找到匹配项
                    if (matcher.find()) {
                        // 6. 提取并返回捕获组1（即目标url）
                        return matcher.group(1);
                    }
                }
            }

            log.error("在页面 {} 中未找到匹配的目标URL。", url);

        } catch (IOException e) {
            log.error("连接或解析URL时发生错误: {}", url);
        } catch (Exception e) {
            log.error("处理过程中发生异常: {}", e.getMessage());
        }

        // 7. 如果遍历完所有<script>标签都没有找到，则返回null
        return null;
    }

    /**
     * 主方法，用于测试.
     * @param args 命令行参数（未使用）
     */
    public static void main(String[] args) {
        // 请替换为你要测试的实际URL
        String testUrl = "https://graph.baidu.com/s?card_key=&client_app_id=&entrance=&f=general&jsup=&pageFrom=graph_upload_wise&session_id=17751077263554410738&sign=02189bfab6c3a510532a901753966607&tn=";
        String foundUrl = getImageListAjaxUrl(testUrl);
        System.out.println("--- 测试开始 ---");
        if (foundUrl != null) {
            System.out.println("成功提取到目标URL: " + foundUrl);
        } else {
            System.out.println("未能提取到目标URL。");
        }
        System.out.println("--- 测试结束 ---");
    }
}