package com.xh.business.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xh.business.domain.model.ApiProductInfo;
import com.xh.business.mapper.ApiProductInfoMapper;
import com.xh.business.service.ApiProductInfoService;
import com.xh.business.utils.BusinessException;
import com.xh.business.utils.ErrorCode;
import java.util.Date;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

/**
* @author apple
* @description 针对表【api_product_info(产品信息)】的数据库操作Service实现
* @createDate 2025-05-26 16:55:21
*/
@Service
public class ApiProductInfoServiceImpl extends ServiceImpl<ApiProductInfoMapper, ApiProductInfo>
    implements ApiProductInfoService {

    @Override
    public void validProductInfo(ApiProductInfo productInfo, boolean add) {
        if (productInfo == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        String name = productInfo.getName();
        String description = productInfo.getDescription();
        Long total = productInfo.getAmount();
        Date expirationTime = productInfo.getExpirationTime();
        String productType = productInfo.getProductType();
        Long addPoints = productInfo.getAddPoints();
        // 创建时，所有参数必须非空
        if (add) {
            if (StringUtils.isAnyBlank(name)) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR);
            }
        }
        if (addPoints < 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "增加积分不能为负数");
        }
        if (total < 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "售卖金额不能为负数");
        }
    }

}




