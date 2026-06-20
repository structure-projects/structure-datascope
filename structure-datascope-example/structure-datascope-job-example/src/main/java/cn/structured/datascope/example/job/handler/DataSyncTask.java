package cn.structured.datascope.example.job.handler;

import cn.structured.datascope.DataScopeContext;
import cn.structured.datascope.DataScopeInfo;
import cn.structured.datascope.provider.DataScopeProvider;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import cn.structured.datascope.example.job.service.TaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * XXL-JOB 任务处理器
 *
 * <p>职责：</p>
 * <ul>
 *     <li>从 XXL-JOB 获取任务参数</li>
 *     <li>通过 DataScopeProvider 获取用户数据权限</li>
 *     <li>设置和清理 DataScopeContext</li>
 *     <li>调用业务 Service 执行实际任务</li>
 * </ul>
 *
 * <p>命名规范：xxxTask.java</p>
 *
 * @see DataScopeContext
 * @see DataScopeProvider
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataSyncTask {

    private final DataScopeProvider dataScopeProvider;
    private final TaskService taskService;

    /**
     * 数据同步任务
     *
     * <p>XXL-JOB 配置：</p>
     * <ul>
     *     <li>任务名称：数据同步任务</li>
     *     <li>任务参数：user-123（执行用户ID）</li>
     *     <li>任务Handler：dataSyncTask</li>
     *     <li>Cron表达式：0 0 2 * * ?（每日凌晨2点）</li>
     * </ul>
     */
    @XxlJob("dataSyncTask")
    public void dataSyncTask() {
        // 从 XXL-JOB 任务参数获取需要执行的用户ID
        String userId = XxlJobHelper.getJobParam();
        if (userId == null || userId.isEmpty()) {
            log.warn("No userId provided in job parameter");
            XxlJobHelper.handleFail("No userId provided in job parameter");
            return;
        }

        log.info("Starting data sync task for user: {}", userId);

        // 初始化数据权限上下文
        DataScopeInfo info = initDataScopeContext(userId);
        if (info == null) {
            XxlJobHelper.handleFail("Failed to initialize data scope context");
            return;
        }

        try {
            // 执行业务逻辑
            taskService.executeDataSync();

            XxlJobHelper.handleSuccess("Task executed successfully");
        } catch (Exception e) {
            log.error("Task execution failed", e);
            XxlJobHelper.handleFail("Task execution failed: " + e.getMessage());
        } finally {
            // 清理上下文，避免内存泄漏
            DataScopeContext.remove();
        }

        log.info("Data sync task completed for user: {}", userId);
    }

    /**
     * 初始化数据权限上下文
     *
     * @param userId 用户ID
     * @return DataScopeInfo
     */
    private DataScopeInfo initDataScopeContext(String userId) {
        // 通过 DataScopeProvider 获取用户的数据权限信息
        DataScopeInfo info = dataScopeProvider.getScopeInfo(userId);
        if (info == null) {
            log.warn("No scope info found for user: {}, using empty context", userId);
            info = new DataScopeInfo();
            info.setUserId(userId);
        }

        // 设置到上下文
        DataScopeContext.setInfo(info);

        log.info("Data scope context initialized for user {}: orgId={}, deptIds={}, roles={}",
                userId, info.getOrgId(), info.getDeptIds(), info.getRoles());

        return info;
    }
}
