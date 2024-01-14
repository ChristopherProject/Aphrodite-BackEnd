package it.adrian.code.util.system;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class Configuration {

    private static Map<String, Object> parseYaml(InputStream inputStream) {
        Map<String, Object> yamlData = new HashMap<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            String currentKey;
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

    private String getDataByKey(String key) {
        try {
            final InputStream inputStream = new FileInputStream(System.getProperty("user.dir") + File.separator + "config.yaml");
            if (inputStream != null) {
                Map<String, Object> yamlData = parseYaml(inputStream);
                if (yamlData != null) {
                    return yamlData.get(key).toString().replace("'", "").replace("/", File.separator);
                }
            }
        } catch (Exception e) {
            return "";
        }
        return "";
    }


    public String getString(String key){
        return getDataByKey(key);
    }

    public boolean getBoolean(String key){
        return Boolean.parseBoolean(getDataByKey(key));
    }

    public int getInt(String key){
        return  Integer.parseInt(getDataByKey(key));
    }
}