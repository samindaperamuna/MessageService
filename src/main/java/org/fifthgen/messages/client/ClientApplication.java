package org.fifthgen.messages.client;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class ClientApplication {

    private static final String HOST = "localhost";
    private static final int PORT = 1212;


    public static void main(String[] args) {
        try (Socket socket = new Socket(HOST, PORT)) {
            if (socket.isConnected()) {
                System.out.println("Connected to server on " + HOST + ":" + PORT);
                System.out.println("Use Ctrl + z to close the connection");
            }

            try (var out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
                 Scanner scanner = new Scanner(System.in)) {

                while (scanner.hasNextLine()) {
                    new Thread(() -> {
                        try (var in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
                            String line;
                            while ((line = in.readLine()) != null) {
                                System.out.println(line);
                            }
                        } catch (IOException e) {
                            System.out.println("Error reading socket " + e.getLocalizedMessage());
                        }
                    }).start();

                    String line = scanner.nextLine();
                    out.write(line);
                    out.flush();
                }
            } catch (IOException e) {
                System.out.println("Error during transmission " + e.getLocalizedMessage());
            }

            System.out.println("Disconnected from server");
        } catch (IOException e) {
            System.out.println("Couldn't create socket on" + HOST + ":" + PORT);
        }
    }
}
