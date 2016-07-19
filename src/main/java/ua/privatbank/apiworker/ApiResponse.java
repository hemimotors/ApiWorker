package ua.privatbank.apiworker;

import com.google.gson.annotations.SerializedName;

public class ApiResponse {

    private boolean success;
    @SerializedName("error") ApiError error;

    public ApiResponse() {

    }

    public ApiResponse(ApiError error) {
        this.error = error;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public boolean isSuccess() {
        return success;
    }

    public ApiError getError() {
        return error;
    }

}