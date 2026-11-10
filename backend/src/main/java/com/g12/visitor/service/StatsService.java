package com.g12.visitor.service;

import com.g12.visitor.common.AuthContext;
import com.g12.visitor.common.CurrentUser;
import com.g12.visitor.common.RoleConstant;
import com.g12.visitor.mapper.StatsMapper;
import com.g12.visitor.vo.DateCountVO;
import com.g12.visitor.vo.DeptStatsVO;
import com.g12.visitor.vo.StatsOverviewVO;
import com.g12.visitor.vo.TrendVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class StatsService {

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final int DEFAULT_DAYS = 7;

    private final StatsMapper statsMapper;

    public StatsOverviewVO overview() {
        return statsMapper.selectOverview(scopeDept());
    }

    /**
     * 近 N 天趋势，服务端补齐没有数据的日期（前端直接画图，不用自己补）。
     */
    public List<TrendVO> trend(Integer days) {
        int n = days == null || days < 1 ? DEFAULT_DAYS : Math.min(days, 90);
        Long deptId = scopeDept();
        Map<String, Long> applyMap = toMap(statsMapper.selectApplyTrend(deptId, n));
        Map<String, Long> checkinMap = toMap(statsMapper.selectCheckinTrend(deptId, n));

        List<TrendVO> result = new ArrayList<>(n);
        LocalDate today = LocalDate.now();
        for (int i = n - 1; i >= 0; i--) {
            String date = today.minusDays(i).format(DAY);
            TrendVO vo = new TrendVO();
            vo.setDate(date);
            vo.setApplyCount(applyMap.getOrDefault(date, 0L));
            vo.setCheckinCount(checkinMap.getOrDefault(date, 0L));
            result.add(vo);
        }
        return result;
    }

    public List<DeptStatsVO> department() {
        return statsMapper.selectDeptStats();
    }

    private Long scopeDept() {
        CurrentUser user = AuthContext.get();
        return RoleConstant.AUDITOR.equals(user.getRole()) ? user.getDeptId() : null;
    }

    private Map<String, Long> toMap(List<DateCountVO> rows) {
        Map<String, Long> map = new HashMap<>();
        for (DateCountVO row : rows) {
            map.put(row.getDate(), row.getCnt());
        }
        return map;
    }
}
