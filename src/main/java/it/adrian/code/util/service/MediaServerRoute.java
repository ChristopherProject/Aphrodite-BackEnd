package it.adrian.code.util.service;

import it.adrian.code.Main;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;


/***
 * @author AdrianCode
 * @date 13.01.23
 */
public class MediaServerRoute {

    private static final String BASE_DIRECTORY = Main.getConfiguration().getBoolean("use_default_path") ? System.getProperty("user.dir") + File.separator + "medias" : Main.getConfiguration().getString("media_data_directory_path");  // http://localhost/example_dir/example_file.png

    public static void init(String serverName, int port){
        final File path = new File(BASE_DIRECTORY);
        if(!path.exists())path.mkdirs();
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("«" +  serverName + "» service working on port " + port);
            System.out.println("«" +  serverName + "» server init done. the service started on port 419");
            while (true) {
                Socket clientSocket = serverSocket.accept();
                handleRequest(clientSocket);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void handleRequest(Socket clientSocket) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             OutputStream out = clientSocket.getOutputStream()) {
            String requestLine = reader.readLine();
            if (requestLine != null && requestLine.startsWith("GET")) handlerResponse(requestLine, out);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void handlerResponse(String requestLine, OutputStream out) throws IOException {
        String[] requestParts = requestLine.split(" ");
        String filePath = requestParts[1].substring(1);

        File file = new File(BASE_DIRECTORY, filePath);
        if (file.exists() && filePath.endsWith(".png")) {
            byte[] fileContent = Files.readAllBytes(file.toPath());
            String responseHeader = "HTTP/1.1 200 OK\r\n" +
                    "Content-Length: " + fileContent.length + "\r\n" +
                    "Content-Type: image/png\r\n" +
                    "\r\n";
            out.write(responseHeader.getBytes());
            out.write(fileContent);
            out.flush();
        }
        if (file.exists() && (filePath.endsWith(".mp3") || filePath.endsWith(".m4a"))) {
            byte[] fileContent = Files.readAllBytes(file.toPath());
            String contentType = filePath.endsWith(".mp3") ? "audio/mpeg" : "audio/mp4";
            String responseHeader = "HTTP/1.1 200 OK\r\n" +
                    "Content-Length: " + fileContent.length + "\r\n" +
                    "Content-Type: " + contentType + "\r\n" +
                    "\r\n";
            out.write(responseHeader.getBytes());
            out.write(fileContent);
            out.flush();
        }
        else if (file.exists() && filePath.endsWith(".mp4")) {
            byte[] fileContent = Files.readAllBytes(file.toPath());
            String responseHeader = "HTTP/1.1 200 OK\r\n" +
                    "Content-Length: " + fileContent.length + "\r\n" +
                    "Content-Type: video/mp4\r\n" +
                    "\r\n";
            out.write(responseHeader.getBytes());
            out.write(fileContent);
            out.flush();
        }
        if (file.exists() && filePath.endsWith(".jpg")) {
            byte[] fileContent = Files.readAllBytes(file.toPath());
            String responseHeader = "HTTP/1.1 200 OK\r\n" +
                    "Content-Length: " + fileContent.length + "\r\n" +
                    "Content-Type: image/jpg\r\n" +
                    "\r\n";
            out.write(responseHeader.getBytes());
            out.write(fileContent);
            out.flush();
        } else {
            String response = "HTTP/1.1 200 OK\r\n" + "Content-Type: text/html\r\n" + "\r\n" + "<html><body><h1></h1></body></html>";
            out.write(response.getBytes());
            out.flush();
        }
    }
}