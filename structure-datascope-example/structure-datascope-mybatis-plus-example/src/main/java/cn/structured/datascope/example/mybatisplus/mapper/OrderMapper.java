package cn.structured.datascope.example.mybatisplus.mapper;

import cn.structured.datascope.example.mybatisplus.entity.OrderEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 订单Mapper接口
 * <p>
 * 业务层无需感知数据权限实现，数据权限通过SQL拦截器自动注入
 * </p>
 */
public interface OrderMapper extends BaseMapper<OrderEntity> {

    /**
     * 根据订单编号查询
     */
    OrderEntity selectByOrderNo(@Param("orderNo") String orderNo);

    /**
     * 条件查询订单
     */
    List<OrderEntity> selectByCondition(@Param("status") String status);
}