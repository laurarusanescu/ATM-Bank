package org.example.domain;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.net.Socket;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ClientHandler implements Runnable {  // Implement Runnable
    private Socket socket;
    private Connection dbConnection;
    private BufferedReader reader;
    private BufferedWriter writer;

    public ClientHandler(Socket socket, Connection dbConnection) throws IOException {
        this.socket = socket;
        this.dbConnection = dbConnection;
        this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
    }

    @Override
    public void run() {  // Override the run method
        try {
            String requestLine = reader.readLine();
            JSONObject request = new JSONObject(requestLine);

            JSONObject response;
            switch (request.getString("type")) {
                case "VERIFY_REQUEST":
                    response = handleVerifyRequest(request);
                    break;
                case "BALANCE_REQUEST":
                    response = handleBalanceRequest(request);
                    break;
                case "WITHDRAWAL_REQUEST":
                    response = handleWithdrawalRequest(request);
                    break;
                default:
                    response = new JSONObject().put("status", "failure").put("error_code", "unknown_request");
            }

            writer.write(response.toString());
            writer.newLine();
            writer.flush();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private JSONObject handleVerifyRequest(JSONObject request) throws SQLException, JSONException {
        String cardId = request.getString("card_id");
        String password = request.getString("password");

        String sql = "SELECT * FROM accounts WHERE account_id = ? AND password = ?";
        PreparedStatement stmt = dbConnection.prepareStatement(sql);
        stmt.setString(1, cardId);
        stmt.setString(2, password);
        ResultSet rs = stmt.executeQuery();

        if (rs.next()) {
            return new JSONObject().put("status", "success");
        } else {
            return new JSONObject().put("status", "failure").put("error_code", "invalid_credentials");
        }
    }

    private JSONObject handleBalanceRequest(JSONObject request) throws SQLException, JSONException {
        String accountId = request.getString("account_id");

        String sql = "SELECT balance FROM accounts WHERE account_id = ?";
        PreparedStatement stmt = dbConnection.prepareStatement(sql);
        stmt.setString(1, accountId);
        ResultSet rs = stmt.executeQuery();

        if (rs.next()) {
            return new JSONObject().put("status", "success").put("balance", rs.getDouble("balance"));
        } else {
            return new JSONObject().put("status", "failure").put("error_code", "account_not_found");
        }
    }

    private JSONObject handleWithdrawalRequest(JSONObject request) throws SQLException, JSONException {
        String accountId = request.getString("account_id");
        double amount = request.getDouble("amount");

        String sql = "SELECT balance FROM accounts WHERE account_id = ?";
        PreparedStatement stmt = dbConnection.prepareStatement(sql);
        stmt.setString(1, accountId);
        ResultSet rs = stmt.executeQuery();

        if (rs.next()) {
            double balance = rs.getDouble("balance");
            if (balance >= amount) {
                // Update balance
                String updateSql = "UPDATE accounts SET balance = balance - ? WHERE account_id = ?";
                PreparedStatement updateStmt = dbConnection.prepareStatement(updateSql);
                updateStmt.setDouble(1, amount);
                updateStmt.setString(2, accountId);
                updateStmt.executeUpdate();

                return new JSONObject().put("status", "success").put("new_balance", balance - amount);
            } else {
                return new JSONObject().put("status", "failure").put("error_code", "insufficient_funds");
            }
        } else {
            return new JSONObject().put("status", "failure").put("error_code", "account_not_found");
        }
    }
}
