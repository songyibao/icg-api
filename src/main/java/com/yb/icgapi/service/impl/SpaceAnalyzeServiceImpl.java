package com.yb.icgapi.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yb.icgapi.icpic.application.service.PictureApplicationService;
import com.yb.icgapi.icpic.infrastructure.exception.BusinessException;
import com.yb.icgapi.icpic.infrastructure.exception.ErrorCode;
import com.yb.icgapi.icpic.infrastructure.exception.ThrowUtils;
import com.yb.icgapi.icpic.infrastructure.mapper.SpaceMapper;
import com.yb.icgapi.model.dto.analyze.*;
import com.yb.icgapi.icpic.domain.picture.entity.Picture;
import com.yb.icgapi.model.entity.Space;
import com.yb.icgapi.icpic.domain.user.entity.User;
import com.yb.icgapi.model.vo.analyze.*;
import com.yb.icgapi.icpic.domain.picture.service.PictureDomainService;
import com.yb.icgapi.service.SpaceAnalyzeService;
import com.yb.icgapi.service.SpaceService;
import com.yb.icgapi.icpic.application.service.UserApplicationService;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SpaceAnalyzeServiceImpl extends ServiceImpl<SpaceMapper, Space> implements SpaceAnalyzeService {
    @Resource
    PictureApplicationService pictureApplicationService;
    @Resource
    UserApplicationService userApplicationService;
    @Resource
    SpaceService spaceService;
    @Resource
    PictureDomainService pictureDomainService;

    // 为提高代码可读性和可维护性，将单位定义为常量
    private static final long KB = 1024;
    private static final long MB = 1024 * KB;

    private void checkSpaceAnalyzeAuth(SpaceAnalyzeRequest spaceAnalyzeRequest, User loginUser) {
        // 只有管理员可以分析全部空间或者公共空间
        if (spaceAnalyzeRequest.isQueryAll() || spaceAnalyzeRequest.isQueryPublic()) {
            ThrowUtils.ThrowIf(!loginUser.isAdmin(), ErrorCode.NO_AUTHORIZED);
            return;
        }
        // 私有空间权限校验
        Long spaceId = spaceAnalyzeRequest.getSpaceId();
        ThrowUtils.ThrowIf(spaceId == null || spaceId <= 0, ErrorCode.PARAMS_ERROR, "非法参数");
        // 检查用户是否有权限访问该空间
        spaceService.checkSpaceAuth(loginUser, spaceId);
    }

    private void fillAnalyzeQueryWrapper(SpaceAnalyzeRequest spaceAnalyzeRequest,
                                         QueryWrapper<Picture> queryWrapper) {
        if (spaceAnalyzeRequest.isQueryAll()) {
            return;
        } else if (spaceAnalyzeRequest.isQueryPublic()) {
            queryWrapper.isNull("spaceId");
            return;
        }
        Long spaceId = spaceAnalyzeRequest.getSpaceId();
        if (spaceId != null) {
            queryWrapper.eq("spaceId", spaceId);
            return;
        }
        throw new BusinessException(ErrorCode.PARAMS_ERROR, "未指定查询范围");
    }

    @Override
    public SpaceUsageAnalyzeResponse analyzeSpaceUsage(SpaceUsageAnalyzeRequest spaceUsageAnalyzeRequest, User loginUser) {
        // 校验权限
        checkSpaceAnalyzeAuth(spaceUsageAnalyzeRequest, loginUser);
        if (spaceUsageAnalyzeRequest.isQueryAll() || spaceUsageAnalyzeRequest.isQueryPublic()) {
            // 分析公共空间或者全部空间
            Boolean isQueryPublic = spaceUsageAnalyzeRequest.isQueryPublic();
            List<Long> pictureSizeList =
                    pictureApplicationService.getPictureSizeList(isQueryPublic,null);
            // 计算总大小
            long totalSize = pictureSizeList.stream().mapToLong(Long::longValue).sum();
            // 计算总数量
            long totalCount = pictureSizeList.size();
            SpaceUsageAnalyzeResponse spaceUsageAnalyzeResponse = new SpaceUsageAnalyzeResponse();
            spaceUsageAnalyzeResponse.setUsedSize(totalSize);
            spaceUsageAnalyzeResponse.setMaxSize(null);
            spaceUsageAnalyzeResponse.setSizeUsageRatio(null);
            spaceUsageAnalyzeResponse.setUsedCount(totalCount);
            spaceUsageAnalyzeResponse.setMaxCount(null);
            spaceUsageAnalyzeResponse.setCountUsageRatio(null);
            return spaceUsageAnalyzeResponse;
        } else {
            // 分析私有空间
            Long spaceId = spaceUsageAnalyzeRequest.getSpaceId();
            ThrowUtils.ThrowIf(spaceId == null || spaceId <= 0, ErrorCode.PARAMS_ERROR, "非法参数");
            Space space = spaceService.getById(spaceId);
            ThrowUtils.ThrowIf(space == null, ErrorCode.NOT_FOUND, "空间不存在");
            // 获取该空间下的所有图片大小
            List<Long> pictureSizeList = pictureApplicationService.getPictureSizeList(false,
                    spaceId);
            // 计算总大小
            long totalSize = pictureSizeList.stream().mapToLong(Long::longValue).sum();
            // 计算总数量
            long totalCount = pictureSizeList.size();
            SpaceUsageAnalyzeResponse spaceUsageAnalyzeResponse = new SpaceUsageAnalyzeResponse();
            spaceUsageAnalyzeResponse.setUsedSize(totalSize);
            spaceUsageAnalyzeResponse.setMaxSize(space.getMaxSize());
            spaceUsageAnalyzeResponse.setSizeUsageRatio(totalSize * 1.0 / space.getMaxSize());
            spaceUsageAnalyzeResponse.setUsedCount(totalCount);
            spaceUsageAnalyzeResponse.setMaxCount(space.getMaxCount());
            spaceUsageAnalyzeResponse.setCountUsageRatio(totalCount * 1.0 / space.getMaxCount());
            return spaceUsageAnalyzeResponse;
        }
    }

    @Override
    public List<SpaceCategoryAnalyzeResponse> analyzeSpaceCategory(SpaceCategoryAnalyzeRequest spaceCategoryAnalyzeRequest, User loginUser) {
        // 校验权限
        checkSpaceAnalyzeAuth(spaceCategoryAnalyzeRequest, loginUser);
        // 创建查询条件
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        fillAnalyzeQueryWrapper(spaceCategoryAnalyzeRequest, queryWrapper);
        queryWrapper
                .select("category", "count(*) as count", "sum(picSize) as totalSize")
                .groupBy("category");
        // 分组统计每个分类的图片数量和总大小

        List<Map<String, Object>> mapList = pictureApplicationService.listMaps(queryWrapper);
        // 如果没有查询到数据，返回空列表
        if (CollectionUtil.isEmpty(mapList)) {
            return Collections.emptyList();
        }
        // 转换为响应对象列表
        return mapList.stream()
                .map(map -> {
                    SpaceCategoryAnalyzeResponse response = new SpaceCategoryAnalyzeResponse();

                    Object categoryObj = map.get("category");
                    // 如果 category 为 null，给一个默认值，比如 "未分类"
                    response.setCategory(categoryObj == null ? "未分类" : categoryObj.toString());

                    response.setCount(((Number) map.get("count")).longValue());

                    // ⭐ 最佳实践：同样对 totalSize 做空值检查，因为 sum() 在某些情况下也可能返回 null
                    Object totalSizeObj = map.get("totalSize");
                    response.setTotalSize(totalSizeObj == null ? 0L : ((Number) totalSizeObj).longValue());

                    return response;
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<SpaceTagAnalyzeResponse> analyzeSpaceTag(SpaceTagAnalyzeRequest spaceTagAnalyzeRequest,
                                                         User loginUser) {
        // 校验权限
        checkSpaceAnalyzeAuth(spaceTagAnalyzeRequest, loginUser);
        boolean isQueryPublic = spaceTagAnalyzeRequest.isQueryPublic();
        Long spaceId = spaceTagAnalyzeRequest.getSpaceId();
        Map<String, Long> tagCountMap = pictureApplicationService.getTagCountMap(isQueryPublic,
                spaceId);
        // 转换为响应对象列表,按标签使用次数降序排序
        return tagCountMap.entrySet().stream()
                .sorted((e1, e2) -> Long.compare(e2.getValue(), e1.getValue()))
                .map(entry -> {
                    return new SpaceTagAnalyzeResponse(entry.getKey(), entry.getValue());
                })
                .collect(Collectors.toList());
    }

    /**
     * 分析空间内图片大小的情况
     */
    @Override
    public List<SpacePictureSizeAnalyzeResponse> analyzeSpacePictureSize(SpacePictureSizeAnalyzeRequest spacePictureSizeAnalyzeRequest, User loginUser) {
        // 校验权限
        checkSpaceAnalyzeAuth(spacePictureSizeAnalyzeRequest, loginUser);
        // 创建查询条件
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        fillAnalyzeQueryWrapper(spacePictureSizeAnalyzeRequest, queryWrapper);
        // 获取所有图片的大小的列表
        List<Long> pictureSizeList =
                pictureApplicationService.getPictureSizeList(spacePictureSizeAnalyzeRequest.isQueryPublic(),
                        spacePictureSizeAnalyzeRequest.getSpaceId());
        // 分组统计每个图片大小范围的数量
        return pictureSizeList
                .stream()
                .collect(Collectors.groupingBy(size -> {
                    if (size < 100 * KB) {
                        return "0-100KB";
                    } else if (size < 500 * KB) {
                        return "100KB-500KB";
                    } else if (size < 1 * MB) {
                        return "500KB-1MB";
                    } else if (size < 5 * MB) {
                        return "1MB-5MB";
                    } else if (size < 10 * MB) {
                        return "5MB-10MB";
                    } else {
                        return "10MB+";
                    }
                }, Collectors.counting()))
                .entrySet().stream()
                .map(entry -> new SpacePictureSizeAnalyzeResponse(entry.getKey(), entry.getValue()))
                .sorted((o1, o2) -> {
                    // 排序逻辑本身不需要修改，但它依赖的 parseSize 方法已增强
                    String[] sizeRange1 = o1.getSizeRange().split("-");
                    String[] sizeRange2 = o2.getSizeRange().split("-");
                    // 使用增强后的 parseSize 方法进行比较
                    long size1Start = parseSize(sizeRange1[0]);
                    long size2Start = parseSize(sizeRange2[0]);
                    return Long.compare(size1Start, size2Start);
                }).collect(Collectors.toList());
    }

    /**
     * 排序辅助函数
     * 将 "100KB", "5MB", "10MB+" 等字符串解析为字节数(long)以便排序。
     * 此版本通过正则表达式移除非数字字符，增强了对 "10MB+" 这类字符串的处理能力。
     *
     * @param sizeStr 尺寸范围字符串 (如 "100KB", "10MB+")
     * @return 对应的字节数
     */
    private long parseSize(String sizeStr) {
        // 移除非数字和"."之外的所有字符，以处理 "10MB+" 这样的情况
        String numStr = sizeStr.replaceAll("[^0-9.]", "");
        // 注意：这里假设不会出现小数，如果可能出现则需使用 Double.parseDouble
        long num = Long.parseLong(numStr);

        if (sizeStr.toUpperCase().contains("MB")) {
            return num * MB;
        } else if (sizeStr.toUpperCase().contains("KB")) {
            return num * KB;
        } else {
            // 假设没有单位的 "0" 代表 0 字节
            return num;
        }
    }

    @Override
    public List<SpaceUserAnalyzeResponse> analyzeSpaceUserUpload(SpaceUserAnalyzeRequest spaceUserAnalyzeRequest, User loginUser) {
        ThrowUtils.ThrowIf(spaceUserAnalyzeRequest == null, ErrorCode.PARAM_BLANK, "空间用户分析请求不能为空");
        // 校验权限
        checkSpaceAnalyzeAuth(spaceUserAnalyzeRequest, loginUser);
        // 用户不能分析别的用户
        if (!loginUser.isAdmin() && !loginUser.getId().equals(spaceUserAnalyzeRequest.getUserId())) {
            throw new BusinessException(ErrorCode.NO_AUTHORIZED, "无权分析其他用户的空间使用情况");
        }
        // 创建查询条件
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        fillAnalyzeQueryWrapper(spaceUserAnalyzeRequest, queryWrapper);
        // 分析每 日 周 月 的图片上传数量 key:period value:count
        String timeDimension = spaceUserAnalyzeRequest.getTimeDimension();
        switch (timeDimension) {
            case "day":
                queryWrapper.select("DATE_FORMAT(createTime,'%Y-%m-%d') as period", "COUNT(*) as " +
                        "count");
                break;
            case "week":
                queryWrapper.select("YEARWEEK(createTime) as period", "COUNT(*) as count");
                break;
            case "month":
                queryWrapper.select("DATE_FORMAT(createTime,'%Y-%m') as period", "COUNT(*) as count");
                break;
            default:
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "不支持的时间维度: " + timeDimension);
        }
        queryWrapper.groupBy("period").orderByAsc("period");
        // 执行查询
        List<Map<String, Object>> mapList = pictureDomainService.listMaps(queryWrapper);
        // 如果没有查询到数据，返回空列表
        if (CollectionUtils.isEmpty(mapList)) {
            return Collections.emptyList();
        }
        // 转换为响应对象列表
        List<SpaceUserAnalyzeResponse> responseList = mapList.stream()
                .map(map -> {
                    SpaceUserAnalyzeResponse response = new SpaceUserAnalyzeResponse();
                    response.setPeriod(map.get("period").toString());
                    response.setCount(((Number) map.get("count")).longValue());
                    return response;
                })
                .collect(Collectors.toList());
        return responseList;
    }

    @Override
    public List<Space> analyzeSpaceUsageRank(SpaceRankAnalyzeRequest spaceRankAnalyzeRequest,
                                             User loginUser) {
        // 校验权限
        ThrowUtils.ThrowIf(!loginUser.isAdmin(), ErrorCode.NO_AUTHORIZED, "只有管理员可以查看空间使用排行");
        // 创建查询条件
        QueryWrapper<Space> queryWrapper = new QueryWrapper<>();
        // 公共空间不参与排名
        queryWrapper.isNotNull("spaceId");
        Integer limit = spaceRankAnalyzeRequest.getTopN();
        queryWrapper.orderByDesc("totalSize");
        if (limit != null && limit > 0) {
            queryWrapper.last("LIMIT " + limit);
        } else {
            // 默认查询前10个
            queryWrapper.last("LIMIT 10");
        }
        // 查询所有空间的使用情况
        // 执行查询
        List<Space> spaceList = this.list(queryWrapper);
        // 如果没有查询到空间，返回空列表
        if (spaceList.isEmpty()) {
            return Collections.emptyList();
        }
        return spaceList;
    }
}
