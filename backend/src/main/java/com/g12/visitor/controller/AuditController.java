package com.g12.visitor.controller;

import com.g12.visitor.common.PageResult;
import com.g12.visitor.common.RequireRole;
import com.g12.visitor.common.Result;
import com.g12.visitor.common.RoleConstant;
import com.g12.visitor.dto.RejectDTO;
import com.g12.visitor.dto.RemarkDTO;
import com.g12.visitor.service.AuditService;
import com.g12.visitor.vo.ApplyListItemVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/audit")
@RequiredArgsConstructor
@RequireRole({RoleConstant.AUDITOR, RoleConstant.ADMIN})
public class AuditController {

    private final AuditService auditService;

    @GetMapping("/pending")
    public Result<PageResult<ApplyListItemVO>> pending(@RequestParam(required = false) Integer page,
                                                       @RequestParam(required = false) Integer size,
                                                       @RequestParam(required = false) String keyword) {
        return Result.ok(auditService.pending(page, size, keyword));
    }

    @GetMapping("/list")
    public Result<PageResult<ApplyListItemVO>> list(@RequestParam(required = false) Integer status,
                                                    @RequestParam(required = false) String keyword,
                                                    @RequestParam(required = false) String startDate,
                                                    @RequestParam(required = false) String endDate,
                                                    @RequestParam(required = false) Integer page,
                                                    @RequestParam(required = false) Integer size) {
        return Result.ok(auditService.list(status, keyword, startDate, endDate, page, size));
    }

    @PutMapping("/{id}/approve")
    public Result<Void> approve(@PathVariable Long id, @RequestBody(required = false) RemarkDTO dto) {
        auditService.approve(id, dto == null ? null : dto.pick());
        return Result.ok();
    }

    @PutMapping("/{id}/reject")
    public Result<Void> reject(@PathVariable Long id, @Valid @RequestBody RejectDTO dto) {
        auditService.reject(id, dto.getReason());
        return Result.ok();
    }

    @PutMapping("/{id}/verify-code")
    public Result<Map<String, String>> verifyCode(@PathVariable Long id) {
        return Result.ok(Map.of("verifyCode", auditService.verifyCode(id)));
    }
}
