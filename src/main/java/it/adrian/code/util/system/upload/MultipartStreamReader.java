package it.adrian.code.util.system.upload;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class MultipartStreamReader {
    private BufferedReader reader;
    private String boundary;

    public MultipartStreamReader(InputStream inputStream, String boundary) {
        this.reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
        this.boundary = boundary;
    }

    public MultipartFile readNextPart() throws IOException {
        String line = reader.readLine();
        while (line != null && !line.startsWith("--" + boundary)) {
            line = reader.readLine();
        }

        if (line == null) {
            return null; // Nessuna parte successiva trovata
        }

        MultipartFile file = new MultipartFile();
        line = reader.readLine(); // Salta la riga vuota dopo il separatore
        file.setContentType(reader.readLine().split(":")[1].trim()); // Leggi il tipo di contenuto
        line = reader.readLine(); // Salta la riga vuota dopo il tipo di contenuto
        file.setInputStream(reader);

        return file;
    }
}
