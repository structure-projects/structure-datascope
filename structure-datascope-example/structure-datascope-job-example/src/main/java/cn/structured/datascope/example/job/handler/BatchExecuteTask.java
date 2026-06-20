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
 * 批量执行任务处理器
 *
 * <p>职责：批量执行待处理任务，每个任务使用其创建者的数据权限</p>
 *
 * <p>XXL-JOB 配置：</p>
 * <ul>
 *     <li>任务名称：批量执行待处理任务</li>
 *     <li>任务参数：PENDING（任务状态）</li>
 *     <li>任务Handler：batchExecuteTask</li>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BatchExecuteTask {

    private final DataScopeProvider dataScopeProvider;
    private final TaskService taskService;

    /**
     * 批量执行任务
     */
    @XxlJob("batchExecuteTask")
    public void batchExecuteTask() {
        log.info("Starting batch execute tasks...");

        // 获取需要执行的任务状态
        String status = XxlJobHelper.getJobParam();
        if (status == null || status.isEmpty()) {
            status = "PENDING";
        }

        try {
            // 执行批量任务（由 TaskService 管理上下文切换）
            int[] result = taskService.batchExecuteWithCreatorScope(status);

            XxlJobHelper.handleSuccess(String.format("Batch execute completed: %d success, %d failed",
                    result[0], result[1]));
        } catch (Exception e) {
            log.error("Batch execute failed", e);
            XxlJobHelper.handleFail("Batch execute failed: " + e.getMessage());
        }
    }
}
