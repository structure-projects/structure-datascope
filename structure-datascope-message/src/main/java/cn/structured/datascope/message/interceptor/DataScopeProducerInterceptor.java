package cn.structured.datascope.message.interceptor;

import cn.structured.datascope.message.DataScopeMessageUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;

/**
 * 数据权限生产者拦截器
 * <p>
 * 在消息发送前自动将当前线程的DataScopeContext数据权限信息注入到消息头中，
 * 实现数据权限从生产者到消费者的透传
 * </p>
 */
@Slf4j
public class DataScopeProducerInterceptor implements ChannelInterceptor {

    @Override
    public Message<?> preSend(Message<?> message, org.springframework.messaging.MessageChannel channel) {
        try {
            log.debug("DataScopeProducerInterceptor preSend called");
            Message<?> modifiedMessage = DataScopeMessageUtils.injectDataScopeIntoMessage(message);
            log.debug("DataScopeProducerInterceptor preSend completed");
            return modifiedMessage;
        } catch (Exception e) {
            log.error("Error in DataScopeProducerInterceptor preSend", e);
            return message;
        }
    }
}