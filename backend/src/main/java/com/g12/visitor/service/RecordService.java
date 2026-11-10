package com.g12.visitor.service;

import com.g12.visitor.common.ApplyStatus;
import com.g12.visitor.common.AuthContext;
import com.g12.visitor.common.BizException;
import com.g12.visitor.common.CurrentUser;
import com.g12.visitor.common.ErrorCode;
import com.g12.visitor.common.PageQuery;
import com.g12.visitor.common.PageResult;
import com.g12.visitor.common.RoleConstant;
import com.g12.visitor.dto.CheckinDTO;
import com.g12.visitor.dto.CheckoutDTO;
import com.g12.visitor.entity.VisitorApply;
import com.g12.visitor.entity.VisitorRecord;
import com.g12.visitor.mapper.VisitorApplyMapper;
import com.g12.visitor.mapper.VisitorRecordMapper;
import com.g12.visitor.vo.CheckinVO;
import com.g12.visitor.vo.CheckoutVO;
import com.g12.visitor.vo.RecordVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RecordService {

    private final VisitorRecordMapper recordMapper;
    private final VisitorApplyMapper applyMapper;
    private final ApplyStateService stateService;

    @Transactional(rollbackFor = Exception.class)
    public CheckinVO checkin(CheckinDTO dto) {
        VisitorApply apply = applyMapper.selectByVerifyCode(dto.getVerifyCode());
        if (apply == null) {
            throw BizException.badRequest(ErrorCode.VERIFY_CODE_INVALID);
        }
        CurrentUser user = AuthContext.get();
        if (RoleConstant.AUDITOR.equals(user.getRole()) && !apply.getDeptId().equals(user.getDeptId())) {
            throw BizException.forbidden(ErrorCode.DEPT_SCOPE_DENIED);
        }
        if (apply.getStatus() != ApplyStatus.WAITING_VISIT) {
            throw BizException.badRequest(ErrorCode.STATE_NOT_ALLOWED);
        }
        if (recordMapper.selectByApplyId(apply.getId()) != null) {
            throw BizException.conflict(ErrorCode.DUPLICATE_CHECKIN);
        }
        LocalDateTime now = LocalDateTime.now();
        VisitorRecord record = new VisitorRecord();
        record.setApplyId(apply.getId());
        record.setCheckinTime(now);
        record.setCheckinBy(user.getId());
        record.setGate(dto.getGate());
        recordMapper.insert(record);

        stateService.transit(apply, ApplyStatus.WAITING_VISIT, ApplyStatus.CHECKED_IN, "CHECKIN", "门岗核验入场");

        CheckinVO vo = new CheckinVO();
        vo.setRecordId(record.getId());
        vo.setApplyId(apply.getId());
        vo.setVisitorName(apply.getVisitorName());
        vo.setCheckinTime(now);
        vo.setStatus(ApplyStatus.CHECKED_IN);
        return vo;
    }

    @Transactional(rollbackFor = Exception.class)
    public CheckoutVO checkout(CheckoutDTO dto) {
        VisitorApply apply = applyMapper.selectById(dto.getApplyId());
        if (apply == null) {
            throw BizException.notFound();
        }
        CurrentUser user = AuthContext.get();
        if (RoleConstant.AUDITOR.equals(user.getRole()) && !apply.getDeptId().equals(user.getDeptId())) {
            throw BizException.forbidden(ErrorCode.DEPT_SCOPE_DENIED);
        }
        if (apply.getStatus() != ApplyStatus.CHECKED_IN) {
            throw BizException.badRequest(ErrorCode.STATE_NOT_ALLOWED);
        }
        VisitorRecord record = recordMapper.selectByApplyId(apply.getId());
        if (record == null) {
            throw BizException.badRequest(ErrorCode.NOT_CHECKED_IN);
        }
        LocalDateTime now = LocalDateTime.now();
        VisitorRecord patch = new VisitorRecord();
        patch.setId(record.getId());
        patch.setCheckoutTime(now);
        patch.setCheckoutBy(user.getId());
        patch.setGate(dto.getGate());
        recordMapper.updateById(patch);

        stateService.transit(apply, ApplyStatus.CHECKED_IN, ApplyStatus.CHECKED_OUT, "CHECKOUT", "门岗登记离场");

        CheckoutVO vo = new CheckoutVO();
        vo.setApplyId(apply.getId());
        vo.setCheckoutTime(now);
        vo.setStayMinutes(Duration.between(record.getCheckinTime(), now).toMinutes());
        vo.setStatus(ApplyStatus.CHECKED_OUT);
        return vo;
    }

    public PageResult<RecordVO> page(String date, String keyword, Integer page, Integer size) {
        CurrentUser user = AuthContext.get();
        Long deptId = RoleConstant.AUDITOR.equals(user.getRole()) ? user.getDeptId() : null;
        int p = PageQuery.page(page);
        int s = PageQuery.size(size);
        List<RecordVO> list = recordMapper.selectRecordPage(date, deptId, keyword, PageQuery.offset(p, s), s);
        long total = recordMapper.countRecord(date, deptId, keyword);
        return PageResult.of(list, total, p, s);
    }
}
