package cn.structured.datascope.example.job.handler;

import cn.structured.datascope.DataScopeContext;
import cn.structured.datascope.DataScopeInfo;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import cn.structured.datascope.example.job.service.TaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 系统管理任务处理器
 *
 * <p>职责：使用系统用户上下文执行管理任务，拥有全部权限</p>
 *
 * <p>XXL-JOB 配置：</p>
 * <ul>
 *     <li>任务名称：系统管理任务</li>
 *     <li>任务参数：（无需参数）</li>
 *     <li>任务Handler：adminTask</li>
 *     <li>执行策略：单机串行</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminTask {

    private static final String SYSTEM_USER_ID = "SYSTEM";

    private final TaskService taskService;

    /**
     * 管理任务（使用系统用户执行）
     *
     * <p>创建系统用户上下文，拥有全部权限，可以访问所有数据</p>
     */
    @XxlJob("adminTask")
    public void adminTask() {
        log.info("Starting admin task with system user");

        // 创建系统用户上下文（拥有全部权限）
        DataScopeInfo systemInfo = createSystemContext();

        // 设置到上下文
        DataScopeContext.setInfo(systemInfo);

        try {
            log.info("Executing as system user with full permissions");

            // 执行管理任务，可以访问全部数据
            int processedCount = taskService.executeAdminTask();

            XxlJobHelper.handleSuccess("Admin task executed successfully, processed " + processedCount + " tasks");
        } catch (Exception e) {
            log.error("Admin task execution failed", e);
            XxlJobHelper.handleFail("Admin task execution failed: " + e.getMessage());
        } finally {
            // 清理上下文
            DataScopeContext.remove();
        }

        log.info("Admin task completed");
    }

    /**
     * 创建系统用户上下文
     */
    private DataScopeInfo createSystemContext() {
        DataScopeInfo info = new DataScopeInfo();
        info.setUserId(SYSTEM_USER_ID);
        info.setRoles(List.of("ADMIN", "SYSTEM"));
        // 系统用户不设置 orgId 和 deptIds，表示可以访问全部数据
        return info;
    }
}
