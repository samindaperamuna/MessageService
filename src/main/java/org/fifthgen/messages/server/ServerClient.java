package org.fifthgen.messages.server;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

@Data
@AllArgsConstructor
public class ServerClient implements Runnable {
    private int id;
    private String name;
    private Socket socket;

    @Override
    public String toString() {
        return "Id: " + id + ", Address" + socket.getInetAddress().getHostAddress();
    }

    @Override
    public void run() {
        System.out.println("Client " + name + ", connected");

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            while (!socket.isClosed()) {
                String response = reader.readLine();

                if (response != null && !response.isBlank()) {
                    System.out.println(name + ": " + response);
                }
            }
        } catch (IOException e) {
            System.out.println("Error reading client socket: " + e.getLocalizedMessage());
        }
    }
}
