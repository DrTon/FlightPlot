package me.drton.flightplot;

import java.io.EOFException;
import java.io.IOException;
import java.nio.BufferUnderflowException;
import java.util.*;

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
    private List<String> fieldsList = null;
    private long time = 0;

    public PX4LogReader(String fileName) throws IOException, FormatErrorException {
        super(fileName);
        messageDescriptions.clear();
        readFormats();
    }

    @Override
    public boolean seek(long time) throws IOException, FormatErrorException {
        position(dataStart);
        if (time == 0)      // Seek to start of log
            return true;
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
                    if (msg.getLong(0) > time) {
                        // Time found
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
    public Collection<String> getFields() {
        return fieldsList;
    }

    private void readFormats() throws IOException, FormatErrorException {
        fieldsList = new ArrayList<String>();
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
                    for (String field : msgDescr.getFields()) {
                        fieldsList.add(msgDescr.name + "." + field);
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

    private int readHeaderFillBuffer() throws IOException, FormatErrorException {
        while (true) {
            if (buffer.remaining() < HEADER_LEN) {
                if (fillBuffer() == 0)
                    throw new BufferUnderflowException();
                continue;
            }
            return readHeader();
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
        while (true) {
            buffer.mark();
            int msgType = readHeaderFillBuffer();
            PX4LogMessageDescription messageDescription = messageDescriptions.get(msgType);
            if (messageDescription == null) {
                buffer.reset();
                throw new RuntimeException("Unknown message type: " + msgType);
            }
            if (buffer.remaining() < messageDescription.length - HEADER_LEN) {
                buffer.reset();
                fillBuffer();
                continue;
            }
            message = messageDescription.parseMessage(buffer);
            break;
        }
        return message;
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
