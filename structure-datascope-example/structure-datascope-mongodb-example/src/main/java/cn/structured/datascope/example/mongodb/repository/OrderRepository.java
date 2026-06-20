package cn.structured.datascope.example.mongodb.repository;

import cn.structured.datascope.example.mongodb.document.OrderDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 订单MongoDB仓库接口
 */
@Repository
public interface OrderRepository extends MongoRepository<OrderDocument, String> {

    /**
     * 根据组织ID查询订单
     */
    List<OrderDocument> findByOrgId(String orgId);

    /**
     * 根据部门ID查询订单
     */
    List<OrderDocument> findByDeptId(String deptId);

    /**
     * 根据组织ID和部门ID查询订单
     */
    List<OrderDocument> findByOrgIdAndDeptId(String orgId, String deptId);

    /**
     * 根据部门ID列表查询订单
     */
    @Query("{ 'deptId': { $in: ?0 } }")
    List<OrderDocument> findByDeptIdIn(List<String> deptIds);

    /**
     * 根据组织ID和部门ID列表查询订单
     */
    @Query("{ 'orgId': ?0, 'deptId': { $in: ?1 } }")
    List<OrderDocument> findByOrgIdAndDeptIdIn(String orgId, List<String> deptIds);

    /**
     * 根据订单编号查询
     */
    OrderDocument findByOrderNo(String orderNo);

    /**
     * 根据组织ID统计订单数量
     */
    long countByOrgId(String orgId);
}