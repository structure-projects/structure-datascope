package cn.structured.datascope.example.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 统一响应结果
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {

    /**
     * 状态码
     */
    private int code;

    /**
     * 消息
     */
    private String message;

    /**
     * 数据
     */
    private T data;

    /**
     * 当前数据范围ID
     */
    private String dataScopeId;

    /**
     * 当前用户角色
     */
    private List<String> roles;

    /**
     * 创建成功响应
     */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(200, "success", data, null, null);
    }

    /**
     * 创建成功响应（带数据范围信息）
     */
    public static <T> ApiResponse<T> success(T data, String dataScopeId, List<String> roles) {
        return new ApiResponse<>(200, "success", data, dataScopeId, roles);
    }

    /**
     * 创建错误响应
     */
    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(500, message, null, null, null);
    }
}