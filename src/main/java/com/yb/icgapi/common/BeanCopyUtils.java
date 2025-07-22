package com.yb.icgapi.common;

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import java.beans.PropertyDescriptor;
import java.util.HashSet;
import java.util.Set;

public class BeanCopyUtils {

    /**
     * 获取一个对象中所有值为null的属性名数组
     * @param source 要检查的对象
     * @return 值为null的属性名数组
     */
    public static String[] getNullPropertyNames(Object source) {
        // 使用Spring的BeanWrapper来访问对象的属性
        final BeanWrapper src = new BeanWrapperImpl(source);
        PropertyDescriptor[] pds = src.getPropertyDescriptors();

        Set<String> emptyNames = new HashSet<>();
        for (PropertyDescriptor pd : pds) {
            Object srcValue = src.getPropertyValue(pd.getName());
            if (srcValue == null) {
                emptyNames.add(pd.getName());
            }
        }
        String[] result = new String[emptyNames.size()];
        return emptyNames.toArray(result);
    }
}