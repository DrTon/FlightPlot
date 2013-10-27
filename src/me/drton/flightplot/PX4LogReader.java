package me.drton.flightplot;

import java.io.EOFException;
import java.io.IOException;
import java.nio.BufferUnderflowException;
import java.util.HashMap;
import java.util.Map;

/**
 * User: ton Date: 03.06.13 Time: 14:18
 */
public class PX4LogReader extends BinaryLogReader {
    private static final int HEADER_LEN = 3;
    private static final byte HEADER_HEAD1 = (byte) 0xA3;
    private static final byte HEADER_HEAD2 = (byte) 0x95;

    private long dataStart = 0;
    private Map<Integer, PX4LogMessageDescription> messageDescriptions
            = new HashMap<Integer, PX4LogMessageDescription>();
    private Map<String, String> fieldsList = null;
    private long time = 0;
    private long sizeUpdates = -1;
    private long sizeMicroseconds = -1;
    private long startMicroseconds = -1;
    private Map<String, Object> version = new HashMap<String, Object>();
    private Map<String, Object> parameters = new HashMap<String, Object>();

    public PX4LogReader(String fileName) throws IOException, FormatErrorException {
        super(fileName);
        readFormats();
        updateStatistics();
    }

    @Override
    public String getFormat() {
        return "PX4";
    }

    @Override
    public long getSizeUpdates() {
        return sizeUpdates;
    }

    @Override
    public long getStartMicroseconds() {
        return startMicroseconds;
    }

    @Override
    public long getSizeMicroseconds() {
        return sizeMicroseconds;
    }

    @Override
    public Map<String, Object> getVersion() {
        return version;
    }

    @Override
    public Map<String, Object> getParameters() {
        return parameters;
    }

    private void updateStatistics() throws IOException, FormatErrorException {
        seek(0);
        long packetsNum = 0;
        long timeStart = -1;
        long timeEnd = -1;
        while (true) {
            PX4LogMessage msg;
            try {
                msg = readMessage();
            } catch (EOFException e) {
                break;
            }
            if ("TIME".equals(msg.description.name)) {
                long t = msg.getLong(0);
                if (timeStart < 0)
                    timeStart = t;
                timeEnd = t;
                packetsNum++;
            } else if ("VER".equals(msg.description.name)) {
                version = msg.toMap();
            } else if ("PARM".equals(msg.description.name)) {
                parameters.put((String) msg.get("Name"), msg.get("Value"));
            }
        }
        startMicroseconds = timeStart;
        sizeUpdates = packetsNum;
        sizeMicroseconds = timeEnd - timeStart;
        seek(0);
    }

    @Override
    public boolean seek(long seekTime) throws IOException, FormatErrorException {
        position(dataStart);
        if (seekTime == 0) {      // Seek to start of log
            time = 0;
            return true;
        }
        try {
            while (true) {
                buffer.mark();
                int msgType = readHeaderFillBuffer();
                PX4LogMessageDescription messageDescription = messageDescriptions.get(msgType);
                if (messageDescription == null) {
                    buffer.reset();
                    throw new RuntimeException("Unknown message type: " + msgType);
                }
                int bodyLen = messageDescription.length - HEADER_LEN;
                if (buffer.remaining() < bodyLen) {
                    buffer.reset();
                    fillBuffer();
                    continue;
                }
                if ("TIME".equals(messageDescription.name)) {
                    PX4LogMessage msg = messageDescription.parseMessage(buffer);
                    long t = msg.getLong(0);
                    if (t > seekTime) {
                        // Time found
                        time = t;
                        buffer.reset();
                        return true;
                    }
                } else {
                    // Skip the message
                    buffer.position(buffer.position() + bodyLen);
                }
            }
        } catch (EOFException e) {
            return false;
        }
    }

    @Override
    public long readUpdate(Map<String, Object> update) throws IOException, FormatErrorException {
        long t = time;
        while (true) {
            PX4LogMessage msg = readMessage();
            if ("TIME".equals(msg.description.name)) {
                time = msg.getLong(0);
                if (t == 0) {
                    t = time;
                    continue;
                } else
                    break;
            }
            String[] fields = msg.description.fields;
            for (int i = 0; i < fields.length; i++) {
                String field = fields[i];
                update.put(msg.description.name + "." + field, msg.get(i));
            }
        }
        return t;
    }

    @Override
    public Map<String, String> getFields() {
        return fieldsList;
    }

    private void readFormats() throws IOException, FormatErrorException {
        fieldsList = new HashMap<String, String>();
        while (true) {
            if (fillBuffer() < 0)
                break;
            while (true) {
                if (buffer.remaining() < PX4LogMessageDescription.FORMAT.length)
                    break;
                buffer.mark();
                int msgType = readHeader();     // Don't try to handle errors in formats
                if (msgType == PX4LogMessageDescription.FORMAT.type) {
                    // Message description
                    PX4LogMessageDescription msgDescr = new PX4LogMessageDescription(buffer);
                    messageDescriptions.put(msgDescr.type, msgDescr);
                    for (int i = 0; i < msgDescr.fields.length; i++) {
                        String field = msgDescr.fields[i];
                        fieldsList.put(msgDescr.name + "." + field, Character.toString(msgDescr.format.charAt(i)));
                    }
                } else {
                    // Data message, all formats are read
                    buffer.reset();
                    dataStart = position();
                    return;
                }
            }
        }
    }

    private int readHeader() throws IOException, FormatErrorException {
        if (buffer.get() != HEADER_HEAD1 || buffer.get() != HEADER_HEAD2)
            throw new FormatErrorException(String.format("Invalid header at %s (0x%X)", position(), position()));
        return buffer.get() & 0xFF;
    }

    private int readHeaderFillBuffer() throws IOException {
        while (true) {
            if (buffer.remaining() < HEADER_LEN) {
                if (fillBuffer() == 0)
                    throw new BufferUnderflowException();
                continue;
            }
            int p = buffer.position();
            try {
                return readHeader();
            } catch (FormatErrorException e) {
                buffer.position(p + 1);
            }
        }
    }

    /**
     * Read next message from log
     *
     * @return log message
     * @throws IOException  on IO error
     * @throws EOFException on end of stream
     */
    public PX4LogMessage readMessage() throws IOException, FormatErrorException {
        PX4LogMessage message;
        int msgType = readHeaderFillBuffer();
        PX4LogMessageDescription messageDescription = messageDescriptions.get(msgType);
        if (messageDescription == null) {
            throw new FormatErrorException("Unknown message type: " + msgType);
        }
        if (buffer.remaining() < messageDescription.length - HEADER_LEN) {
            fillBuffer();
            if (buffer.remaining() < messageDescription.length - HEADER_LEN)
                throw new FormatErrorException("Unexpected end of file");
        }
        return messageDescription.parseMessage(buffer);
    }

    public static void main(String[] args) throws Exception {
        PX4LogReader reader = new PX4LogReader("test.bin");
        long tStart = System.currentTimeMillis();
        while (true) {
            try {
                PX4LogMessage msg = reader.readMessage();
            } catch (EOFException e) {
                break;
            }
        }
        long tEnd = System.currentTimeMillis();
        System.out.println(tEnd - tStart);
        reader.close();
    }
}
