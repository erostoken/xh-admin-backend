package com.xh.business.domain.enums;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @Author: QiMu
 * @Date: 2023/08/23 04:19:02
 * @Version: 1.0
 * @Description: 支付类型枚举类型
 */
public enum PayTypeStatusEnum {

    /**
     * 微信支付
     */
    WX("微信支付", "WX", 0),
    /**
     * 支付宝支付
     */
    ALIPAY("支付宝支付", "ALIPAY", 1);

    private final String text;

    private final String value;

    private final Integer status;

    PayTypeStatusEnum(String text, String value, Integer status) {
        this.text = text;
        this.value = value;
        this.status = status;
    }

    /**
     * 得到值
     * 获取值列表
     *
     * @return {@link List}<{@link Integer}>
     */
    public static List<String> getValues() {
        return Arrays.stream(values()).filter(item -> item.status == 1).map(item -> item.value).collect(Collectors.toList());
    }

    /**
     * 得到值
     * 获取值列表
     *
     * @return {@link List}<{@link Integer}>
     */
    public static List<Map<String, String>> getKvValues() {
        return Arrays.stream(values()).filter(item -> item.status == 1).map(item -> Map.of("label", item.text, "value", item.value)).collect(Collectors.toList());
    }

    public String getValue() {
        return value;
    }

    public String getText() {
        return text;
    }
}
