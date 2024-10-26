package org.example.domain;

import java.io.*;
import java.net.*;
import java.sql.*;

public class BankServer {
    private ServerSocket serverSocket;
    private Connection dbConnection;

    public BankServer(int port, String dbUrl) throws IOException, SQLException {
        serverSocket = new ServerSocket(port);
        dbConnection = DriverManager.getConnection(dbUrl);
        System.out.println("BankServer started and listening on port " + port);
    }

    public void startServer() {
        while (true) {
            try {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Client connected: " + clientSocket.getInetAddress());
                // Create a new ClientHandler for each client connection
                new Thread(new ClientHandler(clientSocket, dbConnection)).start();
            } catch (IOException e) {
                System.err.println("Error accepting client connection: " + e.getMessage());
            }
        }
    }

    public void close() throws IOException, SQLException {
        if (serverSocket != null) {
            serverSocket.close();
        }
        if (dbConnection != null) {
            dbConnection.close();
        }
    }
}
