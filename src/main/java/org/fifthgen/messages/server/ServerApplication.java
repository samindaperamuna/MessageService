package org.fifthgen.messages.server;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class ServerApplication {

    private int clientCnt = 0;

    private final int serverPort;

    private final List<ServerClient> clients;

    public int getServerPort() {
        return serverPort;
    }

    public List<ServerClient> getClients() {
        return clients;
    }

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

//    public static void main(String[] args) {
//        if (args.length < 1) {
//            System.out.println("Usage: server <port>");
//        } else {
//            String port = args[0];
//            new ServerApplication(Integer.parseInt(port)).startServer();
//        }
//    }
}
