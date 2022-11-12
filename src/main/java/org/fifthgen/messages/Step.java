package org.fifthgen.messages;

import lombok.*;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class Step {

    // Node ID to track node with
    private int id;
    private Instant startedAt;

    // Execution duration in minutes
    private int duration;
    private String message;
    private Response response;

    public Step(int duration, String message) {
        this.duration = duration;
        this.message = message;
    }

    /**
     * Mock the final response (for testing purposes only).
     *
     * @param response Response object to be set into final response.
     */
    public void mockResponse(Response response) {
        this.response = response;
    }
}
