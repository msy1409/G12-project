-- ============================================================
-- G12 访客预约与到访核验平台 · 建表脚本
-- 库：ctts_g12　字符集：utf8mb4　排序：utf8mb4_general_ci　引擎：InnoDB
-- 与 docs/database-design.md 逐字一致；改表结构先改本文件，再改文档，再通知 A/C
-- ============================================================

CREATE DATABASE IF NOT EXISTS `ctts_g12`
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_general_ci;

USE `ctts_g12`;

SET NAMES utf8mb4;

-- 重跑友好：先关外键检查，按依赖反序删表
SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS `biz_status_change_log`;
DROP TABLE IF EXISTS `apply_process_record`;
DROP TABLE IF EXISTS `visitor_record`;
DROP TABLE IF EXISTS `visitor_apply`;
DROP TABLE IF EXISTS `sys_user`;
DROP TABLE IF EXISTS `sys_department`;
SET FOREIGN_KEY_CHECKS = 1;

-- ------------------------------------------------------------
-- 1. sys_department　部门表
-- ------------------------------------------------------------
CREATE TABLE `sys_department` (
  `id`           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `dept_code`    VARCHAR(32)     NOT NULL                COMMENT '部门编码，如 D001',
  `dept_name`    VARCHAR(64)     NOT NULL                COMMENT '部门名称',
  `manager_name` VARCHAR(32)     DEFAULT NULL            COMMENT '部门负责人',
  `phone`        VARCHAR(20)     DEFAULT NULL            COMMENT '办公电话',
  `status`       TINYINT         NOT NULL DEFAULT 1      COMMENT '1启用 / 0停用',
  `create_time`  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dept_code` (`dept_code`),
  UNIQUE KEY `uk_dept_name` (`dept_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='部门表';

-- ------------------------------------------------------------
-- 2. sys_user　用户表
-- ------------------------------------------------------------
CREATE TABLE `sys_user` (
  `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `username`    VARCHAR(32)     NOT NULL                COMMENT '登录名，唯一',
  `password`    VARCHAR(100)    NOT NULL                COMMENT 'BCrypt 密文，禁止存明文',
  `real_name`   VARCHAR(32)     NOT NULL                COMMENT '真实姓名',
  `phone`       VARCHAR(20)     DEFAULT NULL            COMMENT '手机号',
  `dept_id`     BIGINT UNSIGNED NOT NULL                COMMENT '所属部门',
  `role`        VARCHAR(20)     NOT NULL                COMMENT 'EMPLOYEE / AUDITOR / ADMIN',
  `status`      TINYINT         NOT NULL DEFAULT 1      COMMENT '1启用 / 0停用（停用即不可登录）',
  `create_time` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  KEY `idx_user_dept` (`dept_id`),
  KEY `idx_user_role` (`role`),
  CONSTRAINT `fk_user_dept` FOREIGN KEY (`dept_id`) REFERENCES `sys_department` (`id`)
    ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='用户表';

-- ------------------------------------------------------------
-- 3. visitor_apply　预约主单表
-- ------------------------------------------------------------
CREATE TABLE `visitor_apply` (
  `id`             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `apply_no`       VARCHAR(32)     NOT NULL                COMMENT '预约单号 AP+yyyyMMdd+6位序号',
  `visitor_name`   VARCHAR(32)     NOT NULL                COMMENT '访客姓名',
  `visitor_phone`  VARCHAR(20)     NOT NULL                COMMENT '访客手机号',
  `visitor_company`VARCHAR(64)     DEFAULT NULL            COMMENT '访客单位',
  `dept_id`        BIGINT UNSIGNED NOT NULL                COMMENT '被访部门（AUDITOR 审核范围依据）',
  `host_name`      VARCHAR(32)     DEFAULT NULL            COMMENT '被访人姓名',
  `visit_reason`   VARCHAR(255)    DEFAULT NULL            COMMENT '来访事由',
  `visit_time`     DATETIME        NOT NULL                COMMENT '预计到访时间',
  `leave_time`     DATETIME        DEFAULT NULL            COMMENT '预计离开时间',
  `verify_code`    VARCHAR(6)      DEFAULT NULL            COMMENT '6位核验码，状态2时生成',
  `status`         TINYINT         NOT NULL DEFAULT 0      COMMENT '0待审核/1审核通过/2待到访/3已到访/4已离开/9驳回或取消',
  `create_by`      BIGINT UNSIGNED NOT NULL                COMMENT '预约发起人（员工）',
  `create_time`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_apply_no` (`apply_no`),
  KEY `idx_apply_status` (`status`),
  KEY `idx_apply_dept_status` (`dept_id`, `status`),
  KEY `idx_apply_create_by` (`create_by`),
  KEY `idx_apply_visit_time` (`visit_time`),
  CONSTRAINT `fk_apply_dept` FOREIGN KEY (`dept_id`) REFERENCES `sys_department` (`id`)
    ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_apply_user` FOREIGN KEY (`create_by`) REFERENCES `sys_user` (`id`)
    ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='预约主单表';

-- ------------------------------------------------------------
-- 4. visitor_record　到访离场登记表
-- ------------------------------------------------------------
CREATE TABLE `visitor_record` (
  `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `apply_id`      BIGINT UNSIGNED NOT NULL                COMMENT '关联预约单，唯一',
  `checkin_time`  DATETIME        NOT NULL                COMMENT '入场登记时间',
  `checkout_time` DATETIME        DEFAULT NULL            COMMENT '离场登记时间，NULL 表示仍在场',
  `checkin_by`    BIGINT UNSIGNED NOT NULL                COMMENT '入场登记操作人',
  `checkout_by`   BIGINT UNSIGNED DEFAULT NULL            COMMENT '离场登记操作人',
  `gate`          VARCHAR(32)     DEFAULT NULL            COMMENT '门岗标识，如 东门-01',
  `create_time`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_record_apply` (`apply_id`),
  KEY `idx_record_checkin` (`checkin_time`),
  CONSTRAINT `fk_record_apply` FOREIGN KEY (`apply_id`) REFERENCES `visitor_apply` (`id`)
    ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='到访离场登记表';

-- ------------------------------------------------------------
-- 5. apply_process_record　业务操作处理记录表
-- ------------------------------------------------------------
CREATE TABLE `apply_process_record` (
  `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `apply_id`      BIGINT UNSIGNED NOT NULL                COMMENT '关联预约单',
  `action`        VARCHAR(32)     NOT NULL                COMMENT 'CREATE/APPROVE/REJECT/VERIFY_CODE/CHECKIN/CHECKOUT/CANCEL',
  `operator_id`   BIGINT UNSIGNED NOT NULL                COMMENT '操作人ID',
  `operator_name` VARCHAR(32)     NOT NULL                COMMENT '操作人姓名（冗余，防改名后无法追溯）',
  `operator_role` VARCHAR(20)     NOT NULL                COMMENT '操作时的角色',
  `remark`        VARCHAR(255)    DEFAULT NULL            COMMENT '备注 / 驳回原因',
  `create_time`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '处理时间',
  PRIMARY KEY (`id`),
  KEY `idx_process_apply` (`apply_id`),
  KEY `idx_process_action` (`action`),
  KEY `idx_process_time` (`create_time`),
  CONSTRAINT `fk_process_apply` FOREIGN KEY (`apply_id`) REFERENCES `visitor_apply` (`id`)
    ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='业务操作处理记录表';

-- ------------------------------------------------------------
-- 6. biz_status_change_log　状态变更审计日志表
-- ------------------------------------------------------------
CREATE TABLE `biz_status_change_log` (
  `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `apply_id`      BIGINT UNSIGNED NOT NULL                COMMENT '关联预约单',
  `from_status`   TINYINT         NOT NULL                COMMENT '变更前状态（新建记为 -1）',
  `to_status`     TINYINT         NOT NULL                COMMENT '变更后状态',
  `action`        VARCHAR(32)     NOT NULL                COMMENT '触发动作，与处理记录 action 对齐',
  `operator_id`   BIGINT UNSIGNED NOT NULL                COMMENT '操作人ID',
  `operator_name` VARCHAR(32)     NOT NULL                COMMENT '操作人姓名',
  `remark`        VARCHAR(255)    DEFAULT NULL            COMMENT '备注',
  `create_time`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '变更时间',
  PRIMARY KEY (`id`),
  KEY `idx_statuslog_apply` (`apply_id`),
  KEY `idx_statuslog_time` (`create_time`),
  CONSTRAINT `fk_statuslog_apply` FOREIGN KEY (`apply_id`) REFERENCES `visitor_apply` (`id`)
    ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='状态变更审计日志表';

-- ============================================================
-- 建表完成：6 张表、6 条外键
-- ============================================================
SELECT 'schema.sql done: 6 tables created in ctts_g12' AS result;
