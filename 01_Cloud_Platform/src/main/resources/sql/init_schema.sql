-- 校安通云端平台数据库初始化脚本

-- 1. 创建数据库（如果不存在）
CREATE DATABASE IF NOT EXISTS security_db
CHARACTER SET utf8mb4
COLLATE utf8mb4_unicode_ci;

-- 2. 使用数据库
USE security_db;

-- 3. 创建设备表
CREATE TABLE IF NOT EXISTS sys_device (
                                          id INT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
                                          device_id VARCHAR(64) NOT NULL UNIQUE COMMENT '设备唯一标识',
    device_name VARCHAR(128) DEFAULT '未命名设备' COMMENT '设备名称',
    device_ip VARCHAR(64) DEFAULT '' COMMENT '设备IP地址',
    status TINYINT DEFAULT 0 COMMENT '设备状态：0-离线，1-在线，2-故障',
    firmware_version VARCHAR(32) DEFAULT 'v1.0' COMMENT '固件版本',
    last_active_time DATETIME COMMENT '最后活跃时间',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备信息表';

-- 4. 创建算法商城表
CREATE TABLE IF NOT EXISTS sys_skill (
    id INT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    skill_name VARCHAR(128) NOT NULL COMMENT '技能名称',
    skill_code VARCHAR(64) NOT NULL UNIQUE COMMENT '技能编码',
    version VARCHAR(32) NOT NULL DEFAULT 'v1.0' COMMENT '版本号',
    model_file_path VARCHAR(255) NOT NULL COMMENT '模型文件路径',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='算法商城表';

-- 5. 创建设备-技能关联表
CREATE TABLE IF NOT EXISTS sys_device_skill (
    id INT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    device_id INT NOT NULL COMMENT '设备主键ID',
    skill_id INT NOT NULL COMMENT '技能ID',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '运行状态',
    threshold DECIMAL(5,2) DEFAULT 0.00 COMMENT '报警阈值',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_device_skill (device_id, skill_id),
    CONSTRAINT fk_device_skill_device FOREIGN KEY (device_id) REFERENCES sys_device(id) ON DELETE CASCADE,
    CONSTRAINT fk_device_skill_skill FOREIGN KEY (skill_id) REFERENCES sys_skill(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备-技能关联表';

-- 6. 创建报警表
CREATE TABLE IF NOT EXISTS sys_alarm (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    device_id VARCHAR(64) NOT NULL COMMENT '设备编号(字符串，如box_001)',
    skill_id BIGINT NULL COMMENT '技能ID(预留)',
    alarm_type VARCHAR(32) NOT NULL COMMENT '报警类型',
    alarm_time DATETIME NOT NULL COMMENT '报警时间',
    img_path VARCHAR(255) NOT NULL COMMENT '图片路径',
    confidence FLOAT DEFAULT 0.0 COMMENT '置信度',
    is_handled TINYINT DEFAULT 0 COMMENT '是否处理 0未处理 1已确认 2误报',
    location VARCHAR(128) DEFAULT '' COMMENT '报警位置',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='报警记录表';

-- 4. 插入测试数据（幂等：不存在才插入）
INSERT INTO sys_device (device_id, device_name, device_ip, status)
SELECT 'box_001', '南门摄像头', '192.168.1.101', 0
WHERE NOT EXISTS (SELECT 1 FROM sys_device WHERE device_id='box_001');

INSERT INTO sys_device (device_id, device_name, device_ip, status)
SELECT 'box_002', '实验室A区', '192.168.1.102', 0
WHERE NOT EXISTS (SELECT 1 FROM sys_device WHERE device_id='box_002');

INSERT INTO sys_device (device_id, device_name, device_ip, status)
SELECT 'box_003', '走廊监控', '192.168.1.103', 0
WHERE NOT EXISTS (SELECT 1 FROM sys_device WHERE device_id='box_003');

-- 7. 创建用户表
CREATE TABLE IF NOT EXISTS sys_user (
    id INT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(64) NOT NULL UNIQUE,
    password VARCHAR(128) NOT NULL,
    real_name VARCHAR(64) DEFAULT '',
    role VARCHAR(32) DEFAULT 'user',
    status TINYINT DEFAULT 1,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统用户';

INSERT INTO sys_user (username, password, real_name, role, status)
SELECT 'admin', MD5('123456'), '管理员', 'admin', 1
WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE username='admin');

INSERT INTO sys_user (username, password, real_name, role, status)
SELECT 'user01', MD5('123456'), '安保员01', 'user', 1
WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE username='user01');
