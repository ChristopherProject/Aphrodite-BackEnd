package it.adrian.code.util.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.HashMap;
import java.util.Map;

public class JSON {

    public static ObjectNode generateJSON(HashMap<String, String> data) {
        ObjectNode objectNode = new ObjectMapper().createObjectNode();
        for (Map.Entry<String, String> entry : data.entrySet()) {
            objectNode.put(entry.getKey(), entry.getValue());
        }
        return objectNode;
    }

    public static JsonNode parseStringToJson(String src) {
        try {
            final ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.readTree(src);
        } catch (JsonProcessingException e) {
            return null;
        }
    }
}
