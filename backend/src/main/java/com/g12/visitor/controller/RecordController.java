package com.g12.visitor.controller;

import com.g12.visitor.common.PageResult;
import com.g12.visitor.common.RequireRole;
import com.g12.visitor.common.Result;
import com.g12.visitor.common.RoleConstant;
import com.g12.visitor.dto.CheckinDTO;
import com.g12.visitor.dto.CheckoutDTO;
import com.g12.visitor.service.RecordService;
import com.g12.visitor.vo.CheckinVO;
import com.g12.visitor.vo.CheckoutVO;
import com.g12.visitor.vo.RecordVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/records")
@RequiredArgsConstructor
@RequireRole({RoleConstant.AUDITOR, RoleConstant.ADMIN})
public class RecordController {

    private final RecordService recordService;

    @PostMapping("/checkin")
    public Result<CheckinVO> checkin(@Valid @RequestBody CheckinDTO dto) {
        return Result.ok(recordService.checkin(dto));
    }

    @PostMapping("/checkout")
    public Result<CheckoutVO> checkout(@Valid @RequestBody CheckoutDTO dto) {
        return Result.ok(recordService.checkout(dto));
    }

    @GetMapping
    public Result<PageResult<RecordVO>> page(@RequestParam(required = false) String date,
                                             @RequestParam(required = false) String keyword,
                                             @RequestParam(required = false) Integer page,
                                             @RequestParam(required = false) Integer size) {
        return Result.ok(recordService.page(date, keyword, page, size));
    }
}
