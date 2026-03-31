package DTO;


public class ApiResponseWrapper<T> {

    private boolean success;
    private String message;
    private T data;
    private String token;

    public ApiResponseWrapper() {
    }

    public ApiResponseWrapper(boolean success, String message, T data, String token) {
        this.success = success;
        this.message = message;
        this.data = data;
        this.token = token;
    }

    //  OK
    public static <T> ApiResponseWrapper<T> ok(T data) {
        return new ApiResponseWrapper<>(true, null, data, null);
    }

    // OK
    public static <T> ApiResponseWrapper<T> ok(String message, String token) {
        return new ApiResponseWrapper<>(true, message, null, token);
    }

    // Error
    public static <T> ApiResponseWrapper<T> error(String message) {
        return new ApiResponseWrapper<>(false, message, null, null);
    }

    // getters & setters

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public T getData() {
        return data;
    }

    public String getToken() {
        return token;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setData(T data) {
        this.data = data;
    }

    public void setToken(String token) {
        this.token = token;
    }
}