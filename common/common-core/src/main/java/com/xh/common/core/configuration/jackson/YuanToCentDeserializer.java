package com.xh.common.core.configuration.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
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
 * @description 元转分
 */
@NoArgsConstructor
@AllArgsConstructor
public class YuanToCentDeserializer extends JsonDeserializer<Number>
        implements ContextualDeserializer {

    private int scale;
    private RoundingMode rounding;

    @Override
    public Number deserialize(JsonParser p, DeserializationContext ctx)
            throws IOException {
        BigDecimal yuan = p.getDecimalValue()
                .setScale(scale, rounding);
        return yuan.multiply(new BigDecimal("100")).longValue();
    }

    @Override
    public JsonDeserializer<?> createContextual(DeserializationContext deserializationContext, BeanProperty beanProperty) throws JsonMappingException {
        MoneyFormat ann = beanProperty.getAnnotation(MoneyFormat.class);
        if (ann != null) {
            return new YuanToCentDeserializer(ann.scale(), ann.rounding());
        }
        return this;
    }
}
