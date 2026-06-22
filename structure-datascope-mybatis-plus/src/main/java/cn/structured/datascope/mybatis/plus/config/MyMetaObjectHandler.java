package cn.structured.datascope.mybatis.plus.config;

import cn.structured.security.context.UserContext;
import cn.structured.security.entity.UserContextEntity;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.reflection.MetaObject;

import java.time.LocalDateTime;

/**
 * MyBatis Plus自动填充配置
 *
 * @author chuck
 * @since 2024-01-01
 */
@Slf4j
public class MyMetaObjectHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        // 检查字段是否已存在值，如果不存在则填充默认值
        if (this.getFieldValByName("delFlag", metaObject) == null) {
            this.setFieldValByName("delFlag", Boolean.FALSE, metaObject);
        }
        if (this.getFieldValByName("enabled", metaObject) == null) {
            this.setFieldValByName("enabled", Boolean.TRUE, metaObject);
        }
        if (this.getFieldValByName("deleted", metaObject) == null) {
            this.setFieldValByName("deleted", Boolean.FALSE, metaObject);
        }
        if (this.getFieldValByName("createDate", metaObject) == null) {
            this.setFieldValByName("createDate", LocalDateTime.now(), metaObject);
        }
        if (this.getFieldValByName("createTime", metaObject) == null) {
            this.setFieldValByName("createTime", LocalDateTime.now(), metaObject);
        }
        if (this.getFieldValByName("createBy", metaObject) == null) {
            this.setFieldValByName("createBy", getUserId(), metaObject);
        }
        if (this.getFieldValByName("updateDate", metaObject) == null) {
            this.setFieldValByName("updateDate", LocalDateTime.now(), metaObject);
        }
        if (this.getFieldValByName("updateTime", metaObject) == null) {
            this.setFieldValByName("updateTime", LocalDateTime.now(), metaObject);
        }
        if (this.getFieldValByName("updateBy", metaObject) == null) {
            this.setFieldValByName("updateBy", getUserId(), metaObject);
        }
        if (this.getFieldValByName("deptId", metaObject) == null) {
            this.setFieldValByName("deptId", getDeptId(), metaObject);
        }
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        // 检查字段是否已存在值，如果不存在则更新为新值
        if (this.getFieldValByName("updateDate", metaObject) == null) {
            this.setFieldValByName("updateDate", LocalDateTime.now(), metaObject);
        }
        if (this.getFieldValByName("updateTime", metaObject) == null) {
            this.setFieldValByName("updateTime", LocalDateTime.now(), metaObject);
        }
        if (this.getFieldValByName("updateBy", metaObject) == null) {
            this.setFieldValByName("updateBy", getUserId(), metaObject);
        }
    }

    /**
     * 获取用户ID
     *
     * @return 用户ID
     */
    private Object getUserId() {
        try {
            // 使用 用户上下文获取 用户ID
            UserContextEntity userContextEntity = UserContext.get();
            if (null != UserContext.get()) {
                return userContextEntity.getUserId();
            }
        } catch (Exception e) {
            log.debug("get user id is error -> message = {}", e.getMessage());
        }
        return null;
    }

    /**
     * 获取部门ID
     *
     * @return 部门ID
     */
    private Object getDeptId() {
        try {
            // 使用 用户上下文获取 部门ID
            UserContextEntity userContextEntity = UserContext.get();
            if (null != UserContext.get()) {
                return userContextEntity.getDeptId();
            }
        } catch (Exception e) {
            log.debug("get dept id is error -> message = {}", e.getMessage());
        }
        return null;
    }
}
