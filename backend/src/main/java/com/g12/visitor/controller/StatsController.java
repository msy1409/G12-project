package com.g12.visitor.controller;

import com.g12.visitor.common.RequireRole;
import com.g12.visitor.common.Result;
import com.g12.visitor.common.RoleConstant;
import com.g12.visitor.service.StatsService;
import com.g12.visitor.vo.DeptStatsVO;
import com.g12.visitor.vo.StatsOverviewVO;
import com.g12.visitor.vo.TrendVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/stats")
@RequiredArgsConstructor
@RequireRole({RoleConstant.AUDITOR, RoleConstant.ADMIN})
public class StatsController {

    private final StatsService statsService;

    @GetMapping("/overview")
    public Result<StatsOverviewVO> overview() {
        return Result.ok(statsService.overview());
    }

    @GetMapping("/trend")
    public Result<List<TrendVO>> trend(@RequestParam(required = false) Integer days) {
        return Result.ok(statsService.trend(days));
    }

    @GetMapping("/department")
    public Result<List<DeptStatsVO>> department() {
        return Result.ok(statsService.department());
    }
}
