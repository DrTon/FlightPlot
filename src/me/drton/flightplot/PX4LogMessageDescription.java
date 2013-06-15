package me.drton.flightplot;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.*;

/**
 * User: ton Date: 03.06.13 Time: 14:35
 */
public class PX4LogMessageDescription {
    private static Charset charset = Charset.forName("latin1");

    static PX4LogMessageDescription FORMAT = new PX4LogMessageDescription(0x80, 89, "FMT", "BBnNZ",
            new String[]{"Type", "Length", "Name", "Format", "Labels"});

    public final int type;
    public final int length;
    public final String name;
    public final String format;
    public final String[] fields;
    public final Map<String, Integer> fieldsMap = new HashMap<String, Integer>();

    public PX4LogMessageDescription(int type, int length, String name, String format, String[] fields) {
        this.type = type;
        this.length = length;
        this.name = name;
        this.format = format;
        this.fields = fields;
    }

    public PX4LogMessageDescription(ByteBuffer buffer) {
        type = buffer.get() & 0xFF;
        length = buffer.get() & 0xFF;
        byte[] strBuf;
        strBuf = new byte[4];
        buffer.get(strBuf);
        name = new String(strBuf, charset).split("\0")[0];
        strBuf = new byte[16];
        buffer.get(strBuf);
        format = new String(strBuf, charset).split("\0")[0];
        strBuf = new byte[64];
        buffer.get(strBuf);
        fields = new String(strBuf, charset).split("\0")[0].split(",");
        if (fields.length != format.length())
            throw new RuntimeException(String.format("Labels count != format length: fields = \"%s\", format = \"%s\"",
                    Arrays.asList(fields), format));
        for (int i = 0; i < fields.length; i++) {
            fieldsMap.put(fields[i], i);
        }
    }

    public PX4LogMessage parseMessage(ByteBuffer buffer) {
        List<Object> data = new ArrayList<Object>(format.length());
        for (char f : format.toCharArray()) {
            if (f == 'b') {
                data.add((int) buffer.get());
            } else if (f == 'B') {
                data.add(buffer.get() & 0xFF);
            } else if (f == 'h') {
                data.add((int) buffer.getShort());
            } else if (f == 'H') {
                data.add(buffer.getShort() & 0xFFFF);
            } else if (f == 'i') {
                data.add(buffer.getInt());
            } else if (f == 'I') {
                data.add(buffer.getInt() & 0xFFFFFFFFl);
            } else if (f == 'f') {
                data.add(buffer.getFloat());
            } else if (f == 'q' || f == 'Q') {
                data.add(buffer.getLong());
            } else if (f == 'L') {
                data.add(buffer.getInt() * 0.0000001);
            } else {
                throw new RuntimeException("Invalid format char in message " + name + ": " + f);
            }
        }
        return new PX4LogMessage(this, data);
    }

    public List<String> getFields() {
        return Arrays.asList(fields);
    }

    public int getFieldIdx(String field) {
        return fieldsMap.get(field);
    }

    @Override
    public String toString() {
        return String.format("PX4LogMessageDescription: type=%s, length=%s, name=%s, format=%s, fields=%s", type,
                length, name, format, Arrays.asList(fields));
    }
}
