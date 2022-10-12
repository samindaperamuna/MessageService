package org.fifthgen.messages.server;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Setter;
import org.fifthgen.messages.Response;

import java.io.*;
import java.net.Socket;

@Data
public class ServerClient implements Runnable {
    private int id;
    private String name;
    private Socket socket;

    private ClientCallback callback;

    @Setter(AccessLevel.NONE)
    private boolean sendMsgReady;

    private String msg;

    public ServerClient(int id, String name, Socket socket) {
        this.id = id;
        this.name = name;
        this.socket = socket;
    }

    @Override
    public String toString() {
        return "Id: " + id + ", Address" + socket.getInetAddress().getHostAddress();
    }

    public void sendMessage(String msg) {
        this.msg = msg;
        this.sendMsgReady = true;
    }

    @Override
    public void run() {
        System.out.println("Client " + name + ", connected");

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()))) {

            // Client's output message loop
            new Thread(() -> {
                while (!socket.isClosed()) {
                    if (sendMsgReady) {
                        try {
                            writer.write(msg);
                            writer.newLine();
                            writer.flush();
                        } catch (IOException e) {
                            System.out.println("Can't write to client:" + e.getLocalizedMessage());
                        }

                        sendMsgReady = false;
                        msg = "";
                    }
                }
            }).start();

            while (!socket.isClosed()) {
                String msg = reader.readLine();

                if (msg != null && !msg.isBlank()) {
                    System.out.println(name + ": " + msg);

                    // send msg in the  callback
                    Response response = new Response(id, msg);
                    this.callback.getClientResponse(response);
                }
            }
        } catch (IOException e) {
            System.out.println("Error reading client socket: " + e.getLocalizedMessage());
        }
    }
}
