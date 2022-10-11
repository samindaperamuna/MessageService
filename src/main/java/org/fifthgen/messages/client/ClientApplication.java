package org.fifthgen.messages.client;

import lombok.Data;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;

@Data
public class ClientApplication {

    private int serverPort;
    private String serverHost;

    public ClientApplication(String host, int port) {

        this.serverHost = host;
        this.serverPort = port;
    }

    public void startClient() {
        try (Socket socket = new Socket(serverHost, serverPort)) {
            new Thread(() -> listenForMessages(socket)).start();

            try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
                 Scanner sc = new Scanner(System.in)) {

                // Get the client name from the user.
                System.out.println("Connected to server at " + serverHost + ":" + serverPort);
                System.out.print("Please provide a name: ");

                // Send the client name to the server.
                String clientName = sc.nextLine();
                writeToServer(writer, clientName);

                while (!socket.isClosed()) {
                    String line = sc.nextLine();

                    writeToServer(writer, line);
                }
            } catch (IOException e) {
                System.out.println("Error reading socket output stream: " + e.getLocalizedMessage());
            }
        } catch (IOException e) {
            System.out.println("Error creating socket: " + e.getLocalizedMessage());
        }
    }

    private void writeToServer(BufferedWriter writer, String msg) throws IOException {
        if (msg != null && !msg.isBlank()) {
            writer.write(msg);
            writer.newLine();
            writer.flush();
        }
    }

    private void listenForMessages(Socket socket) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            while (!socket.isClosed()) {
                String response = reader.readLine();

                if (response != null && !response.isEmpty()) {
                    System.out.println(response);
                }
            }
        } catch (IOException e) {
            System.out.println("Error reading client socket: " + e.getLocalizedMessage());
        }
    }

    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: client <host_name> <port>");
        } else {
            String host = args[0];
            String port = args[1];

            new ClientApplication(host, Integer.parseInt(port)).startClient();
        }
    }
}
