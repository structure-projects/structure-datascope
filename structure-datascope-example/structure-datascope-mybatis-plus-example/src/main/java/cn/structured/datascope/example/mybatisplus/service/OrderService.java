package cn.structured.datascope.example.mybatisplus.service;

import cn.structured.datascope.DataScopeContext;
import cn.structured.datascope.example.mybatisplus.dto.OrderResponse;
import cn.structured.datascope.example.mybatisplus.entity.OrderEntity;
import cn.structured.datascope.example.mybatisplus.mapper.OrderMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 订单服务
 * <p>
 * 业务代码无需感知数据权限实现，数据权限通过SQL拦截器自动注入
 * </p>
 */
@Slf4j
@Service
public class OrderService {

    private final OrderMapper orderMapper;

    public OrderService(OrderMapper orderMapper) {
        this.orderMapper = orderMapper;
    }

    /**
     * 获取订单列表
     * <p>
     * 无需显式处理数据权限，SQL拦截器自动注入权限条件
     * </p>
     */
    public List<OrderResponse> getOrderList() {
        log.info("Fetching order list...");

        // 普通的MyBatis-Plus查询，权限条件由拦截器自动注入
        List<OrderEntity> entities = orderMapper.selectList(null);

        return entities.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * 获取订单详情
     */
    public OrderResponse getOrderById(Long id) {
        log.info("Fetching order by id: {}", id);

        OrderEntity entity = orderMapper.selectById(id);
        if (entity == null) {
            return null;
        }

        return convertToResponse(entity);
    }

    /**
     * 根据订单编号查询
     */
    public OrderResponse getOrderByOrderNo(String orderNo) {
        log.info("Fetching order by orderNo: {}", orderNo);

        OrderEntity entity = orderMapper.selectByOrderNo(orderNo);
        if (entity == null) {
            return null;
        }

        return convertToResponse(entity);
    }

    /**
     * 条件查询订单
     */
    public List<OrderResponse> getOrdersByStatus(String status) {
        log.info("Fetching orders by status: {}", status);

        List<OrderEntity> entities = orderMapper.selectByCondition(status);

        return entities.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * 创建订单
     */
    public OrderResponse createOrder(OrderResponse request) {
        log.info("Creating order: {}", request.getOrderNo());

        OrderEntity entity = OrderEntity.builder()
                .orderNo(request.getOrderNo())
                .amount(request.getAmount())
                .phone(request.getPhone())
                .email(request.getEmail())
                .remark(request.getRemark())
                // 数据权限字段自动从上下文获取并填充
                .orgId(Long.parseLong(DataScopeContext.getOrgId()))
                .deptId(Long.parseLong(DataScopeContext.getDeptIds().get(0)))
                .createBy(DataScopeContext.getUserId())
                .build();

        orderMapper.insert(entity);

        return convertToResponse(entity);
    }

    /**
     * 更新订单
     */
    public OrderResponse updateOrder(Long id, OrderResponse request) {
        log.info("Updating order: {}", id);

        OrderEntity existing = orderMapper.selectById(id);
        if (existing == null) {
            return null;
        }

        existing.setAmount(request.getAmount());
        existing.setPhone(request.getPhone());
        existing.setEmail(request.getEmail());
        existing.setRemark(request.getRemark());
        existing.setUpdateBy(DataScopeContext.getUserId());

        orderMapper.updateById(existing);

        return convertToResponse(existing);
    }

    /**
     * 删除订单
     */
    public void deleteOrder(Long id) {
        log.info("Deleting order: {}", id);
        orderMapper.deleteById(id);
    }

    /**
     * 获取订单数量
     */
    public long getOrderCount() {
        return orderMapper.selectCount(null);
    }

    /**
     * 分页查询订单
     * <p>
     * 数据权限通过SQL拦截器自动注入，分页插件自动处理分页
     * </p>
     */
    public IPage<OrderResponse> getOrderPage(int pageNum, int pageSize, String status) {
        log.info("Fetching order page: pageNum={}, pageSize={}, status={}", pageNum, pageSize, status);

        Page<OrderEntity> page = new Page<>(pageNum, pageSize);
        IPage<OrderEntity> entityPage = orderMapper.selectOrderPage(page, status);

        return entityPage.convert(this::convertToResponse);
    }

    /**
     * 分页查询订单（使用MyBatis-Plus内置分页）
     * <p>
     * 使用BaseMapper的selectPage方法
     * </p>
     */
    public IPage<OrderResponse> getOrderPageByWrapper(int pageNum, int pageSize, String orderNo) {
        log.info("Fetching order page by wrapper: pageNum={}, pageSize={}, orderNo={}", pageNum, pageSize, orderNo);

        Page<OrderEntity> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<OrderEntity> wrapper = new LambdaQueryWrapper<>();
        if (orderNo != null && !orderNo.isEmpty()) {
            wrapper.like(OrderEntity::getOrderNo, orderNo);
        }
        wrapper.orderByDesc(OrderEntity::getCreateTime);

        IPage<OrderEntity> entityPage = orderMapper.selectPage(page, wrapper);

        return entityPage.convert(this::convertToResponse);
    }

    /**
     * 实体转DTO
     */
    private OrderResponse convertToResponse(OrderEntity entity) {
        OrderResponse response = new OrderResponse();
        response.setId(entity.getId());
        response.setOrderNo(entity.getOrderNo());
        response.setAmount(entity.getAmount());
        response.setPhone(entity.getPhone());
        response.setEmail(entity.getEmail());
        response.setRemark(entity.getRemark());
        response.setOrgId(entity.getOrgId());
        response.setDeptId(entity.getDeptId());
        response.setCreateTime(entity.getCreateTime());
        response.setCreateBy(entity.getCreateBy());
        return response;
    }
}
