import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.json.simple.JSONObject;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

/**
 * @author Vannaravuth YO
 * @since 25-May-22, 4:57 PM
 */
public class ApiMapper {

    public static final ObjectMapper mapper = new ObjectMapper();

    public static <T> T map(Object source, Class<T> target) {
        return mapper.convertValue(source, target);
    }

    public static <T> T map(Object fromValue, TypeReference<?> toValueTypeRef) {
        return mapper.convertValue(fromValue, toValueTypeRef);
    }

    public static String toGSONString(Object object) {
        if (object == null) {
            return "{}";
        }
        GsonBuilder builder = new GsonBuilder();
        builder.serializeNulls();
        Gson gson = builder.setPrettyPrinting().create();
        return gson.toJson(object);
    }

    public static <T> String toJSONString(T object) {
        if (object instanceof Map) {
            return new JSONObject((Map<String, Object>) object).toString();
        } else if (object != null) {
            try {
                return mapper.writer()
                        .withDefaultPrettyPrinter()
                        .writeValueAsString(object);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }
        return "{}";
    }

    public static String toURIString(String url, Map<String, Object> params) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url);
        if (params != null) {
            for (Map.Entry<String, Object> entry : params.entrySet()) {
                builder.queryParam(entry.getKey(), entry.getValue());
            }
        }
        // UriComponents builder = UriComponentsBuilder.fromHttpUrl(url)
        //        .queryParam("size", "1")
        //        .queryParam("page", "0")
        //        .build();
        return builder.toUriString();
    }

}
