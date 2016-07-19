package ua.privatbank.apiworker;

import com.google.gson.annotations.SerializedName;

public class ApiError {

    public static final int ERROR_CODE_NO_BODY = 899;
    public static final int ERROR_CODE_NO_CONNECTION = 900;

    @SerializedName("code") private int code;
    @SerializedName("message") private String message;

    public ApiError() {

    }

    public ApiError(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public ApiError setCode(int code) {
        this.code = code;
        return this;
    }

    public String getMessage() {
        return message;
    }

    public ApiError setMessage(String message) {
        this.message = message;
        return this;
    }

}