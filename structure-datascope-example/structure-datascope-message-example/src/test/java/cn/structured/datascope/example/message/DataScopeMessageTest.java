package cn.structured.datascope.example.message;

import cn.structured.datascope.DataScopeContext;
import cn.structured.datascope.DataScopeInfo;
import cn.structured.datascope.example.message.dto.OrderEvent;
import cn.structured.datascope.example.message.config.TestBinderConfiguration;
import cn.structured.datascope.message.DataScopeMessageUtils;
import cn.structured.datascope.provider.DataScopeProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 数据权限消息透传测试
 * <p>
 * 测试基于Spring Cloud Stream的数据权限信息从生产者到消费者的透传功能
 * </p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestBinderConfiguration.class)
class DataScopeMessageTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DataScopeProvider dataScopeProvider;

    @BeforeEach
    void setUp() {
        DataScopeContext.remove();
    }

    @AfterEach
    void tearDown() {
        DataScopeContext.remove();
    }

    /**
     * 手动设置用户上下文
     */
    private void setupUserContext(String userId) {
        DataScopeInfo scopeInfo = dataScopeProvider.getScopeInfo(userId);
        if (scopeInfo != null) {
            DataScopeContext.set(scopeInfo);
        }
    }

    @Nested
    @DisplayName("数据权限上下文设置测试")
    class DataScopeContextTest {

        @Test
        @DisplayName("设置组织10管理员上下文")
        void testOrg10AdminContext() {
            setupUserContext("user-row-org10-admin");

            assertEquals("user-row-org10-admin", DataScopeContext.getUserId());
            assertEquals("10", DataScopeContext.getOrgId());
            assertTrue(DataScopeContext.getDeptIds().contains("1"));
            assertTrue(DataScopeContext.hasRole("ORG_ADMIN"));
        }

        @Test
        @DisplayName("设置组织20管理员上下文")
        void testOrg20AdminContext() {
            setupUserContext("user-row-org20-admin");

            assertEquals("user-row-org20-admin", DataScopeContext.getUserId());
            assertEquals("20", DataScopeContext.getOrgId());
            assertTrue(DataScopeContext.getDeptIds().contains("6"));
            assertTrue(DataScopeContext.hasRole("ORG_ADMIN"));
        }

        @Test
        @DisplayName("设置部门1用户上下文")
        void testDept1UserContext() {
            setupUserContext("user-row-dept1");

            assertEquals("user-row-dept1", DataScopeContext.getUserId());
            assertEquals("10", DataScopeContext.getOrgId());
            assertEquals(1, DataScopeContext.getDeptIds().size());
            assertTrue(DataScopeContext.getDeptIds().contains("1"));
        }

        @Test
        @DisplayName("设置多角色用户上下文")
        void testMultiRoleUserContext() {
            setupUserContext("user-004");

            assertEquals("user-004", DataScopeContext.getUserId());
            assertTrue(DataScopeContext.hasRole("EMPLOYEE"));
            assertTrue(DataScopeContext.hasRole("FINANCE"));
            assertTrue(DataScopeContext.hasAnyRole("EMPLOYEE", "FINANCE"));
        }
    }

    @Nested
    @DisplayName("消息头注入测试")
    class MessageHeaderInjectionTest {

        @Test
        @DisplayName("有上下文时消息头应包含数据权限信息")
        void testInjectDataScopeIntoMessageWithContext() {
            setupUserContext("user-row-org10-admin");

            OrderEvent event = createTestOrderEvent();
            Message<OrderEvent> originalMessage = MessageBuilder.withPayload(event).build();

            Message<OrderEvent> injectedMessage = DataScopeMessageUtils.injectDataScopeIntoMessage(originalMessage);
            MessageHeaders headers = injectedMessage.getHeaders();

            assertNotNull(headers.get("X-DataScope-User-Id"));
            assertNotNull(headers.get("X-DataScope-Org-Id"));
            assertNotNull(headers.get("X-DataScope-Dept-Ids"));
            assertNotNull(headers.get("X-DataScope-Roles"));

            assertEquals("user-row-org10-admin", headers.get("X-DataScope-User-Id"));
            assertEquals("10", headers.get("X-DataScope-Org-Id"));
            assertTrue(headers.get("X-DataScope-Dept-Ids").toString().contains("1"));
            assertTrue(headers.get("X-DataScope-Roles").toString().contains("ORG_ADMIN"));
        }

        @Test
        @DisplayName("无上下文时消息头不应包含数据权限信息")
        void testInjectDataScopeIntoMessageWithoutContext() {
            OrderEvent event = createTestOrderEvent();
            Message<OrderEvent> originalMessage = MessageBuilder.withPayload(event).build();

            Message<OrderEvent> injectedMessage = DataScopeMessageUtils.injectDataScopeIntoMessage(originalMessage);
            MessageHeaders headers = injectedMessage.getHeaders();

            assertNull(headers.get("X-DataScope-User-Id"));
            assertNull(headers.get("X-DataScope-Org-Id"));
            assertNull(headers.get("X-DataScope-Dept-Ids"));
            assertNull(headers.get("X-DataScope-Roles"));
        }

        @Test
        @DisplayName("多角色用户消息头应包含所有角色")
        void testInjectDataScopeWithMultipleRoles() {
            setupUserContext("user-004");

            OrderEvent event = createTestOrderEvent();
            Message<OrderEvent> originalMessage = MessageBuilder.withPayload(event).build();

            Message<OrderEvent> injectedMessage = DataScopeMessageUtils.injectDataScopeIntoMessage(originalMessage);
            MessageHeaders headers = injectedMessage.getHeaders();

            String roles = headers.get("X-DataScope-Roles").toString();
            assertTrue(roles.contains("EMPLOYEE"));
            assertTrue(roles.contains("FINANCE"));
        }
    }

    @Nested
    @DisplayName("消息头提取测试")
    class MessageHeaderExtractionTest {

        @Test
        @DisplayName("从消息头提取数据权限信息并设置到上下文")
        void testExtractDataScopeFromMessage() {
            DataScopeContext.remove();

            Message<OrderEvent> message = createMessageWithDataScopeHeaders(
                    "test-user", "10", Arrays.asList("1", "2"), Arrays.asList("ADMIN", "USER")
            );

            DataScopeMessageUtils.extractDataScopeFromMessage(message);

            assertEquals("test-user", DataScopeContext.getUserId());
            assertEquals("10", DataScopeContext.getOrgId());
            assertTrue(DataScopeContext.getDeptIds().contains("1"));
            assertTrue(DataScopeContext.getDeptIds().contains("2"));
            assertTrue(DataScopeContext.hasRole("ADMIN"));
            assertTrue(DataScopeContext.hasRole("USER"));
        }

        @Test
        @DisplayName("从空消息头提取不应设置上下文")
        void testExtractDataScopeFromEmptyMessage() {
            DataScopeContext.remove();

            OrderEvent event = createTestOrderEvent();
            Message<OrderEvent> message = MessageBuilder.withPayload(event).build();

            DataScopeMessageUtils.extractDataScopeFromMessage(message);

            assertNull(DataScopeContext.getUserId());
            assertNull(DataScopeContext.getOrgId());
        }

        @Test
        @DisplayName("提取后上下文状态正确")
        void testContextStateAfterExtraction() {
            DataScopeContext.remove();

            Message<OrderEvent> message = createMessageWithDataScopeHeaders(
                    "user-001", "20", Arrays.asList("5"), Arrays.asList("EMPLOYEE")
            );

            DataScopeMessageUtils.extractDataScopeFromMessage(message);

            assertTrue(DataScopeContext.hasRole("EMPLOYEE"));
            assertFalse(DataScopeContext.hasRole("ADMIN"));
            assertTrue(DataScopeContext.hasAnyRole("EMPLOYEE", "ADMIN"));
            assertFalse(DataScopeContext.hasAnyRole("ADMIN", "MANAGER"));
        }
    }

    @Nested
    @DisplayName("端到端消息透传测试")
    class EndToEndMessageTransmissionTest {

        @Test
        @DisplayName("完整流程：生产者设置上下文 -> 注入消息头 -> 消费者提取上下文")
        void testEndToEndMessageTransmission() {
            DataScopeContext.remove();

            setupUserContext("user-row-org10-admin");
            String originalUserId = DataScopeContext.getUserId();
            String originalOrgId = DataScopeContext.getOrgId();

            OrderEvent event = createTestOrderEvent();
            Message<OrderEvent> originalMessage = MessageBuilder.withPayload(event).build();

            Message<OrderEvent> injectedMessage = DataScopeMessageUtils.injectDataScopeIntoMessage(originalMessage);

            DataScopeContext.remove();
            assertNull(DataScopeContext.getUserId());

            DataScopeMessageUtils.extractDataScopeFromMessage(injectedMessage);

            assertEquals(originalUserId, DataScopeContext.getUserId());
            assertEquals(originalOrgId, DataScopeContext.getOrgId());
            assertTrue(DataScopeContext.getDeptIds().contains("1"));
            assertTrue(DataScopeContext.hasRole("ORG_ADMIN"));
        }

        @Test
        @DisplayName("上下文信息在消息传递前后保持一致")
        void testContextConsistencyAcrossTransmission() {
            DataScopeContext.remove();

            setupUserContext("user-004");

            DataScopeInfo originalInfo = DataScopeContext.get();
            assertNotNull(originalInfo);

            OrderEvent event = createTestOrderEvent();
            Message<OrderEvent> message = MessageBuilder.withPayload(event).build();
            Message<OrderEvent> injectedMessage = DataScopeMessageUtils.injectDataScopeIntoMessage(message);

            DataScopeContext.remove();

            DataScopeMessageUtils.extractDataScopeFromMessage(injectedMessage);
            DataScopeInfo extractedInfo = DataScopeContext.get();

            assertEquals(originalInfo.getUserId(), extractedInfo.getUserId());
            assertEquals(originalInfo.getOrgId(), extractedInfo.getOrgId());
            assertEquals(originalInfo.getDeptIds(), extractedInfo.getDeptIds());
            assertEquals(originalInfo.getRoles(), extractedInfo.getRoles());
        }

        @Test
        @DisplayName("无上下文时消息传递后上下文仍为空")
        void testTransmissionWithoutContext() {
            DataScopeContext.remove();

            OrderEvent event = createTestOrderEvent();
            Message<OrderEvent> message = MessageBuilder.withPayload(event).build();
            Message<OrderEvent> injectedMessage = DataScopeMessageUtils.injectDataScopeIntoMessage(message);

            DataScopeMessageUtils.extractDataScopeFromMessage(injectedMessage);

            assertNull(DataScopeContext.getUserId());
            assertNull(DataScopeContext.getOrgId());
            assertTrue(DataScopeContext.getDeptIds().isEmpty());
        }
    }

    @Nested
    @DisplayName("API接口测试（HTTP调用）")
    class ApiInterfaceTest {

        @Test
        @DisplayName("发送订单创建消息 - 通过API接口")
        void testSendOrderCreatedViaApi() throws Exception {
            setupUserContext("user-row-org10-admin");

            MvcResult result = mockMvc.perform(post("/api/messages/order/created")
                            .param("orderId", "1001")
                            .param("orderNo", "ORDER-API-001"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andReturn();

            String content = result.getResponse().getContentAsString();
            System.out.println("API response: " + content);

            assertTrue(content.contains("success"));
        }

        @Test
        @DisplayName("发送订单更新消息 - 通过API接口")
        void testSendOrderUpdatedViaApi() throws Exception {
            setupUserContext("user-row-org20-admin");

            MvcResult result = mockMvc.perform(post("/api/messages/order/updated")
                            .param("orderId", "1002")
                            .param("orderNo", "ORDER-API-002"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andReturn();

            String content = result.getResponse().getContentAsString();
            System.out.println("API response: " + content);

            assertTrue(content.contains("success"));
        }

        @Test
        @DisplayName("发送订单删除消息 - 通过API接口")
        void testSendOrderDeletedViaApi() throws Exception {
            setupUserContext("user-003");

            MvcResult result = mockMvc.perform(post("/api/messages/order/deleted")
                            .param("orderId", "1003"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andReturn();

            String content = result.getResponse().getContentAsString();
            System.out.println("API response: " + content);

            assertTrue(content.contains("success"));
        }

        @Test
        @DisplayName("获取当前上下文 - 通过API接口")
        void testGetContextViaApi() throws Exception {
            setupUserContext("user-row-org10-admin");

            MvcResult result = mockMvc.perform(get("/api/messages/context"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andReturn();

            String content = result.getResponse().getContentAsString();
            System.out.println("Context response: " + content);

            assertTrue(content.contains("userId"));
            assertTrue(content.contains("orgId"));
            assertTrue(content.contains("deptIds"));
            assertTrue(content.contains("roles"));
            assertTrue(content.contains("user-row-org10-admin"));
            assertTrue(content.contains("10"));
        }

        @Test
        @DisplayName("API测试 - 无上下文时发送消息")
        void testSendMessageWithoutContextViaApi() throws Exception {
            DataScopeContext.remove();

            MvcResult result = mockMvc.perform(post("/api/messages/order/created")
                            .param("orderId", "9999")
                            .param("orderNo", "ORDER-NOCONTEXT-001"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andReturn();

            String content = result.getResponse().getContentAsString();
            System.out.println("API response (no context): " + content);

            assertTrue(content.contains("success"));
        }

        @Test
        @DisplayName("完整场景测试 - API发送消息 + 模拟消费者接收")
        void testFullScenarioApiSendAndConsumerReceive() throws Exception {
            DataScopeContext.remove();

            setupUserContext("user-004");
            String originalUserId = DataScopeContext.getUserId();
            String originalOrgId = DataScopeContext.getOrgId();

            mockMvc.perform(post("/api/messages/order/created")
                            .param("orderId", "2001")
                            .param("orderNo", "ORDER-FULL-001"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            setupUserContext("user-004");

            OrderEvent event = createTestOrderEvent();
            Message<OrderEvent> message = MessageBuilder.withPayload(event).build();
            Message<OrderEvent> injectedMessage = DataScopeMessageUtils.injectDataScopeIntoMessage(message);

            DataScopeContext.remove();
            assertNull(DataScopeContext.getUserId());

            DataScopeMessageUtils.extractDataScopeFromMessage(injectedMessage);

            assertEquals(originalUserId, DataScopeContext.getUserId());
            assertEquals(originalOrgId, DataScopeContext.getOrgId());
            assertTrue(DataScopeContext.hasRole("EMPLOYEE"));
            assertTrue(DataScopeContext.hasRole("FINANCE"));
        }

        @Test
        @DisplayName("多角色用户通过API发送消息 - 上下文正确透传")
        void testMultiRoleUserApiTransmission() throws Exception {
            DataScopeContext.remove();

            setupUserContext("user-004");

            mockMvc.perform(post("/api/messages/test")
                            .param("orderId", "3001")
                            .param("orderNo", "ORDER-MULTIROLE-001"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            setupUserContext("user-004");

            OrderEvent event = createTestOrderEvent();
            Message<OrderEvent> message = MessageBuilder.withPayload(event).build();
            Message<OrderEvent> injectedMessage = DataScopeMessageUtils.injectDataScopeIntoMessage(message);

            DataScopeContext.remove();

            DataScopeMessageUtils.extractDataScopeFromMessage(injectedMessage);

            assertEquals("user-004", DataScopeContext.getUserId());
            assertTrue(DataScopeContext.hasRole("EMPLOYEE"));
            assertTrue(DataScopeContext.hasRole("FINANCE"));
            assertFalse(DataScopeContext.hasRole("ADMIN"));
        }
    }

    @Nested
    @DisplayName("上下文清理测试")
    class ContextCleanupTest {

        @Test
        @DisplayName("手动清理上下文")
        void testManualContextCleanup() {
            setupUserContext("user-002");
            assertNotNull(DataScopeContext.getUserId());

            DataScopeContext.remove();

            assertNull(DataScopeContext.getUserId());
            assertNull(DataScopeContext.getOrgId());
        }

        @Test
        @DisplayName("提取后清理上下文")
        void testCleanupAfterExtraction() {
            Message<OrderEvent> message = createMessageWithDataScopeHeaders(
                    "test-user", "10", Arrays.asList("1"), Arrays.asList("USER")
            );

            DataScopeMessageUtils.extractDataScopeFromMessage(message);
            assertNotNull(DataScopeContext.getUserId());

            DataScopeMessageUtils.clearDataScopeContext();
            assertNull(DataScopeContext.getUserId());
        }
    }

    private OrderEvent createTestOrderEvent() {
        return OrderEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .orderId(1001L)
                .orderNo("ORDER-TEST-001")
                .eventType("ORDER_CREATED")
                .timestamp(LocalDateTime.now())
                .build();
    }

    private Message<OrderEvent> createMessageWithDataScopeHeaders(
            String userId, String orgId, java.util.List<String> deptIds, java.util.List<String> roles) {
        OrderEvent event = createTestOrderEvent();
        MessageBuilder<OrderEvent> builder = MessageBuilder.withPayload(event);

        if (userId != null) {
            builder.setHeader("X-DataScope-User-Id", userId);
        }
        if (orgId != null) {
            builder.setHeader("X-DataScope-Org-Id", orgId);
        }
        if (deptIds != null && !deptIds.isEmpty()) {
            builder.setHeader("X-DataScope-Dept-Ids", String.join(",", deptIds));
        }
        if (roles != null && !roles.isEmpty()) {
            builder.setHeader("X-DataScope-Roles", String.join(",", roles));
        }

        return builder.build();
    }
}
