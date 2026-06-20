package cn.structured.datascope.example.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 统一响应结果
 */
@Data
@NoArgsConstructor
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
     * 当前用户角色
     */
    private List<String> roles;

    /**
     * 全参数构造函数
     */
    public ApiResponse(int code, String message, T data, List<String> roles) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.roles = roles;
    }

    /**
     * 创建成功响应
     */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(200, "success", data, null);
    }

    /**
     * 创建成功响应（带角色信息）
     */
    public static <T> ApiResponse<T> success(T data, List<String> roles) {
        return new ApiResponse<>(200, "success", data, roles);
    }

    /**
     * 创建错误响应
     */
    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(500, message, null, null);
    }
}