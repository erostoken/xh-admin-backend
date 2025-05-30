package com.xh.business.ddd.user;

import com.xh.business.service.ApiProductInfoService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * @author xuwentao
 * @version 1.0
 * @date 2025/5/30
 * @description 备注信息
 */
@Slf4j
@Component
public class ExApiProductAggregate {

    @Resource
    private ApiProductInfoService apiProductInfoService;



}
