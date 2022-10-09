package org.fifthgen.messages;

import lombok.*;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class Node {

    // Node ID to track node with
    private int id;
    private Date startTime;

    // delay time unit is minutes
    private int delay;
    private String message;
    private Response response;

    public Node(int delay, String message) {
        this.delay = delay;
        this.message = message;
    }

    /**
     * Mock the final response (for testing purposes only).
     * @param response Response object to be set into final response.
     */
    public void mockResponse(Response response) {
        this.response = response;
    }
}
