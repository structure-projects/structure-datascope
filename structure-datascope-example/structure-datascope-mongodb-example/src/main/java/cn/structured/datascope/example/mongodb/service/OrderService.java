package cn.structured.datascope.example.mongodb.service;

import cn.structured.datascope.DataScopeContext;
import cn.structured.datascope.example.mongodb.document.OrderDocument;
import cn.structured.datascope.example.mongodb.dto.OrderResponse;
import cn.structured.datascope.example.mongodb.repository.OrderRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 订单服务
 * <p>
 * 业务代码无需感知数据权限实现，数据权限通过AOP切面自动注入
 * </p>
 */
@Slf4j
@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final MongoTemplate mongoTemplate;

    public OrderService(OrderRepository orderRepository,
                       MongoTemplate mongoTemplate) {
        this.orderRepository = orderRepository;
        this.mongoTemplate = mongoTemplate;
    }

    /**
     * 获取订单列表
     * <p>
     * 无需显式处理数据权限，AOP切面自动注入权限条件
     * </p>
     */
    public List<OrderResponse> getOrderList() {
        log.info("Fetching order list from MongoDB...");

        // 普通的MongoDB查询，权限条件由AOP切面自动注入
        List<OrderDocument> documents = mongoTemplate.find(new Query(), OrderDocument.class);

        return documents.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * 获取订单详情
     */
    public OrderResponse getOrderById(String id) {
        log.info("Fetching order by id: {}", id);

        OrderDocument document = mongoTemplate.findById(id, OrderDocument.class);
        if (document == null) {
            return null;
        }

        return convertToResponse(document);
    }

    /**
     * 创建订单
     */
    public OrderResponse createOrder(OrderResponse request) {
        log.info("Creating order: {}", request.getOrderNo());

        OrderDocument document = OrderDocument.builder()
                .orderNo(request.getOrderNo())
                .amount(request.getAmount())
                .phone(request.getPhone())
                .email(request.getEmail())
                .remark(request.getRemark())
                .orgId(DataScopeContext.getOrgId())
                .deptId(DataScopeContext.getDeptIds().isEmpty() ? null : DataScopeContext.getDeptIds().get(0))
                .createTime(LocalDateTime.now())
                .createBy(DataScopeContext.getUserId())
                .build();

        OrderDocument saved = mongoTemplate.save(document);
        return convertToResponse(saved);
    }

    /**
     * 更新订单
     */
    public OrderResponse updateOrder(String id, OrderResponse request) {
        log.info("Updating order: {}", id);

        OrderDocument document = mongoTemplate.findById(id, OrderDocument.class);
        if (document == null) {
            return null;
        }

        document.setAmount(request.getAmount());
        document.setPhone(request.getPhone());
        document.setEmail(request.getEmail());
        document.setRemark(request.getRemark());
        document.setUpdateTime(LocalDateTime.now());
        document.setUpdateBy(DataScopeContext.getUserId());

        mongoTemplate.save(document);

        return convertToResponse(document);
    }

    /**
     * 删除订单
     */
    public void deleteOrder(String id) {
        log.info("Deleting order: {}", id);
        mongoTemplate.remove(mongoTemplate.findById(id, OrderDocument.class));
    }

    /**
     * 获取订单数量
     */
    public long getOrderCount() {
        return mongoTemplate.count(new Query(), OrderDocument.class);
    }

    /**
     * 文档转DTO
     */
    private OrderResponse convertToResponse(OrderDocument document) {
        OrderResponse response = new OrderResponse();
        response.setId(document.getId() != null ? Long.parseLong(document.getId()) : null);
        response.setOrderNo(document.getOrderNo());
        response.setAmount(document.getAmount());
        response.setPhone(document.getPhone());
        response.setEmail(document.getEmail());
        response.setRemark(document.getRemark());
        response.setOrgId(document.getOrgId() != null ? Long.parseLong(document.getOrgId()) : null);
        response.setDeptId(document.getDeptId() != null ? Long.parseLong(document.getDeptId()) : null);
        response.setCreateTime(document.getCreateTime());
        response.setCreateBy(document.getCreateBy());
        return response;
    }
}
