package org.example.domain;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.net.Socket;

public class ATMClient implements AutoCloseable {
    private Socket socket;
    private BufferedReader reader;
    private BufferedWriter writer;

    public ATMClient(String serverAddress, int port) throws IOException {
        socket = new Socket(serverAddress, port);
        reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
    }

    public JSONObject sendRequest(JSONObject request) throws IOException, JSONException {
        // Send request to server
        writer.write(request.toString());
        writer.newLine();
        writer.flush();

        // Read response from server
        String responseLine = reader.readLine();

        // Check if responseLine is not null
        if (responseLine == null) {
            throw new IOException("Server closed connection unexpectedly");
        }

        return new JSONObject(responseLine);
    }

    @Override
    public void close() throws IOException {
        if (reader != null) {
            reader.close();
        }
        if (writer != null) {
            writer.close();
        }
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
    }
}
