package com.g12.visitor.service;

import com.g12.visitor.common.ApplyStatus;
import com.g12.visitor.common.AuthContext;
import com.g12.visitor.common.BizException;
import com.g12.visitor.common.CurrentUser;
import com.g12.visitor.common.ErrorCode;
import com.g12.visitor.common.PageQuery;
import com.g12.visitor.common.PageResult;
import com.g12.visitor.common.RoleConstant;
import com.g12.visitor.dto.ApplyCreateDTO;
import com.g12.visitor.entity.VisitorApply;
import com.g12.visitor.mapper.ApplyProcessRecordMapper;
import com.g12.visitor.mapper.BizStatusChangeLogMapper;
import com.g12.visitor.mapper.VisitorApplyMapper;
import com.g12.visitor.vo.ApplyDetailVO;
import com.g12.visitor.vo.ApplyListItemVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class ApplyService {

    private static final DateTimeFormatter NO_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final VisitorApplyMapper applyMapper;
    private final ApplyProcessRecordMapper processMapper;
    private final BizStatusChangeLogMapper statusLogMapper;
    private final ApplyStateService stateService;

    @Transactional(rollbackFor = Exception.class)
    public Long create(ApplyCreateDTO dto) {
        CurrentUser user = AuthContext.get();
        Long deptId = dto.getDeptId();
        if (!RoleConstant.ADMIN.equals(user.getRole()) || deptId == null) {
            deptId = user.getDeptId();
        }
        if (deptId == null) {
            throw BizException.badRequest(ErrorCode.INVALID_PARAM);
        }
        VisitorApply apply = new VisitorApply();
        apply.setApplyNo("G12" + LocalDateTime.now().format(NO_FORMAT)
                + String.format("%03d", ThreadLocalRandom.current().nextInt(1000)));
        apply.setVisitorName(dto.getVisitorName());
        apply.setVisitorPhone(dto.getVisitorPhone());
        apply.setVisitorCompany(dto.getVisitorCompany());
        apply.setDeptId(deptId);
        apply.setHostName(dto.getHostName());
        apply.setVisitReason(dto.getVisitReason());
        apply.setVisitTime(dto.getVisitTime());
        apply.setLeaveTime(dto.getLeaveTime());
        apply.setStatus(ApplyStatus.PENDING);
        apply.setCreateBy(user.getId());
        applyMapper.insert(apply);
        stateService.recordCreate(apply, user);
        return apply.getId();
    }

    public PageResult<ApplyListItemVO> mine(Integer status, Integer page, Integer size) {
        CurrentUser user = AuthContext.get();
        int p = PageQuery.page(page);
        int s = PageQuery.size(size);
        List<ApplyListItemVO> list = applyMapper.selectMine(user.getId(), status, PageQuery.offset(p, s), s);
        long total = applyMapper.countMine(user.getId(), status);
        return PageResult.of(list, total, p, s);
    }

    public ApplyDetailVO detail(Long id) {
        VisitorApply apply = applyMapper.selectById(id);
        if (apply == null) {
            throw BizException.notFound();
        }
        checkReadable(apply);
        ApplyDetailVO vo = applyMapper.selectDetail(id);
        vo.setProcessList(processMapper.selectByApplyId(id));
        vo.setStatusLogList(statusLogMapper.selectByApplyId(id));
        return vo;
    }

    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long id, String reason) {
        VisitorApply apply = applyMapper.selectById(id);
        if (apply == null) {
            throw BizException.notFound();
        }
        checkReadable(apply);
        int status = apply.getStatus();
        if (status != ApplyStatus.PENDING && status != ApplyStatus.APPROVED && status != ApplyStatus.WAITING_VISIT) {
            throw BizException.badRequest(ErrorCode.STATE_NOT_ALLOWED);
        }
        stateService.transit(apply, status, ApplyStatus.CLOSED, "CANCEL",
                reason == null || reason.isBlank() ? "用户取消" : reason);
    }

    /**
     * EMPLOYEE 只能看自己创建的；AUDITOR 只能看本部门；ADMIN 全部。
     */
    private void checkReadable(VisitorApply apply) {
        CurrentUser user = AuthContext.get();
        if (RoleConstant.ADMIN.equals(user.getRole())) {
            return;
        }
        if (RoleConstant.AUDITOR.equals(user.getRole())) {
            if (!apply.getDeptId().equals(user.getDeptId())) {
                throw BizException.forbidden(ErrorCode.DEPT_SCOPE_DENIED);
            }
            return;
        }
        if (!apply.getCreateBy().equals(user.getId())) {
            throw BizException.forbidden(ErrorCode.FORBIDDEN);
        }
    }
}
