package com.yb.icgapi.manager.sharding;

import lombok.extern.slf4j.Slf4j;
import org.apache.shardingsphere.sharding.api.sharding.standard.PreciseShardingValue;
import org.apache.shardingsphere.sharding.api.sharding.standard.RangeShardingValue;
import org.apache.shardingsphere.sharding.api.sharding.standard.StandardShardingAlgorithm;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
@Slf4j
public class PictureShardingAlgorithm implements StandardShardingAlgorithm<Long> {
    @Override
    public String doSharding(Collection<String> collection,
                             PreciseShardingValue<Long> preciseShardingValue) {
        log.info("do sharding{}",collection);
        Long spaceId = preciseShardingValue.getValue();
        String logicTableName = preciseShardingValue.getLogicTableName();
        String returnTableName = null;
        // spaceId 为 null 表示查询所有图片
        if (spaceId == null) {
            returnTableName=logicTableName;
        }else{
            String realTableName = "picture_" + spaceId;
            if( collection.contains(realTableName)) {
                returnTableName = realTableName;
            }else{
                // 如果没有对应的表，返回逻辑表名
                returnTableName = logicTableName;
            }
        }
        log.info("分表算法返回表:{}",returnTableName);
        return returnTableName;
    }

    @Override
    public Collection<String> doSharding(Collection<String> collection, RangeShardingValue<Long> rangeShardingValue) {
        log.info("do sharding range {}",collection);
        return new ArrayList<>();
    }

    @Override
    public Properties getProps() {
        return null;
    }

    @Override
    public void init(Properties properties) {

    }
}
