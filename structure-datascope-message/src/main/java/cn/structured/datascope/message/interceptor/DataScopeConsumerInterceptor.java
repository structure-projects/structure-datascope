package cn.structured.datascope.message.interceptor;

import cn.structured.datascope.message.DataScopeMessageUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.ChannelInterceptor;

/**
 * 数据权限消费者拦截器
 * <p>
 * 在消息消费前从消息头中提取数据权限信息并设置到当前线程的DataScopeContext，
 * 实现数据权限从生产者到消费者的透传
 * </p>
 */
@Slf4j
public class DataScopeConsumerInterceptor implements ChannelInterceptor {

    @Override
    public Message<?> preSend(Message<?> message, org.springframework.messaging.MessageChannel channel) {
        try {
            log.debug("DataScopeConsumerInterceptor preSend called");
            DataScopeMessageUtils.extractDataScopeFromMessage(message);
            log.debug("DataScopeConsumerInterceptor preSend completed");
            return message;
        } catch (Exception e) {
            log.error("Error in DataScopeConsumerInterceptor preSend", e);
            return message;
        }
    }

    @Override
    public void afterSendCompletion(Message<?> message, org.springframework.messaging.MessageChannel channel, boolean sent, Exception ex) {
        try {
            log.debug("DataScopeConsumerInterceptor afterSendCompletion called");
            DataScopeMessageUtils.clearDataScopeContext();
            log.debug("DataScopeConsumerInterceptor afterSendCompletion completed");
        } catch (Exception e) {
            log.error("Error in DataScopeConsumerInterceptor afterSendCompletion", e);
        }
    }
}