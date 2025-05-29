package com.xh.common.core.configuration.jackson;

import com.fasterxml.jackson.annotation.JacksonAnnotationsInside;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.math.RoundingMode;

/**
 * @author xuwentao
 * @version 1.0
 * @date 2025/5/29
 * @description 备注信息
 */
@Retention(RetentionPolicy.RUNTIME)
@JacksonAnnotationsInside // 关键：声明这是组合注解
@JsonSerialize(using = CentToYuanSerializer.class)
@JsonDeserialize(using = YuanToCentDeserializer.class)
public @interface MoneyFormat {

    /**
     * 可扩展小数位数
     */
    int scale() default 2;

    /**
     * 舍入策略
     */
    RoundingMode rounding() default RoundingMode.HALF_UP;
}
