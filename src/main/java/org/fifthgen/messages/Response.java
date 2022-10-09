package org.fifthgen.messages;

import lombok.*;

@Data
@RequiredArgsConstructor
public class Response {

    @Setter(AccessLevel.PACKAGE)
    private int id;

    private final String message;
}
