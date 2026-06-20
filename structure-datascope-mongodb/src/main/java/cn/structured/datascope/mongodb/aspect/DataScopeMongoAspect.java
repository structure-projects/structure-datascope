package cn.structured.datascope.mongodb.aspect;

import cn.structured.datascope.DataScopeContext;
import cn.structured.datascope.config.DataScopeFieldConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.lang.reflect.Method;
import java.util.List;

/**
 * MongoDB 数据权限切面
 * <p>
 * 自动为MongoDB查询添加数据权限条件，对业务层完全透明
 * </p>
 *
 * @author chuck
 */
@Slf4j
@Aspect
@RequiredArgsConstructor
public class DataScopeMongoAspect {

    private final DataScopeFieldConfig fieldConfig;

    /**
     * 拦截MongoTemplate的查询方法
     */
    @Around("execution(* org.springframework.data.mongodb.core.MongoTemplate.find*(..))")
    public Object aroundFind(ProceedingJoinPoint joinPoint) throws Throwable {
        // 获取数据权限上下文
        String orgId = DataScopeContext.getOrgId();
        List<String> deptIds = DataScopeContext.getDeptIds();

        // 如果没有数据权限信息，直接放行
        if (orgId == null && (deptIds == null || deptIds.isEmpty())) {
            log.debug("No data scope context, bypass MongoDB interception");
            return joinPoint.proceed();
        }

        // 获取Query参数
        Object[] args = joinPoint.getArgs();
        Query originalQuery = null;
        int queryIndex = -1;

        for (int i = 0; i < args.length; i++) {
            if (args[i] instanceof Query) {
                originalQuery = (Query) args[i];
                queryIndex = i;
                break;
            }
        }

        if (originalQuery == null) {
            return joinPoint.proceed();
        }

        // 构建数据权限条件
        Query modifiedQuery = buildDataScopeQuery(originalQuery);

        // 修改参数
        if (queryIndex >= 0) {
            args[queryIndex] = modifiedQuery;
        }

        log.debug("MongoDB query modified with data scope");
        return joinPoint.proceed(args);
    }

    /**
     * 拦截MongoTemplate的count方法
     */
    @Around("execution(* org.springframework.data.mongodb.core.MongoTemplate.count(..))")
    public Object aroundCount(ProceedingJoinPoint joinPoint) throws Throwable {
        String orgId = DataScopeContext.getOrgId();
        List<String> deptIds = DataScopeContext.getDeptIds();

        if (orgId == null && (deptIds == null || deptIds.isEmpty())) {
            return joinPoint.proceed();
        }

        Object[] args = joinPoint.getArgs();
        Query originalQuery = null;
        int queryIndex = -1;

        for (int i = 0; i < args.length; i++) {
            if (args[i] instanceof Query) {
                originalQuery = (Query) args[i];
                queryIndex = i;
                break;
            }
        }

        if (originalQuery != null && queryIndex >= 0) {
            Query modifiedQuery = buildDataScopeQuery(originalQuery);
            args[queryIndex] = modifiedQuery;
            log.debug("MongoDB count query modified with data scope");
        }

        return joinPoint.proceed(args);
    }

    /**
     * 构建带数据权限条件的Query
     */
    private Query buildDataScopeQuery(Query query) {
        String orgId = DataScopeContext.getOrgId();
        List<String> deptIds = DataScopeContext.getDeptIds();

        if (orgId != null && !orgId.isEmpty()) {
            query.addCriteria(Criteria.where(fieldConfig.getOrgIdField()).is(orgId));
        }

        if (deptIds != null && !deptIds.isEmpty()) {
            query.addCriteria(Criteria.where(fieldConfig.getDeptIdField()).in(deptIds));
        }

        return query;
    }
}
