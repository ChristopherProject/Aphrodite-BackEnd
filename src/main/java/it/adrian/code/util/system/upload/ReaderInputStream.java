package it.adrian.code.util.system.upload;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;

public class ReaderInputStream extends InputStream {
    private BufferedReader reader;

    public ReaderInputStream(BufferedReader reader) {
        this.reader = reader;
    }

    @Override
    public int read() throws IOException {
        return reader.read(); // Legge un carattere alla volta
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        char[] buffer = new char[len];
        int bytesRead = reader.read(buffer, 0, len);

        if (bytesRead == -1) {
            return -1; // Fine del flusso
        }

        for (int i = 0; i < bytesRead; i++) {
            b[off + i] = (byte) buffer[i]; // Converti char in byte
        }

        return bytesRead;
    }
}
