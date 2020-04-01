package eu.arrowhead.core.plantdescriptionengine;

import java.io.IOException;
import java.io.OutputStream;

import se.arkalix.dto.binary.BinaryWriter;

class DtoWriter implements BinaryWriter {

    private OutputStream writer;

    public DtoWriter(OutputStream writer) {
        this.writer = writer;
    }

    public int writeOffset() {
        // TODO: Implement
        return 0;
    }

    public void writeOffset(final int offset) {
        // TODO: Implement
    }

    public void write(final byte b) {
        try {
            writer.write(b);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void write(final byte[] bytes) {
        try {
            writer.write(bytes);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}