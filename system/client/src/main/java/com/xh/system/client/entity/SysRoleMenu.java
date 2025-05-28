package com.xh.system.client.entity;

import com.xh.common.core.entity.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;


@Schema(title = "系统角色权限表")
@Table
@Data
@EqualsAndHashCode(callSuper = true)
public class SysRoleMenu extends BaseEntity<Long> {
    @Schema(title = "角色id")
    private Long sysRoleId;
    @Schema(title = "菜单id")
    private Long sysMenuId;
}
