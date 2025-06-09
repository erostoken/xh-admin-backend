package com.xh.business.controller.ex;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xh.business.ddd.user.ExApiInterfaceAggregate;
import com.xh.business.domain.req.interfaceinfo.InterfaceInfoQueryRequest;
import com.xh.business.domain.resp.api.InterfaceInfoVO;
import com.xh.business.utils.BusinessException;
import com.xh.business.utils.ErrorCode;
import com.xh.common.core.configuration.ExUser;
import com.xh.common.core.web.IdRequest;
import com.xh.common.core.web.PageQuery;
import com.xh.common.core.web.RestResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author xuwentao
 * @version 1.0
 * @date 2025/6/4
 * @description 备注信息
 */
@ExUser
@RestController
@RequestMapping("/api/ex/interface")
@Slf4j
@Tag(name = "外部API-接口")
public class ExInterfaceController {

    @Resource
    private ExApiInterfaceAggregate exApiInterfaceAggregate;

    /**
     * 接口分类列表
     * @return {@link RestResponse}<{@link List}<{@link Map}>>
     */
    @Operation(description = "接口分类列表")
    @GetMapping("/category/list")
    public RestResponse<List<Map<String, Object>>> categoryList() {
        return RestResponse.success(exApiInterfaceAggregate.categoryList());
    }

    /**
     * 分页获取列表
     * @param pageQuery 接口信息查询请求
     * @return {@link RestResponse}<{@link Page}<{@link InterfaceInfoVO}>>
     */
    @Operation(description = "分页获取列表")
    @PostMapping("/list/page")
    public RestResponse<Page<InterfaceInfoVO>> listInterfaceInfoByPage(@RequestBody PageQuery<InterfaceInfoQueryRequest> pageQuery) {
        return RestResponse.success(exApiInterfaceAggregate.listInterfaceInfoByPage(pageQuery));
    }

    /**
     * 通过id获取接口信息
     *
     * @param id id
     * @return {@link RestResponse}<{@link InterfaceInfoVO}>
     */
    @Operation(description = "通过id获取接口信息")
    @GetMapping("/get")
    public RestResponse<InterfaceInfoVO> getInterfaceInfoById(long id) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        return RestResponse.success(exApiInterfaceAggregate.getById(id));
    }

    /**
     * 申请接口
     *
     * @param idRequest id请求
     * @return {@link RestResponse}<{@link InterfaceInfoVO}>
     */
    @Operation(description = "申请接口")
    @PostMapping("/apply")
    public RestResponse<Void> applyInterfaceInfo(@RequestBody IdRequest idRequest) {
        if (Objects.isNull(idRequest.getId()) || idRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        exApiInterfaceAggregate.applyInterfaceInfo(idRequest.getId());
        return RestResponse.success();
    }

    /**
     * 重新申请接口
     *
     * @param idRequest id请求
     * @return {@link RestResponse}<{@link Void}>
     */
    @Operation(description = "重新申请接口")
    @PostMapping("/reapply")
    public RestResponse<Void> reapplyInterfaceInfo(@RequestBody IdRequest idRequest) {
        if (Objects.isNull(idRequest.getId()) || idRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        exApiInterfaceAggregate.reapplyInterfaceInfo(idRequest.getId());
        return RestResponse.success();
    }

    /**
     * 申请获取列表
     * @param pageQuery 接口信息查询请求
     * @return {@link RestResponse}<{@link Page}<{@link InterfaceInfoVO}>>
     */
    @Operation(description = "申请获取列表")
    @PostMapping("/apply/page")
    public RestResponse<Page<InterfaceInfoVO>> applyInterfaceInfoByPage(@RequestBody PageQuery<InterfaceInfoQueryRequest> pageQuery) {
        return RestResponse.success(exApiInterfaceAggregate.applyInterfaceInfoByPage(pageQuery));
    }


}
