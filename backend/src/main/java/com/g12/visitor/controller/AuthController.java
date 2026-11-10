package com.g12.visitor.controller;

import com.g12.visitor.common.Result;
import com.g12.visitor.dto.LoginDTO;
import com.g12.visitor.service.AuthService;
import com.g12.visitor.vo.LoginVO;
import com.g12.visitor.vo.UserVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO dto) {
        return Result.ok(authService.login(dto));
    }

    @GetMapping("/me")
    public Result<UserVO> me() {
        return Result.ok(authService.currentUser());
    }

    /**
     * JWT 无状态，登出由前端丢弃 token；接口保留用于审计口径统一。
     */
    @PostMapping("/logout")
    public Result<Void> logout() {
        return Result.ok();
    }
}
