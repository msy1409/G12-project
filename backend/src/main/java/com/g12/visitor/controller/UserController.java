package com.g12.visitor.controller;

import com.g12.visitor.common.PageResult;
import com.g12.visitor.common.RequireRole;
import com.g12.visitor.common.Result;
import com.g12.visitor.common.RoleConstant;
import com.g12.visitor.dto.UserCreateDTO;
import com.g12.visitor.dto.UserUpdateDTO;
import com.g12.visitor.service.UserService;
import com.g12.visitor.vo.UserVO;
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

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@RequireRole(RoleConstant.ADMIN)
public class UserController {

    private final UserService userService;

    @GetMapping
    public Result<PageResult<UserVO>> page(@RequestParam(required = false) Integer page,
                                           @RequestParam(required = false) Integer size,
                                           @RequestParam(required = false) String keyword,
                                           @RequestParam(required = false) String role,
                                           @RequestParam(required = false) Long deptId,
                                           @RequestParam(required = false) Integer status) {
        return Result.ok(userService.page(page, size, keyword, role, deptId, status));
    }

    @GetMapping("/{id}")
    public Result<UserVO> detail(@PathVariable Long id) {
        return Result.ok(userService.detail(id));
    }

    @PostMapping
    public Result<Long> create(@Valid @RequestBody UserCreateDTO dto) {
        return Result.ok(userService.create(dto));
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody UserUpdateDTO dto) {
        userService.update(id, dto);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> disable(@PathVariable Long id) {
        userService.disable(id);
        return Result.ok();
    }
}
