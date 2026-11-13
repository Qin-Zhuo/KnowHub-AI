
USE knowhub_ai;

# 用户管理模块的两张表
CREATE TABLE users (
                       id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '用户唯一标识',
                       username VARCHAR(255) NOT NULL UNIQUE COMMENT '用户名，唯一',
                       password VARCHAR(255) NOT NULL COMMENT '密码哈希',
                       role ENUM('USER', 'ADMIN') NOT NULL DEFAULT 'USER' COMMENT '用户角色',
                       enabled BOOLEAN NOT NULL DEFAULT TRUE COMMENT '账号是否启用',
                       org_tag_ids VARCHAR(300) DEFAULT NULL COMMENT '用户所属组织标签ID，多个用逗号分隔',
                       primary_org_tag_id VARCHAR(300) DEFAULT NULL COMMENT '用户主组织标签ID',
                       created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                       updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                           ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                       INDEX idx_username (username) COMMENT '用户名索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

CREATE TABLE organization_tags (
                                   tag_id VARCHAR(300) PRIMARY KEY COMMENT '标签唯一标识',
                                   name VARCHAR(100) NOT NULL COMMENT '标签名称',
                                   description TEXT COMMENT '描述',
                                   parent_tag_id VARCHAR(300) DEFAULT NULL COMMENT '父标签ID',
                                   created_by BIGINT NOT NULL COMMENT '创建者ID',
                                   created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                   updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                                       ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                   FOREIGN KEY (parent_tag_id)
                                       REFERENCES organization_tags(tag_id)
                                       ON DELETE SET NULL,
                                   FOREIGN KEY (created_by)
                                       REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='组织标签表';



# 文件上传模块的两张表
CREATE TABLE file_upload (
                             id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '文件记录ID',

                             file_md5 CHAR(32) NOT NULL COMMENT '文件内容MD5，用于校验，不作为业务主键',
                             file_name VARCHAR(255) NOT NULL COMMENT '用户上传时的原始文件名',
                             total_size BIGINT NOT NULL COMMENT '文件总字节数',

                             chunk_size BIGINT NOT NULL COMMENT '每个上传分片的目标大小',
                             total_chunks INT NOT NULL COMMENT '上传分片总数',

                             object_key VARCHAR(512) DEFAULT NULL COMMENT 'MinIO中合并后完整文件的对象键',
                             status VARCHAR(20) NOT NULL DEFAULT 'UPLOADING'
                                 COMMENT '文件状态：UPLOADING、UPLOADED、PROCESSING、READY、FAILED',
                             error_message TEXT DEFAULT NULL COMMENT '解析或向量化失败时的错误摘要',

                             user_id BIGINT NOT NULL COMMENT '上传用户ID',
                             org_tag_id VARCHAR(300) NOT NULL COMMENT '文件所属组织标签ID',
                             is_public BOOLEAN NOT NULL DEFAULT FALSE COMMENT '是否允许组织范围之外的用户访问',

                             created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间',
                             updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                                 ON UPDATE CURRENT_TIMESTAMP COMMENT '记录更新时间',
                             merged_at TIMESTAMP NULL DEFAULT NULL COMMENT '文件分片合并完成时间',

                             INDEX idx_file_upload_user_created (user_id, created_at),
                             INDEX idx_file_upload_org_status (org_tag_id, status),
                             INDEX idx_file_upload_md5 (file_md5),

                             CONSTRAINT fk_file_upload_user
                                 FOREIGN KEY (user_id) REFERENCES users(id),
                             CONSTRAINT fk_file_upload_org
                                 FOREIGN KEY (org_tag_id) REFERENCES organization_tags(tag_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文件上传主表';


CREATE TABLE chunk_info (
                            id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '上传分片记录ID',

                            file_id BIGINT NOT NULL COMMENT '所属文件记录ID',
                            chunk_index INT NOT NULL COMMENT '分片序号，从0开始',
                            chunk_md5 CHAR(32) NOT NULL COMMENT '分片内容MD5',
                            object_key VARCHAR(512) NOT NULL COMMENT 'MinIO中该分片的对象键',

                            created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '分片记录创建时间',

                            CONSTRAINT uk_chunk_file_index
                                UNIQUE (file_id, chunk_index),
                            CONSTRAINT fk_chunk_file
                                FOREIGN KEY (file_id) REFERENCES file_upload(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文件上传分片表';
