package cn.structured.datascope.example.mybatisplus.mapper;

import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 订单和用户联合查询Mapper
 * <p>
 * 用于测试多表查询的数据权限拦截
 * </p>
 */
public interface OrderUserMapper {

    /**
     * 多表JOIN查询：订单 + 用户
     */
    List<Map<String, Object>> selectOrderWithUser();

    /**
     * 多表JOIN查询：订单 + 用户（带别名）
     */
    List<Map<String, Object>> selectOrderWithUserAlias();

    /**
     * 逗号分隔多表查询
     */
    List<Map<String, Object>> selectOrderAndUser();

    /**
     * LEFT JOIN 查询
     */
    List<Map<String, Object>> selectOrderLeftJoinUser();

    /**
     * 复杂多表查询：订单 + 用户 + 部门
     */
    List<Map<String, Object>> selectOrderUserDept();
}