package ua.privatbank.apiworker;

import android.content.Context;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public abstract class ApiRequest {

    private HashMap<String, String> params = new HashMap<>();
    private HashMap<String, String> storeParams = new HashMap<>();
    private JSONObject jsonBody = new JSONObject();
    private JSONObject jsonParams = new JSONObject();
    private JSONObject jsonStoreParams = new JSONObject();

    public ApiRequest(Context context) {
        params = new HashMap<>();
        storeParams = new HashMap<>();
        jsonBody = new JSONObject();
        jsonParams = new JSONObject();
        jsonStoreParams = new JSONObject();
        onCreate(context, params);
    }

    public abstract void onCreate(Context context, HashMap<String, String> params);

    public ApiRequest addToParams(String key, String value) {
        params.put(key, value);
        return this;
    }

    public ApiRequest addToStoreParams(String key, String value) {
        storeParams.put(key, value);
        return this;
    }

    @Override
    public String toString() {
        if (!params.isEmpty()) {
            try {
                for (Map.Entry<String, String> entry : params.entrySet()) {
                    jsonParams.put(entry.getKey(), entry.getValue());
                }
                jsonBody.put("params", jsonParams);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        if (!storeParams.isEmpty()) {
            try {
                for (Map.Entry<String, String> entry : storeParams.entrySet()) {
                    jsonStoreParams.put(entry.getKey(), entry.getValue());
                }
                jsonBody.put("store_params", jsonStoreParams);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return jsonBody.toString();
    }

}