package cn.structured.datascope.elasticsearch.aspect;

import cn.structured.datascope.DataScopeContext;
import cn.structured.datascope.config.DataScopeFieldConfig;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder;

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

    @Around("execution(* org.springframework.data.elasticsearch.core.ElasticsearchOperations.search(..)) || execution(* org.springframework.data.elasticsearch.client.elc.ElasticsearchTemplate.search(..))")
    public Object aroundElasticsearchTemplateSearch(ProceedingJoinPoint joinPoint) throws Throwable {
        String orgId = DataScopeContext.getOrgId();
        List<String> deptIds = DataScopeContext.getDeptIds();

        if (orgId == null && (deptIds == null || deptIds.isEmpty())) {
            log.debug("No data scope context, bypass Elasticsearch interception");
            return joinPoint.proceed();
        }

        Object[] args = joinPoint.getArgs();
        org.springframework.data.elasticsearch.core.query.Query query = null;
        int queryIndex = -1;

        for (int i = 0; i < args.length; i++) {
            if (args[i] instanceof org.springframework.data.elasticsearch.core.query.Query) {
                query = (org.springframework.data.elasticsearch.core.query.Query) args[i];
                queryIndex = i;
                break;
            }
        }

        if (query == null) {
            return joinPoint.proceed();
        }

        org.springframework.data.elasticsearch.core.query.Query modifiedQuery = buildDataScopeQuery(query);

        if (queryIndex >= 0) {
            args[queryIndex] = modifiedQuery;
        }

        log.debug("Elasticsearch search query modified with data scope");
        return joinPoint.proceed(args);
    }

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

    private org.springframework.data.elasticsearch.core.query.Query buildDataScopeQuery(org.springframework.data.elasticsearch.core.query.Query originalQuery) {
        String orgId = DataScopeContext.getOrgId();
        List<String> deptIds = DataScopeContext.getDeptIds();

        List<co.elastic.clients.elasticsearch._types.query_dsl.Query> filters = new ArrayList<>();

        if (orgId != null && !orgId.isEmpty()) {
            filters.add(co.elastic.clients.elasticsearch._types.query_dsl.Query.of(q -> q.term(t -> t
                    .field(fieldConfig.getOrgIdField())
                    .value(orgId))));
        }

        if (deptIds != null && !deptIds.isEmpty()) {
            List<FieldValue> fieldValues = deptIds.stream()
                    .map(FieldValue::of)
                    .collect(Collectors.toList());
            filters.add(co.elastic.clients.elasticsearch._types.query_dsl.Query.of(q -> q.terms(t -> t
                    .field(fieldConfig.getDeptIdField())
                    .terms(te -> te.value(fieldValues)))));
        }

        if (filters.isEmpty()) {
            return originalQuery;
        }

        if (originalQuery instanceof NativeQuery nativeQuery) {
            co.elastic.clients.elasticsearch._types.query_dsl.Query originalEsQuery = nativeQuery.getQuery();
            co.elastic.clients.elasticsearch._types.query_dsl.Query combinedQuery;

            if (originalEsQuery == null) {
                combinedQuery = co.elastic.clients.elasticsearch._types.query_dsl.Query.of(q -> q.bool(b -> b.filter(filters)));
            } else {
                combinedQuery = co.elastic.clients.elasticsearch._types.query_dsl.Query.of(q -> q.bool(b -> b
                        .must(originalEsQuery)
                        .filter(filters)));
            }

            NativeQueryBuilder builder = NativeQuery.builder()
                    .withQuery(combinedQuery);

            if (nativeQuery.getPageable() != null) {
                builder.withPageable(nativeQuery.getPageable());
            }
            if (nativeQuery.getSort() != null) {
                builder.withSort(nativeQuery.getSort());
            }
            if (nativeQuery.getFields() != null) {
                builder.withFields(nativeQuery.getFields());
            }
            if (nativeQuery.getSourceFilter() != null) {
                builder.withSourceFilter(nativeQuery.getSourceFilter());
            }
            if (nativeQuery.getHighlightQuery() != null) {
                builder.withHighlightQuery(nativeQuery.getHighlightQuery().orElse(null));
            }

            return builder.build();
        }

        return originalQuery;
    }

    private SearchRequest buildDataScopeRequest(SearchRequest originalRequest) {
        String orgId = DataScopeContext.getOrgId();
        List<String> deptIds = DataScopeContext.getDeptIds();

        List<co.elastic.clients.elasticsearch._types.query_dsl.Query> filters = new ArrayList<>();

        if (orgId != null && !orgId.isEmpty()) {
            filters.add(co.elastic.clients.elasticsearch._types.query_dsl.Query.of(q -> q.term(t -> t
                    .field(fieldConfig.getOrgIdField())
                    .value(orgId))));
        }

        if (deptIds != null && !deptIds.isEmpty()) {
            List<FieldValue> fieldValues = deptIds.stream()
                    .map(FieldValue::of)
                    .collect(Collectors.toList());
            filters.add(co.elastic.clients.elasticsearch._types.query_dsl.Query.of(q -> q.terms(t -> t
                    .field(fieldConfig.getDeptIdField())
                    .terms(te -> te.value(fieldValues)))));
        }

        co.elastic.clients.elasticsearch._types.query_dsl.Query query;
        if (filters.isEmpty()) {
            query = co.elastic.clients.elasticsearch._types.query_dsl.Query.of(q -> q.matchAll(m -> m));
        } else {
            query = co.elastic.clients.elasticsearch._types.query_dsl.Query.of(q -> q.bool(b -> b.filter(filters)));
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