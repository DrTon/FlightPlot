package me.drton.flightplot;

import org.json.JSONObject;

import java.awt.*;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * User: ton Date: 22.06.13 Time: 15:08
 */
public class ProcessorPreset {
    private String title;
    private String processorType;
    private Map<String, Object> parameters;
    private Map<String, Color> colors;

    public ProcessorPreset(String title, String processorType, Map<String, Object> parameters, Map<String, Color> colors) {
        this.title = title;
        this.processorType = processorType;
        this.parameters = parameters;
        this.colors = colors;
    }

    public static ProcessorPreset unpackJSONObject(JSONObject json) throws IOException {
        JSONObject jsonParameters = json.getJSONObject("Parameters");
        Map<String, Object> parametersNew = new HashMap<String, Object>();
        for (Object key : jsonParameters.keySet()) {
            String keyStr = (String) key;
            parametersNew.put(keyStr, jsonParameters.get(keyStr).toString());
        }
        JSONObject jsonColors = json.getJSONObject("Colors");
        Map<String, Color> colorsNew = new HashMap<String, Color>();
        for (Object key : jsonColors.keySet()) {
            String keyStr = (String) key;
            colorsNew.put(keyStr, new Color(Integer.parseInt(jsonColors.get(keyStr).toString(), 16)));
        }
        return new ProcessorPreset(json.getString("Title"), json.getString("ProcessorType"), parametersNew, colorsNew);
    }

    private static Object castValue(Object valueOld, Object valueNewObj) {
        String valueNewStr = valueNewObj.toString();
        Object valueNew = valueNewObj;
        if (valueOld instanceof String) {
            valueNew = valueNewStr;
        } else if (valueOld instanceof Double) {
            valueNew = Double.parseDouble(valueNewStr);
        } else if (valueOld instanceof Float) {
            valueNew = Float.parseFloat(valueNewStr);
        } else if (valueOld instanceof Integer) {
            valueNew = Integer.parseInt(valueNewStr);
        } else if (valueOld instanceof Long) {
            valueNew = Long.parseLong(valueNewStr);
        } else if (valueOld instanceof Boolean) {
            char firstChar = valueNewStr.toLowerCase().charAt(0);
            if (firstChar == 'f' || firstChar == 'n' || "0".equals(valueNewStr))
                valueNew = false;
            else
                valueNew = true;
        }
        return valueNew;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getProcessorType() {
        return processorType;
    }

    public void setProcessorType(String processorType) {
        this.processorType = processorType;
    }

    public Map<String, Object> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, Object> parameters) {
        this.parameters = parameters;
    }

    public void setParameter(String key, String value) {
        Object oldValue = parameters.get(key);
        if (oldValue != null) {
            parameters.put(key, castValue(oldValue, value));
        }
    }

    public Map<String, Color> getColors() {
        return colors;
    }

    public void setColors(Map<String, Color> colors) {
        this.colors = colors;
    }

    public JSONObject packJSONObject() throws IOException {
        JSONObject json = new JSONObject();
        json.put("Title", title);
        json.put("ProcessorType", processorType);
        json.put("Parameters", new JSONObject(parameters));
        Map<String, String> jsonColors = new HashMap<String, String>();
        for (Map.Entry<String, Color> entry : colors.entrySet()) {
            jsonColors.put(entry.getKey(), Integer.toHexString(entry.getValue().getRGB()));
        }
        json.put("Colors", new JSONObject(jsonColors));
        return json;
    }

    @Override
    public String toString() {
        return title + " [" + processorType + "]";
    }
}
