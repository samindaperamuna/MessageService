package org.fifthgen.messages.server;

import org.fifthgen.messages.Response;

public interface ClientCallback {

    void getClientResponse(Response response);
}
