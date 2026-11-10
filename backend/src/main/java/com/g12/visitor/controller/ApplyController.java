package com.g12.visitor.controller;

import com.g12.visitor.common.PageResult;
import com.g12.visitor.common.RequireRole;
import com.g12.visitor.common.Result;
import com.g12.visitor.common.RoleConstant;
import com.g12.visitor.dto.ApplyCreateDTO;
import com.g12.visitor.dto.RemarkDTO;
import com.g12.visitor.service.ApplyService;
import com.g12.visitor.vo.ApplyDetailVO;
import com.g12.visitor.vo.ApplyListItemVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/applies")
@RequiredArgsConstructor
@RequireRole({RoleConstant.EMPLOYEE, RoleConstant.ADMIN})
public class ApplyController {

    private final ApplyService applyService;

    @PostMapping
    public Result<Long> create(@Valid @RequestBody ApplyCreateDTO dto) {
        return Result.ok(applyService.create(dto));
    }

    @GetMapping("/mine")
    public Result<PageResult<ApplyListItemVO>> mine(@RequestParam(required = false) Integer status,
                                                    @RequestParam(required = false) Integer page,
                                                    @RequestParam(required = false) Integer size) {
        return Result.ok(applyService.mine(status, page, size));
    }

    @GetMapping("/{id}")
    public Result<ApplyDetailVO> detail(@PathVariable Long id) {
        return Result.ok(applyService.detail(id));
    }

    @PutMapping("/{id}/cancel")
    public Result<Void> cancel(@PathVariable Long id, @RequestBody(required = false) RemarkDTO dto) {
        applyService.cancel(id, dto == null ? null : dto.pick());
        return Result.ok();
    }
}
