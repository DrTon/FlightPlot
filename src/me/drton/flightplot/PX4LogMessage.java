package me.drton.flightplot;

import java.util.List;

/**
 * User: ton Date: 03.06.13 Time: 16:18
 */
public class PX4LogMessage {
    public final PX4LogMessageDescription description;
    private final List<Object> data;

    public PX4LogMessage(PX4LogMessageDescription description, List<Object> data) {
        this.description = description;
        this.data = data;
    }

    public Object get(int idx) {
        return data.get(idx);
    }

    public long getLong(int idx) {
        return (Long) data.get(idx);
    }

    public Object get(String field) {
        int idx = description.getFieldIdx(field);
        return data.get(idx);
    }

    @Override
    public String toString() {
        return String.format("PX4LogMessage: type=%s, name=%s, data=%s", description.type, description.name, data);
    }
}
