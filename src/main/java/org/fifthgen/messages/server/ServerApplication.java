package org.fifthgen.messages.server;

import java.io.IOException;
import java.net.ServerSocket;

public class ServerApplication {

    private static final int PORT = 1212;

    public static void main(String[] args) {
        try(ServerSocket serverSocket = new ServerSocket(PORT)) {
            // serverSocket.get
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
