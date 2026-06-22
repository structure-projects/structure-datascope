package cn.structured.datascope.message;

import cn.structured.datascope.DataScopeContext;
import cn.structured.datascope.DataScopeInfo;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.MessageBuilder;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * 数据权限消息工具类
 * <p>
 * 提供消息头中数据权限信息的序列化、反序列化和提取操作
 * </p>
 */
@Slf4j
public final class DataScopeMessageUtils {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private DataScopeMessageUtils() {
    }

    /**
     * 将数据权限上下文信息序列化为JSON字符串
     *
     * @param info 数据权限上下文信息
     * @return JSON字符串
     */
    public static String serializeDataScopeInfo(DataScopeInfo info) {
        if (info == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(info);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize DataScopeInfo", e);
            return null;
        }
    }

    /**
     * 将JSON字符串反序列化为数据权限上下文信息
     *
     * @param json JSON字符串
     * @return 数据权限上下文信息
     */
    public static DataScopeInfo deserializeDataScopeInfo(String json) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, DataScopeInfo.class);
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize DataScopeInfo from json: {}", json, e);
            return null;
        }
    }

    /**
     * 从当前线程上下文提取数据权限信息并设置到消息头中
     *
     * @param message 原始消息
     * @param <T>     消息体类型
     * @return 添加了数据权限消息头的新消息
     */
    public static <T> Message<T> injectDataScopeIntoMessage(Message<T> message) {
        DataScopeInfo info = DataScopeContext.get();
        if (info == null) {
            log.debug("No DataScopeInfo found in current context, skipping injection");
            return message;
        }

        String serializedInfo = serializeDataScopeInfo(info);
        if (serializedInfo == null) {
            log.debug("Failed to serialize DataScopeInfo, skipping injection");
            return message;
        }

        MessageBuilder<T> builder = MessageBuilder.fromMessage(message);
        builder.setHeader(DataScopeMessageHeaders.DATA_SCOPE_INFO, serializedInfo);

        if (info.getUserId() != null) {
            builder.setHeader(DataScopeMessageHeaders.USER_ID, info.getUserId());
        }
        if (info.getOrgId() != null) {
            builder.setHeader(DataScopeMessageHeaders.ORG_ID, info.getOrgId());
        }
        if (info.getDeptIds() != null && !info.getDeptIds().isEmpty()) {
            builder.setHeader(DataScopeMessageHeaders.DEPT_IDS, String.join(",", info.getDeptIds()));
        }
        if (info.getRoles() != null && !info.getRoles().isEmpty()) {
            builder.setHeader(DataScopeMessageHeaders.ROLES, String.join(",", info.getRoles()));
        }
        if (info.getPermissions() != null && !info.getPermissions().isEmpty()) {
            builder.setHeader(DataScopeMessageHeaders.PERMISSIONS, String.join(",", info.getPermissions()));
        }

        log.debug("Injected DataScopeInfo into message headers: userId={}, orgId={}", info.getUserId(), info.getOrgId());
        return builder.build();
    }

    /**
     * 从消息头中提取数据权限信息并设置到当前线程上下文
     *
     * @param message 消息
     */
    public static void extractDataScopeFromMessage(Message<?> message) {
        if (message == null || message.getHeaders() == null) {
            log.debug("Message or headers is null, skipping extraction");
            return;
        }

        MessageHeaders headers = message.getHeaders();
        String serializedInfo = getHeaderAsString(headers, DataScopeMessageHeaders.DATA_SCOPE_INFO);

        if (serializedInfo != null) {
            DataScopeInfo info = deserializeDataScopeInfo(serializedInfo);
            if (info != null) {
                DataScopeContext.set(info);
                log.debug("Extracted DataScopeInfo from message headers: userId={}, orgId={}",
                        info.getUserId(), info.getOrgId());
                return;
            }
        }

        DataScopeInfo info = buildDataScopeInfoFromHeaders(headers);
        if (info != null && isDataScopeInfoValid(info)) {
            DataScopeContext.set(info);
            log.debug("Built DataScopeInfo from individual headers: userId={}, orgId={}",
                    info.getUserId(), info.getOrgId());
        }
    }

    /**
     * 从消息头中提取数据权限信息（不设置到上下文）
     *
     * @param message 消息
     * @return 数据权限上下文信息
     */
    public static DataScopeInfo extractDataScopeInfo(Message<?> message) {
        if (message == null || message.getHeaders() == null) {
            return null;
        }

        MessageHeaders headers = message.getHeaders();
        String serializedInfo = getHeaderAsString(headers, DataScopeMessageHeaders.DATA_SCOPE_INFO);

        if (serializedInfo != null) {
            return deserializeDataScopeInfo(serializedInfo);
        }

        return buildDataScopeInfoFromHeaders(headers);
    }

    /**
     * 从消息头中逐个提取字段并构建DataScopeInfo
     */
    private static DataScopeInfo buildDataScopeInfoFromHeaders(MessageHeaders headers) {
        DataScopeInfo info = new DataScopeInfo();

        String userId = getHeaderAsString(headers, DataScopeMessageHeaders.USER_ID);
        String orgId = getHeaderAsString(headers, DataScopeMessageHeaders.ORG_ID);
        String deptIdsStr = getHeaderAsString(headers, DataScopeMessageHeaders.DEPT_IDS);
        String rolesStr = getHeaderAsString(headers, DataScopeMessageHeaders.ROLES);
        String permissionsStr = getHeaderAsString(headers, DataScopeMessageHeaders.PERMISSIONS);

        info.setUserId(userId);
        info.setOrgId(orgId);

        if (deptIdsStr != null && !deptIdsStr.isEmpty()) {
            info.setDeptIds(Arrays.asList(deptIdsStr.split(",")));
        }
        if (rolesStr != null && !rolesStr.isEmpty()) {
            info.setRoles(Arrays.asList(rolesStr.split(",")));
        }
        if (permissionsStr != null && !permissionsStr.isEmpty()) {
            info.setPermissions(Arrays.asList(permissionsStr.split(",")));
        }

        return info;
    }

    /**
     * 检查DataScopeInfo是否包含有效数据
     */
    private static boolean isDataScopeInfoValid(DataScopeInfo info) {
        return info != null && (
                info.getUserId() != null ||
                        info.getOrgId() != null ||
                        (info.getDeptIds() != null && !info.getDeptIds().isEmpty()) ||
                        (info.getRoles() != null && !info.getRoles().isEmpty()) ||
                        (info.getPermissions() != null && !info.getPermissions().isEmpty())
        );
    }

    /**
     * 从消息头中获取字符串值
     */
    private static String getHeaderAsString(MessageHeaders headers, String key) {
        Object value = headers.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof String) {
            return (String) value;
        }
        if (value instanceof byte[]) {
            return new String((byte[]) value, StandardCharsets.UTF_8);
        }
        return value.toString();
    }

    /**
     * 清理当前线程的数据权限上下文
     */
    public static void clearDataScopeContext() {
        DataScopeContext.remove();
        log.debug("DataScope context cleared");
    }
}