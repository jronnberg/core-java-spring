package eu.arrowhead.core.plantdescriptionengine.services.management.dto;

import java.io.IOException;
import java.io.OutputStream;

import se.arkalix.dto.binary.BinaryWriter;

public class DtoWriter implements BinaryWriter {

    private OutputStream writer;

    public DtoWriter(OutputStream writer) {
        this.writer = writer;
    }

    public int writeOffset() {
        throw new UnsupportedOperationException();
    }

    public void writeOffset(final int offset) {
        throw new UnsupportedOperationException();
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