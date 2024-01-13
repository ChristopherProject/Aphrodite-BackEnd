package it.adrian.code.util.system;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class Configuration {

    public String getDataByKey(String key){
        InputStream inputStream = Configuration.class.getClassLoader().getResourceAsStream("config.yaml");
        if (inputStream != null) {
            Map<String, Object> yamlData = parseYaml(inputStream);
            if (yamlData != null) {
                return yamlData.get(key).toString().replace("'", "").replace("/", File.separator);
            }
        }
        return "";
    }

    private static Map<String, Object> parseYaml(InputStream inputStream) {
        Map<String, Object> yamlData = new HashMap<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            String currentKey = null;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
              if (line.contains(":")) {
                    String[] parts = line.split(":", 2);
                    currentKey = parts[0].trim();
                    String value = parts[1].trim();
                    yamlData.put(currentKey, value);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return yamlData;
    }

}