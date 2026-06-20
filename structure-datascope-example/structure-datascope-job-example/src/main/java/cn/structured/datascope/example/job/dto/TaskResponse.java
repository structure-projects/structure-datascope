package cn.structured.datascope.example.job.dto;

import cn.structured.datascope.annotation.DataScopeField;
import cn.structured.datascope.annotation.DataScopeRule;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 任务响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@DataScopeRule(resource = "task")
public class TaskResponse {

    private String id;
    private String taskName;

    @DataScopeField(visible = true)
    private String cronExpression;

    @DataScopeField(hiddenIfRoleIn = {"EMPLOYEE"})
    private String description;

    @DataScopeField(visible = true)
    private String status;

    @DataScopeField(visible = true)
    private String orgId;

    @DataScopeField(visible = true)
    private String deptId;

    @DataScopeField(visibleIfRoleIn = {"SYS_ADMIN"})
    private String creator;

    private LocalDateTime createTime;

    @DataScopeField(visibleIfRoleIn = {"SYS_ADMIN"})
    private String updater;

    private LocalDateTime updateTime;
    private LocalDateTime lastExecuteTime;
}