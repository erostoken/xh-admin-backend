package com.xh.common.core.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @author xuwentao
 * @version 1.0
 * @date 2025/5/30
 * @description 外部用户信息
 */
@Data
@Schema(title = "外部用户DTO")
public class ExUserInfoDTO {

    @Schema(title = "外部用户Key")
    private String userKey;
    @Schema(title = "token")
    private String token;
    @Schema(title = "自动续签，请求会自动延长token失效时间")
    private Boolean autoRenewal;

}
