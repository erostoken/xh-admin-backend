package com.xh.business.ddd;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import org.springframework.beans.BeanUtils;

/**
 * @author xuwentao
 * @version 1.0
 * @date 2025/6/6
 * @description 备注信息
 */
public class PageUtils {

    public static <T, R> Page<R> toPage(Page<T> page, List<R> recordList) {
        Page<R> resultPage = new Page<>();
        resultPage.setRecords(recordList);
        BeanUtils.copyProperties(page, resultPage);
        return resultPage;
    }

    public static <T, R> Page<R> toPage(Page<T> page, Function<T, R> function) {
        Page<R> resultPage = new Page<>();
        BeanUtils.copyProperties(page, resultPage);
        if(CollUtil.isNotEmpty(page.getRecords())) {
            resultPage.setRecords(page.getRecords().stream().map(function).toList());
        }
        return resultPage;
    }

}
