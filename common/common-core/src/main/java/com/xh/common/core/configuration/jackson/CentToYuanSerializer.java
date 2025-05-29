package com.xh.common.core.configuration.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.ContextualSerializer;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;


/**
 * @author xuwentao
 * @version 1.0
 * @date 2025/5/29
 * @description 分转元
 */
@NoArgsConstructor
@AllArgsConstructor
public class CentToYuanSerializer extends JsonSerializer<Number>
        implements ContextualSerializer {

    private int scale;
    private RoundingMode rounding;

    @Override
    public void serialize(Number value, JsonGenerator gen, SerializerProvider provider)
            throws IOException {
        BigDecimal yuan = new BigDecimal(value.toString())
                .divide(new BigDecimal("100"), scale, rounding);
        gen.writeNumber(yuan.stripTrailingZeros());
    }

    @Override
    public JsonSerializer<?> createContextual(SerializerProvider serializerProvider, BeanProperty beanProperty) throws JsonMappingException {
        MoneyFormat ann = beanProperty.getAnnotation(MoneyFormat.class);
        if (ann != null) {
            return new CentToYuanSerializer(ann.scale(), ann.rounding());
        }
        return this; // 默认配置
    }
}

