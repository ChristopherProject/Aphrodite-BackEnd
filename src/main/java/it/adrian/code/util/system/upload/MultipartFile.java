package it.adrian.code.util.system.upload;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class MultipartFile {
    private String contentType;
    private InputStream inputStream;

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public InputStream getInputStream() {
        return inputStream;
    }

    public void setInputStream(BufferedReader reader) throws IOException {
        this.inputStream = new ReaderInputStream(reader);
    }
}
