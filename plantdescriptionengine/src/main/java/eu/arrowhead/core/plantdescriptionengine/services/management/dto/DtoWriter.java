package eu.arrowhead.core.plantdescriptionengine.services.management.dto;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Objects;

import se.arkalix.dto.binary.BinaryWriter;

public class DtoWriter implements BinaryWriter {

    private final OutputStream writer;

    public DtoWriter(OutputStream writer) {
        Objects.requireNonNull(writer, "Expected OutputStream");
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

    @Override
    public int writableBytes() {
        throw new UnsupportedOperationException();
    }
}