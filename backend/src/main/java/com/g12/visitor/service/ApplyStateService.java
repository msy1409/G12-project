package com.g12.visitor.service;

import com.g12.visitor.common.ApplyStatus;
import com.g12.visitor.common.AuthContext;
import com.g12.visitor.common.BizException;
import com.g12.visitor.common.CurrentUser;
import com.g12.visitor.common.ErrorCode;
import com.g12.visitor.entity.ApplyProcessRecord;
import com.g12.visitor.entity.BizStatusChangeLog;
import com.g12.visitor.entity.VisitorApply;
import com.g12.visitor.mapper.ApplyProcessRecordMapper;
import com.g12.visitor.mapper.BizStatusChangeLogMapper;
import com.g12.visitor.mapper.VisitorApplyMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 状态流转唯一出口。
 *
 * <p>事务三件套：乐观锁改状态 → 写 biz_status_change_log → 写 apply_process_record。
 * 任意一步失败整体回滚；updateStatus 影响行数为 0 说明状态已被他人改动，抛 400 STATE_NOT_ALLOWED。
 */
@Service
@RequiredArgsConstructor
public class ApplyStateService {

    private final VisitorApplyMapper applyMapper;
    private final BizStatusChangeLogMapper statusLogMapper;
    private final ApplyProcessRecordMapper processMapper;

    /**
     * 合法流转：0→1、0→9、1→2、1→9、2→3、2→9、3→4。
     */
    @Transactional(rollbackFor = Exception.class)
    public void transit(VisitorApply apply, int fromStatus, int toStatus, String action, String remark) {
        CurrentUser operator = AuthContext.get();
        int rows = applyMapper.updateStatus(apply.getId(), fromStatus, toStatus);
        if (rows == 0) {
            throw BizException.badRequest(ErrorCode.STATE_NOT_ALLOWED);
        }
        writeStatusLog(apply.getId(), fromStatus, toStatus, action, operator, remark);
        writeProcess(apply.getId(), action, operator, remark);
    }

    /**
     * 新建预约：状态从「无」(-1) 变为 0，也要留审计与处理记录。
     */
    @Transactional(rollbackFor = Exception.class)
    public void recordCreate(VisitorApply apply, CurrentUser operator) {
        writeStatusLog(apply.getId(), ApplyStatus.FROM_NONE, ApplyStatus.PENDING, "CREATE", operator, "新建预约");
        writeProcess(apply.getId(), "CREATE", operator, "新建预约");
    }

    private void writeStatusLog(Long applyId, int from, int to, String action, CurrentUser operator, String remark) {
        BizStatusChangeLog log = new BizStatusChangeLog();
        log.setApplyId(applyId);
        log.setFromStatus(from);
        log.setToStatus(to);
        log.setAction(action);
        log.setOperatorId(operator.getId());
        log.setOperatorName(operator.getRealName());
        log.setRemark(remark);
        statusLogMapper.insert(log);
    }

    private void writeProcess(Long applyId, String action, CurrentUser operator, String remark) {
        ApplyProcessRecord record = new ApplyProcessRecord();
        record.setApplyId(applyId);
        record.setAction(action);
        record.setOperatorId(operator.getId());
        record.setOperatorName(operator.getRealName());
        record.setOperatorRole(operator.getRole());
        record.setRemark(remark);
        processMapper.insert(record);
    }
}
