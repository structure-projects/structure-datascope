package cn.structured.datascope.example.job.service;

import cn.structured.datascope.DataScopeContext;
import cn.structured.datascope.DataScopeInfo;
import cn.structured.datascope.example.job.dto.TaskResponse;
import cn.structured.datascope.provider.DataScopeProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 任务服务 - 业务逻辑层
 *
 * <p>职责：</p>
 * <ul>
 *     <li>任务数据的增删改查</li>
 *     <li>任务执行逻辑</li>
 *     <li>数据权限范围内的业务处理</li>
 * </ul>
 *
 * <p>注意：数据权限上下文由 JobHandler 在调用前设置和清理</p>
 *
 * @see cn.structured.datascope.example.job.handler.DataSyncTask
 * @see cn.structured.datascope.example.job.handler.AdminTask
 * @see cn.structured.datascope.example.job.handler.BatchExecuteTask
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TaskService {

    private final DataScopeProvider dataScopeProvider;

    /**
     * 模拟任务存储
     */
    private final Map<String, TaskResponse> taskStorage = new ConcurrentHashMap<>();

    // ==================== JobHandler 调用方法 ====================

    /**
     * 执行数据同步任务
     *
     * <p>由 DataSyncTask 调用，上下文已设置</p>
     */
    public void executeDataSync() {
        // 获取当前上下文中的权限信息
        String orgId = DataScopeContext.getOrgId();
        List<String> deptIds = DataScopeContext.getDeptIds();

        log.debug("Processing data with scope filter: orgId={}, deptIds={}", orgId, deptIds);

        // 获取权限范围内的任务
        List<TaskResponse> tasks = getTaskList();
        for (TaskResponse task : tasks) {
            processTask(task);
        }

        log.info("Data sync completed, processed {} tasks", tasks.size());
    }

    /**
     * 执行管理任务
     *
     * <p>由 AdminTask 调用，使用系统用户上下文</p>
     *
     * @return 处理的任务数量
     */
    public int executeAdminTask() {
        // 系统用户可以访问全部数据
        List<TaskResponse> allTasks = getAllTasksWithoutScope();
        log.info("Executing admin task, processing {} tasks", allTasks.size());

        for (TaskResponse task : allTasks) {
            processTask(task);
        }

        return allTasks.size();
    }

    /**
     * 批量执行任务（根据创建者权限）
     *
     * <p>由 BatchExecuteTask 调用</p>
     *
     * @param status 任务状态
     * @return int[]{成功数量, 失败数量}
     */
    public int[] batchExecuteWithCreatorScope(String status) {
        // 获取所有待执行的任务
        List<TaskResponse> targetTasks = taskStorage.values().stream()
                .filter(task -> status.equals(task.getStatus()))
                .collect(Collectors.toList());

        log.info("Found {} tasks with status: {}", targetTasks.size(), status);

        int successCount = 0;
        int failCount = 0;

        for (TaskResponse task : targetTasks) {
            // 使用任务创建者的 userId 执行任务
            String creatorId = task.getCreator();
            log.debug("Executing task {} with creator's scope: {}", task.getId(), creatorId);

            try {
                // 获取创建者的数据权限
                DataScopeInfo info = dataScopeProvider.getScopeInfo(creatorId);
                if (info == null) {
                    log.warn("No scope info for creator: {}, skip task", creatorId);
                    failCount++;
                    continue;
                }

                // 设置创建者的上下文
                DataScopeContext.setInfo(info);
                try {
                    // 使用创建者的数据权限处理任务
                    processTask(task);
                    successCount++;
                } finally {
                    DataScopeContext.remove();
                }
            } catch (Exception e) {
                log.error("Task {} execution failed", task.getId(), e);
                failCount++;
            }
        }

        log.info("Batch execute completed: {} success, {} failed", successCount, failCount);
        return new int[]{successCount, failCount};
    }

    // ==================== 业务方法 ====================

    /**
     * 在数据权限范围内处理数据
     *
     * <p>此方法在 DataScopeContext 已设置的情况下执行，
     * DataScopeContext 已经设置好了用户的数据权限信息。</p>
     */
    private void processDataWithScope() {
        // 获取当前上下文中的权限信息
        String orgId = DataScopeContext.getOrgId();
        List<String> deptIds = DataScopeContext.getDeptIds();

        log.debug("Processing data with scope filter: orgId={}, deptIds={}", orgId, deptIds);

        // 获取权限范围内的任务
        List<TaskResponse> tasks = getTaskList();
        for (TaskResponse task : tasks) {
            if (isInScope(task, orgId, deptIds)) {
                processTask(task);
            }
        }
    }

    /**
     * 获取任务列表（带数据权限过滤）
     *
     * <p>在 DataScopeContext 已设置的情况下，返回当前用户权限范围内的任务</p>
     */
    public List<TaskResponse> getTaskList() {
        String orgId = DataScopeContext.getOrgId();
        List<String> deptIds = DataScopeContext.getDeptIds();

        log.debug("Getting task list with scope: orgId={}, deptIds={}", orgId, deptIds);

        return taskStorage.values().stream()
                .filter(task -> isInScope(task, orgId, deptIds))
                .collect(Collectors.toList());
    }

    /**
     * 获取全部任务（不受数据权限限制）
     *
     * <p>仅在系统用户上下文中使用</p>
     */
    private List<TaskResponse> getAllTasksWithoutScope() {
        return new ArrayList<>(taskStorage.values());
    }

    /**
     * 检查任务是否在当前用户的数据权限范围内
     */
    private boolean isInScope(TaskResponse task, String orgId, List<String> deptIds) {
        // 检查组织权限
        if (orgId != null && !orgId.equals(task.getOrgId())) {
            return false;
        }

        // 检查部门权限
        if (deptIds != null && !deptIds.isEmpty() && task.getDeptId() != null) {
            return deptIds.contains(task.getDeptId());
        }

        return true;
    }

    /**
     * 处理单个任务
     */
    private void processTask(TaskResponse task) {
        log.info("Processing task: {}", task.getTaskName());
        task.setLastExecuteTime(LocalDateTime.now());
        task.setStatus("SUCCESS");
    }

    /**
     * 获取任务详情
     */
    public TaskResponse getTaskById(String taskId) {
        return taskStorage.get(taskId);
    }

    /**
     * 创建任务
     *
     * <p>任务创建时会记录创建者的 userId，后续执行时使用此 userId 获取数据权限</p>
     */
    public TaskResponse createTask(TaskResponse request) {
        log.info("Creating task: {}", request.getTaskName());

        String taskId = System.currentTimeMillis() + "";
        request.setId(taskId);

        // 从当前上下文获取用户信息
        String currentUserId = DataScopeContext.getUserId();
        String currentOrgId = DataScopeContext.getOrgId();
        List<String> currentDeptIds = DataScopeContext.getDeptIds();

        // 设置任务创建者信息，后续定时任务执行时使用此 userId
        request.setCreator(currentUserId != null ? currentUserId : "system");
        request.setOrgId(currentOrgId);
        request.setDeptId(currentDeptIds.isEmpty() ? null : currentDeptIds.get(0));
        request.setCreateTime(LocalDateTime.now());
        request.setStatus("PENDING");

        taskStorage.put(taskId, request);
        log.info("Task created: {}, creator: {}", taskId, request.getCreator());

        return request;
    }

    /**
     * 更新任务
     */
    public TaskResponse updateTask(String taskId, TaskResponse request) {
        TaskResponse existing = taskStorage.get(taskId);
        if (existing == null) {
            return null;
        }

        existing.setTaskName(request.getTaskName());
        existing.setCronExpression(request.getCronExpression());
        existing.setDescription(request.getDescription());
        existing.setUpdateTime(LocalDateTime.now());
        existing.setUpdater(DataScopeContext.getUserId());

        return existing;
    }

    /**
     * 删除任务
     */
    public void deleteTask(String taskId) {
        log.info("Deleting task: {}", taskId);
        taskStorage.remove(taskId);
    }

    /**
     * 执行指定任务
     *
     * <p>使用任务创建者的数据权限执行任务</p>
     *
     * @param taskId 任务ID
     */
    public void executeTask(String taskId) {
        log.info("Executing task: {}", taskId);

        TaskResponse task = taskStorage.get(taskId);
        if (task == null) {
            log.warn("Task not found: {}", taskId);
            return;
        }

        // 使用任务创建者的 userId 执行任务
        String creatorId = task.getCreator();
        log.debug("Executing task {} with creator's scope: {}", taskId, creatorId);

        try {
            // 获取创建者的数据权限
            DataScopeInfo info = dataScopeProvider.getScopeInfo(creatorId);
            if (info == null) {
                log.warn("No scope info for creator: {}, use current context", creatorId);
                processTask(task);
            } else {
                // 设置创建者的上下文
                DataScopeContext.setInfo(info);
                try {
                    processTask(task);
                } finally {
                    DataScopeContext.remove();
                }
            }
        } catch (Exception e) {
            log.error("Task {} execution failed", taskId, e);
            task.setStatus("FAILED");
        }
    }

    /**
     * 根据数据权限批量执行任务
     *
     * <p>使用当前用户的数据权限执行所有符合条件的任务</p>
     */
    public void executeTasksByDataScope() {
        log.info("Executing tasks by data scope...");

        String orgId = DataScopeContext.getOrgId();
        List<String> deptIds = DataScopeContext.getDeptIds();

        log.debug("Current scope: orgId={}, deptIds={}", orgId, deptIds);

        // 获取当前用户权限范围内的任务
        List<TaskResponse> tasks = getTaskList();

        int successCount = 0;
        int failCount = 0;

        for (TaskResponse task : tasks) {
            try {
                processTask(task);
                successCount++;
            } catch (Exception e) {
                log.error("Task {} execution failed", task.getId(), e);
                task.setStatus("FAILED");
                failCount++;
            }
        }

        log.info("Execute tasks by data scope completed: {} success, {} failed", successCount, failCount);
    }

    /**
     * 批量执行所有待执行的任务
     *
     * <p>每个任务使用其创建者的数据权限执行</p>
     */
    public void executeAllPendingTasks() {
        log.info("Executing all pending tasks...");

        List<TaskResponse> pendingTasks = taskStorage.values().stream()
                .filter(task -> "PENDING".equals(task.getStatus()))
                .collect(Collectors.toList());

        for (TaskResponse task : pendingTasks) {
            String creatorId = task.getCreator();

            // 获取创建者的数据权限并执行
            DataScopeInfo info = dataScopeProvider.getScopeInfo(creatorId);
            if (info == null) {
                log.warn("No scope info for creator: {}", creatorId);
                continue;
            }

            DataScopeContext.setInfo(info);
            try {
                processTask(task);
            } finally {
                DataScopeContext.remove();
            }
        }

        log.info("Executed {} pending tasks", pendingTasks.size());
    }

    /**
     * 获取任务数量
     */
    public long getTaskCount() {
        return taskStorage.size();
    }
}
