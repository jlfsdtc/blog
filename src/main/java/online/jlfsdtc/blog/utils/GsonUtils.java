package online.jlfsdtc.blog.utils;

import com.google.gson.Gson;

public class GsonUtils {
    private static final Gson GSON = new Gson();

    public static String toJsonString(Object object) {
        return object == null ? null : GSON.toJson(object);
    }
}
