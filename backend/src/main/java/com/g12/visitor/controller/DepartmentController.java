package com.g12.visitor.controller;

import com.g12.visitor.common.RequireRole;
import com.g12.visitor.common.Result;
import com.g12.visitor.common.RoleConstant;
import com.g12.visitor.dto.DeptCreateDTO;
import com.g12.visitor.dto.DeptUpdateDTO;
import com.g12.visitor.service.DepartmentService;
import com.g12.visitor.vo.DeptVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/departments")
@RequiredArgsConstructor
public class DepartmentController {

    private final DepartmentService departmentService;

    /**
     * 三个角色都要用它填「部门」下拉，所以不限制角色。
     */
    @GetMapping
    public Result<List<DeptVO>> list(@RequestParam(required = false) Integer status) {
        return Result.ok(departmentService.list(status));
    }

    @PostMapping
    @RequireRole(RoleConstant.ADMIN)
    public Result<Long> create(@Valid @RequestBody DeptCreateDTO dto) {
        return Result.ok(departmentService.create(dto));
    }

    @PutMapping("/{id}")
    @RequireRole(RoleConstant.ADMIN)
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody DeptUpdateDTO dto) {
        departmentService.update(id, dto);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @RequireRole(RoleConstant.ADMIN)
    public Result<Void> disable(@PathVariable Long id) {
        departmentService.disable(id);
        return Result.ok();
    }
}
