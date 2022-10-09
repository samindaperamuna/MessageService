package org.fifthgen.messages.server;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.net.InetSocketAddress;

@Data
@AllArgsConstructor
public class ServerClient {
    private int id;
    private InetSocketAddress address;

    @Override
    public String toString() {
        return "Id: " + id + ", Address" + address.getHostString();
    }
}
