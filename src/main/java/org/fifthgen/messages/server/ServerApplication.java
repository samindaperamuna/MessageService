package org.fifthgen.messages.server;

import lombok.Data;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

@Data
public class ServerApplication {

    private int clientCnt = 0;

    private int serverPort;

    private List<ServerClient> clients;

    public ServerApplication(int port) {
        this.serverPort = port;
        this.clients = new ArrayList<>();
    }

    public void startServer() {
        try (ServerSocket serverSocket = new ServerSocket(serverPort)) {
            System.out.println("Server started on port : " + serverPort);

            while (!serverSocket.isClosed()) {
                Socket socket = serverSocket.accept();

                // Read the initial line from client which is the client name.
                BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                String clientName = reader.readLine();

                // Create a client instance which stores information about the connected client.
                ServerClient client = new ServerClient(++clientCnt, clientName, socket);
                clients.add(client);

                // Run the server client on its own thread
                Thread clientThread = new Thread(client);
                clientThread.start();
            }
        } catch (IOException e) {
            System.out.println("Error accepting client connection: " + e.getLocalizedMessage());
        }
    }

    public void sendMsgToClient(ServerClient client, String msg) {
        Socket socket = client.getSocket();

        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()))) {
            if (!socket.isClosed()) {
                writer.write(msg);
                writer.newLine();
                writer.flush();
            } else {
                System.out.println("Can't write to client. Socket is closed.");
            }
        } catch (IOException e) {
            System.out.println("Failed to write to the client socket: " + e.getLocalizedMessage());
        }
    }

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: server <port>");
        } else {
            String port = args[0];
            new ServerApplication(Integer.parseInt(port)).startServer();
        }
    }
}
