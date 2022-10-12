package org.fifthgen.messages;

import lombok.*;

@Data
@RequiredArgsConstructor
public class Response {

    private final int id;
    private final String message;
}
