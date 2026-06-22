package cn.structured.datascope.message.wrapper;

import cn.structured.datascope.message.DataScopeMessageUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.util.MimeType;

/**
 * StreamBridge 的包装器，用于在发送消息前注入数据权限信息
 * <p>
 * 由于 StreamBridge 是 final 类，无法使用 CGLIB 代理，因此采用包装器模式
 * </p>
 */
@Slf4j
public class DataScopeStreamBridge {

    private final StreamBridge delegate;

    public DataScopeStreamBridge(StreamBridge delegate) {
        this.delegate = delegate;
    }

    public boolean send(String bindingName, Object data) {
        if (data instanceof Message) {
            Message<?> message = (Message<?>) data;
            Message<?> messageWithDataScope = DataScopeMessageUtils.injectDataScopeIntoMessage(message);
            if (messageWithDataScope != message) {
                log.debug("DataScopeStreamBridge: Injected data scope into message for binding {}", bindingName);
                return delegate.send(bindingName, messageWithDataScope);
            }
        }
        return delegate.send(bindingName, data);
    }

    public boolean send(String bindingName, String binderType, Object data) {
        if (data instanceof Message) {
            Message<?> message = (Message<?>) data;
            Message<?> messageWithDataScope = DataScopeMessageUtils.injectDataScopeIntoMessage(message);
            if (messageWithDataScope != message) {
                log.debug("DataScopeStreamBridge: Injected data scope into message for binding {} with binder {}", bindingName, binderType);
                return delegate.send(bindingName, binderType, messageWithDataScope);
            }
        }
        return delegate.send(bindingName, binderType, data);
    }

    public boolean send(String bindingName, Object data, MimeType outputContentType) {
        if (data instanceof Message) {
            Message<?> message = (Message<?>) data;
            Message<?> messageWithDataScope = DataScopeMessageUtils.injectDataScopeIntoMessage(message);
            if (messageWithDataScope != message) {
                log.debug("DataScopeStreamBridge: Injected data scope into message for binding {} with contentType {}", bindingName, outputContentType);
                return delegate.send(bindingName, messageWithDataScope, outputContentType);
            }
        }
        return delegate.send(bindingName, data, outputContentType);
    }

    public boolean send(String bindingName, String binderName, Object data, MimeType outputContentType) {
        if (data instanceof Message) {
            Message<?> message = (Message<?>) data;
            Message<?> messageWithDataScope = DataScopeMessageUtils.injectDataScopeIntoMessage(message);
            if (messageWithDataScope != message) {
                log.debug("DataScopeStreamBridge: Injected data scope into message for binding {} with binder {} and contentType {}", bindingName, binderName, outputContentType);
                return delegate.send(bindingName, binderName, messageWithDataScope, outputContentType);
            }
        }
        return delegate.send(bindingName, binderName, data, outputContentType);
    }
}