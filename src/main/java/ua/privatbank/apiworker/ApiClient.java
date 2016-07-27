package ua.privatbank.apiworker;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import com.google.gson.Gson;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;

public abstract class ApiClient {

    private final Context context;
    private final OkHttpClient defaultClient;
    private String connectionErrorText = "no connection";
    private String emptyBodyErrorText = "no body";

    public ApiClient(Context context, boolean logging, long connectTimeoutSeconds, long readTimeoutSeconds) {
        this.context = context;
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(logging ? HttpLoggingInterceptor.Level.BODY : HttpLoggingInterceptor.Level.NONE);
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        builder.connectTimeout(connectTimeoutSeconds, TimeUnit.SECONDS);
        builder.readTimeout(readTimeoutSeconds, TimeUnit.SECONDS);
        builder.addInterceptor(loggingInterceptor);
        defaultClient = builder.build();
    }

    public abstract void onResponse(ApiResponse response);

    protected <T extends ApiResponse> void get(String url, Class<T> clazz, Callback<T> callback) {
        get(defaultClient, url, clazz, callback);
    }

    protected <T extends ApiResponse> void get(OkHttpClient client, String url, Class<T> clazz, Callback<T> callback) {
        if (isOnline()) {
            Request request = new Request.Builder().url(url).get().build();
            client.newCall(request).enqueue(new CallbackWrapper<>(clazz, callback));
        } else {
            ApiHandler<T> apiHandler = new ApiHandler<>(clazz);
            callback.onResponse(apiHandler.createErrorResponse(ApiError.ERROR_CODE_NO_CONNECTION, connectionErrorText));
        }
    }

    protected <T extends ApiResponse> T get(String url, Class<T> clazz) {
        return get(defaultClient, url, clazz);
    }

    protected <T extends ApiResponse> T get(OkHttpClient client, String url, Class<T> clazz) {
        ApiHandler<T> apiHandler = new ApiHandler<>(clazz);
        if (isOnline()) {
            Request request = new Request.Builder().url(url).get().build();
            try {
                Response response = client.newCall(request).execute();
                return apiHandler.createTypedResponse(response);
            } catch (IOException e) {
                return apiHandler.createErrorResponse(0, e.getMessage());
            }
        } else {
            return apiHandler.createErrorResponse(ApiError.ERROR_CODE_NO_CONNECTION, connectionErrorText);
        }
    }

    protected <T extends ApiResponse> void post(String url, ApiRequest apiRequest, Class<T> clazz, Callback<T> callback) {
        post(defaultClient, url, apiRequest, clazz, callback);
    }

    protected <T extends ApiResponse> void post(OkHttpClient client, String url, ApiRequest apiRequest, Class<T> clazz, Callback<T> callback) {
        if (isOnline()) {
            RequestBody requestBody = RequestBody.create(MediaType.parse("application/json"), apiRequest.toString());
            Request request = new Request.Builder().url(url).post(requestBody).build();
            client.newCall(request).enqueue(new CallbackWrapper<>(clazz, callback));
        } else {
            ApiHandler<T> apiHandler = new ApiHandler<>(clazz);
            callback.onResponse(apiHandler.createErrorResponse(ApiError.ERROR_CODE_NO_CONNECTION, connectionErrorText));
        }
    }

    protected <T extends ApiResponse> T post(String url, ApiRequest apiRequest, Class<T> clazz) {
        return post(defaultClient, url, apiRequest, clazz);
    }

    protected <T extends ApiResponse> T post(OkHttpClient client, String url, ApiRequest apiRequest, Class<T> clazz) {
        ApiHandler<T> apiHandler = new ApiHandler<>(clazz);
        if (isOnline()) {
            RequestBody requestBody = RequestBody.create(MediaType.parse("application/json"), apiRequest.toString());
            Request request = new Request.Builder().url(url).post(requestBody).build();
            try {
                Response response = client.newCall(request).execute();
                return apiHandler.createTypedResponse(response);
            } catch (IOException e) {
                return apiHandler.createErrorResponse(0, e.getMessage());
            }
        } else {
            return apiHandler.createErrorResponse(ApiError.ERROR_CODE_NO_CONNECTION, connectionErrorText);
        }
    }

    protected void setConnectionErrorText(String connectionErrorText) {
        this.connectionErrorText = connectionErrorText;
    }

    protected void setEmptyBodyErrorText(String emptyBodyErrorText) {
        this.emptyBodyErrorText = emptyBodyErrorText;
    }

    public Context getContext() {
        return context;
    }

    public boolean isOnline() {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnectedOrConnecting();
    }

    public interface Callback<T> {
        void onResponse(T response);
    }

    private class CallbackWrapper<T extends ApiResponse> implements okhttp3.Callback {

        private final ApiHandler<T> responseHandler;
        private final Callback<T> callback;

        public CallbackWrapper(Class<T> clazz, Callback<T> callback) {
            this.responseHandler = new ApiHandler<>(clazz);
            this.callback = callback;
        }

        @Override
        public void onFailure(Call call, IOException e) {
            callback.onResponse(responseHandler.createErrorResponse(0, e.getMessage()));
        }

        @Override
        public void onResponse(Call call, Response response) throws IOException {
            callback.onResponse(responseHandler.createTypedResponse(response));
        }
    }

    private class ApiHandler<T extends ApiResponse> {

        private final Class<T> clazz;

        public ApiHandler(Class<T> tClass) {
            this.clazz = tClass;
        }

        public T createErrorResponse(int errorCode, String errorMessage) {
            ApiResponse response = new ApiResponse(new ApiError(errorCode, errorMessage));
            return new Gson().fromJson(new Gson().toJson(response), clazz);
        }

        public T createTypedResponse(Response response) {
            if (response.body() == null) {
                T errorResponse = createErrorResponse(ApiError.ERROR_CODE_NO_BODY, emptyBodyErrorText);
                onResponse(errorResponse);
                return errorResponse;
            } else {
                try {
                    T typedResponse = new Gson().fromJson(response.body().charStream(), clazz);
                    if (typedResponse == null) {
                        T errorResponse = createErrorResponse(ApiError.ERROR_CODE_NO_BODY, emptyBodyErrorText);
                        onResponse(errorResponse);
                        return errorResponse;
                    } else if (typedResponse.getError() != null) {
                        onResponse(typedResponse);
                    } else {
                        typedResponse.setSuccess(true);
                        onResponse(typedResponse);
                    }
                    return typedResponse;
                } catch (Exception e) {
                    T errorResponse = createErrorResponse(ApiError.ERROR_CODE_NO_BODY, emptyBodyErrorText);
                    onResponse(errorResponse);
                    return errorResponse;
                }
            }
        }
    }

}