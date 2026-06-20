package cn.structured.datascope.example.job.controller;

import cn.structured.datascope.example.job.dto.TaskResponse;
import cn.structured.datascope.example.job.service.TaskService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 任务控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping
    public ResponseEntity<List<TaskResponse>> getTaskList() {
        log.info("API: GET /api/tasks");
        List<TaskResponse> tasks = taskService.getTaskList();
        return ResponseEntity.ok(tasks);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TaskResponse> getTaskById(@PathVariable String id) {
        log.info("API: GET /api/tasks/{}", id);
        TaskResponse task = taskService.getTaskById(id);
        if (task == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(task);
    }

    @PostMapping
    public ResponseEntity<TaskResponse> createTask(@RequestBody TaskResponse request) {
        log.info("API: POST /api/tasks");
        TaskResponse task = taskService.createTask(request);
        return ResponseEntity.ok(task);
    }

    @PutMapping("/{id}")
    public ResponseEntity<TaskResponse> updateTask(
            @PathVariable String id,
            @RequestBody TaskResponse request) {
        log.info("API: PUT /api/tasks/{}", id);
        TaskResponse task = taskService.updateTask(id, request);
        if (task == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(task);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTask(@PathVariable String id) {
        log.info("API: DELETE /api/tasks/{}", id);
        taskService.deleteTask(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/execute")
    public ResponseEntity<Void> executeTask(@PathVariable String id) {
        log.info("API: POST /api/tasks/{}/execute", id);
        taskService.executeTask(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/batch-execute")
    public ResponseEntity<Void> executeTasksByDataScope() {
        log.info("API: POST /api/tasks/batch-execute");
        taskService.executeTasksByDataScope();
        return ResponseEntity.ok().build();
    }

    @GetMapping("/count")
    public ResponseEntity<Long> getTaskCount() {
        log.info("API: GET /api/tasks/count");
        long count = taskService.getTaskCount();
        return ResponseEntity.ok(count);
    }
}