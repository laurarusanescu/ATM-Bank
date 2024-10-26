package org.example;

import java.io.IOException;
import java.sql.SQLException;

import org.example.domain.ATMClient;
import org.example.domain.BankServer;
import org.json.JSONException;
import org.json.JSONObject;

public class Main {

    public static void main(String[] args) {
        // Start the BankServer in a separate thread
        Thread serverThread = new Thread(() -> {
            try {
                // Start the BankServer and listen on port 5000 with the SQLite database
                BankServer bankServer = new BankServer(5000, "jdbc:sqlite:data/bank.db");
                bankServer.startServer(); // Ensure the server starts listening
            } catch (IOException | SQLException e) {
                System.err.println("Error starting BankServer: " + e.getMessage());
                e.printStackTrace();
            }
        });
        serverThread.start();

        // Small delay to ensure the server has started before the client connects
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            System.err.println("Error while waiting for server to start: " + e.getMessage());
            e.printStackTrace();
        }

        // Start the ATMClient
        try (ATMClient atmClient = new ATMClient("127.0.0.1", 5000)) {
            // Step 1: Send VERIFY_REQUEST
            JSONObject verifyRequest = new JSONObject();
            verifyRequest.put("type", "VERIFY_REQUEST");
            verifyRequest.put("card_id", "123456789");
            verifyRequest.put("password", "password123");

            JSONObject verifyResponse = atmClient.sendRequest(verifyRequest);
            System.out.println("Verify Response: " + verifyResponse);

            if ("success".equals(verifyResponse.getString("status"))) {
                // Step 2: Send BALANCE_REQUEST
                JSONObject balanceRequest = new JSONObject();
                balanceRequest.put("type", "BALANCE_REQUEST");
                balanceRequest.put("account_id", "123456789");

                JSONObject balanceResponse = atmClient.sendRequest(balanceRequest);
                System.out.println("Balance Response: " + balanceResponse);

                // Step 3: Send WITHDRAWAL_REQUEST
                JSONObject withdrawalRequest = new JSONObject();
                withdrawalRequest.put("type", "WITHDRAWAL_REQUEST");
                withdrawalRequest.put("account_id", "123456789");
                withdrawalRequest.put("amount", 50);

                JSONObject withdrawalResponse = atmClient.sendRequest(withdrawalRequest);
                System.out.println("Withdrawal Response: " + withdrawalResponse);
            } else {
                System.out.println("Verification failed, cannot proceed with other requests.");
            }
        } catch (IOException | JSONException e) {
            System.err.println("Error in ATMClient: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
