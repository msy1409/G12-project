package com.g12.visitor.service;

import com.g12.visitor.common.ApplyStatus;
import com.g12.visitor.common.AuthContext;
import com.g12.visitor.common.BizException;
import com.g12.visitor.common.CurrentUser;
import com.g12.visitor.common.ErrorCode;
import com.g12.visitor.common.PageQuery;
import com.g12.visitor.common.PageResult;
import com.g12.visitor.common.RoleConstant;
import com.g12.visitor.entity.VisitorApply;
import com.g12.visitor.mapper.VisitorApplyMapper;
import com.g12.visitor.vo.ApplyListItemVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final VisitorApplyMapper applyMapper;
    private final ApplyStateService stateService;

    public PageResult<ApplyListItemVO> pending(Integer page, Integer size, String keyword) {
        CurrentUser user = AuthContext.get();
        Long deptId = scopeDept(user);
        int p = PageQuery.page(page);
        int s = PageQuery.size(size);
        List<ApplyListItemVO> list = applyMapper.selectPending(deptId, keyword, PageQuery.offset(p, s), s);
        long total = applyMapper.countPending(deptId, keyword);
        return PageResult.of(list, total, p, s);
    }

    public PageResult<ApplyListItemVO> list(Integer status, String keyword, String startDate, String endDate,
                                            Integer page, Integer size) {
        CurrentUser user = AuthContext.get();
        Long deptId = scopeDept(user);
        int p = PageQuery.page(page);
        int s = PageQuery.size(size);
        List<ApplyListItemVO> list = applyMapper.selectAuditList(deptId, status, keyword, startDate, endDate,
                PageQuery.offset(p, s), s);
        long total = applyMapper.countAuditList(deptId, status, keyword, startDate, endDate);
        return PageResult.of(list, total, p, s);
    }

    @Transactional(rollbackFor = Exception.class)
    public void approve(Long id, String remark) {
        VisitorApply apply = loadInScope(id);
        if (apply.getStatus() != ApplyStatus.PENDING) {
            throw BizException.badRequest(ErrorCode.STATE_NOT_ALLOWED);
        }
        stateService.transit(apply, ApplyStatus.PENDING, ApplyStatus.APPROVED, "APPROVE",
                remark == null || remark.isBlank() ? "审核通过" : remark);
    }

    @Transactional(rollbackFor = Exception.class)
    public void reject(Long id, String reason) {
        VisitorApply apply = loadInScope(id);
        if (apply.getStatus() != ApplyStatus.PENDING) {
            throw BizException.badRequest(ErrorCode.STATE_NOT_ALLOWED);
        }
        stateService.transit(apply, ApplyStatus.PENDING, ApplyStatus.CLOSED, "REJECT", reason);
    }

    /**
     * 审核通过后生成 6 位核验码，状态 1→2。
     */
    @Transactional(rollbackFor = Exception.class)
    public String verifyCode(Long id) {
        VisitorApply apply = loadInScope(id);
        if (apply.getStatus() != ApplyStatus.APPROVED) {
            throw BizException.badRequest(ErrorCode.STATE_NOT_ALLOWED);
        }
        String code = generateUniqueCode();
        applyMapper.updateVerifyCode(id, code);
        stateService.transit(apply, ApplyStatus.APPROVED, ApplyStatus.WAITING_VISIT, "VERIFY", "生成核验码");
        return code;
    }

    private String generateUniqueCode() {
        for (int i = 0; i < 3; i++) {
            String code = String.format("%06d", ThreadLocalRandom.current().nextInt(1_000_000));
            if (applyMapper.selectByVerifyCode(code) == null) {
                return code;
            }
        }
        throw BizException.badRequest(ErrorCode.INVALID_PARAM);
    }

    private Long scopeDept(CurrentUser user) {
        return RoleConstant.AUDITOR.equals(user.getRole()) ? user.getDeptId() : null;
    }

    private VisitorApply loadInScope(Long id) {
        VisitorApply apply = applyMapper.selectById(id);
        if (apply == null) {
            throw BizException.notFound();
        }
        CurrentUser user = AuthContext.get();
        if (RoleConstant.AUDITOR.equals(user.getRole()) && !apply.getDeptId().equals(user.getDeptId())) {
            throw BizException.forbidden(ErrorCode.DEPT_SCOPE_DENIED);
        }
        return apply;
    }
}
