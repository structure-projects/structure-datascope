package cn.structured.datascope.elasticsearch.aspect;

import cn.structured.datascope.DataScopeContext;
import cn.structured.datascope.config.DataScopeFieldConfig;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Elasticsearch 数据权限切面
 * <p>
 * 自动为Elasticsearch查询添加数据权限条件，对业务层完全透明
 * </p>
 */
@Slf4j
@Aspect
@RequiredArgsConstructor
public class DataScopeElasticsearchAspect {

    private final DataScopeFieldConfig fieldConfig;

    @Around("execution(* co.elastic.clients.elasticsearch.ElasticsearchClient.search(..))")
    public Object aroundSearch(ProceedingJoinPoint joinPoint) throws Throwable {
        String orgId = DataScopeContext.getOrgId();
        List<String> deptIds = DataScopeContext.getDeptIds();

        if (orgId == null && (deptIds == null || deptIds.isEmpty())) {
            log.debug("No data scope context, bypass Elasticsearch interception");
            return joinPoint.proceed();
        }

        Object[] args = joinPoint.getArgs();
        SearchRequest originalRequest = null;
        int requestIndex = -1;

        for (int i = 0; i < args.length; i++) {
            if (args[i] instanceof SearchRequest) {
                originalRequest = (SearchRequest) args[i];
                requestIndex = i;
                break;
            }
        }

        if (originalRequest == null) {
            return joinPoint.proceed();
        }

        SearchRequest modifiedRequest = buildDataScopeRequest(originalRequest);

        if (requestIndex >= 0) {
            args[requestIndex] = modifiedRequest;
        }

        log.debug("Elasticsearch search request modified with data scope");
        return joinPoint.proceed(args);
    }

    private SearchRequest buildDataScopeRequest(SearchRequest originalRequest) {
        String orgId = DataScopeContext.getOrgId();
        List<String> deptIds = DataScopeContext.getDeptIds();

        List<Query> filters = new ArrayList<>();

        if (orgId != null && !orgId.isEmpty()) {
            filters.add(Query.of(q -> q.term(t -> t
                    .field(fieldConfig.getOrgIdField())
                    .value(orgId))));
        }

        if (deptIds != null && !deptIds.isEmpty()) {
            List<FieldValue> fieldValues = deptIds.stream()
                    .map(FieldValue::of)
                    .collect(Collectors.toList());
            filters.add(Query.of(q -> q.terms(t -> t
                    .field(fieldConfig.getDeptIdField())
                    .terms(te -> te.value(fieldValues)))));
        }

        Query query;
        if (filters.isEmpty()) {
            query = Query.of(q -> q.matchAll(m -> m));
        } else {
            query = Query.of(q -> q.bool(b -> b.filter(filters)));
        }

        return SearchRequest.of(s -> s
                .index(originalRequest.index())
                .query(query)
                .from(originalRequest.from())
                .size(originalRequest.size())
                .sort(originalRequest.sort())
        );
    }
}