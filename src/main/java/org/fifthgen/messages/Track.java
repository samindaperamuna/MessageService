package org.fifthgen.messages;

import org.fifthgen.messages.server.ClientCallback;
import org.fifthgen.messages.server.ServerApplication;
import org.fifthgen.messages.server.ServerClient;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Logger;

public class Track implements Runnable, ClientCallback {

    private final Logger log;
    private final ServerApplication server;
    private final String id;
    private final Stack<Node> nodes;
    private final ExecutorService executorService;
    private int nodeDuration;
    private boolean isDone;
    private Response trackResponse;
    private int nodeCounter = 0;

    public String getId() {
        return id;
    }

    public boolean isDone() {
        return isDone;
    }

    public Response getTrackResponse() {
        return trackResponse;
    }

    public Track getNext() {
        return next;
    }

    public void setNext(Track next) {
        this.next = next;
    }

    public TrackResponseValidator getValidator() {
        return validator;
    }

    private Track next;

    private final TrackResponseValidator validator;
    private final TrackResponseCallback responseCallback;

    public Track(ServerApplication server, TrackResponseValidator validator, TrackResponseCallback responseCallback) {
        this.id = UUID.randomUUID().toString();
        this.log = Logger.getLogger(this.id);
        this.nodes = new Stack<>();
        this.executorService = Executors.newFixedThreadPool(10);
        this.validator = validator;
        this.server = server;

        // Set the server's client callback to this instance
        // Responses from client nodes are received through the callback
        this.server.setClientCallback(this);
        this.responseCallback = responseCallback;
    }

    /**
     * Start executing the nodes one by one after the set delay.
     */
    public void run() {
        log.info("Executing track: " + id + System.lineSeparator());

        executeNextNode();
    }

    private void executeNextNode() {
        if (!nodes.empty()) {
            Node node = nodes.pop();
            nodeDuration = node.getDuration();

            executorService.execute(() -> execute(node));
        }
    }

    /**
     * Add a node to the track to be processed. This method is useful for manipulating the node in the context
     * of the track before it's being processed. Getters and setters to this field is disabled by design to allow
     * insertions of nodes only through this method.
     *
     * @param node Node to be added to the internal stack
     */
    public void addNode(Node node) {
        node.setId(++nodeCounter);
        this.nodes.push(node);
    }

    /**
     * Shutdown the executor and cancel all the non executing tasks.
     */
    public void shutdown() {
        log.info("Track: " + id + " execution complete" + System.lineSeparator());

        executorService.shutdown();

        // Start the next track
        this.next.run();
    }

    /**
     * Execute the instructions in the node. Needs to be run on a separate thread.
     *
     * @param node {@link Node} object to be executed
     */
    private void execute(Node node) {
        node.setStartedAt(Instant.now());

        for (ServerClient client : server.getClients()) {
            if (client.getId() == node.getId()) {
                log.info("Sending message to client: " + client.getName());
                server.sendMsgToClient(client, node.getMessage());

                break;
            }
        }

        // Time elapsed in seconds, node duration is in minutes
        long timeElapsed = 0;

        // Block the thread till client response is received or time expires;
        while (trackResponse == null && (timeElapsed / 60) < nodeDuration) {
            // Recalculated time elapsed every 5 seconds
            if (timeElapsed % 5 == 0) {
                timeElapsed = Duration.between(Instant.now(), node.getStartedAt()).toSeconds();
            }
        }

        // Reset response and duration
        if (trackResponse == null) {
            nodeDuration = 0;
            executeNextNode();
        } else {
            if (this.responseCallback != null) {
                this.responseCallback.onSuccess(this.trackResponse);
            }

            shutdown();
        }
    }

    /**
     * Validate client response using a custom validator to ensure the completion of track
     * according to requirements.
     *
     * @param response Response retrieved from the client
     * @return Whether the response is valid by the validation criteria
     */
    private boolean validateNodeResponse(Response response) {
        if (this.validator != null) {
            return this.validator.validate(response);
        }

        return false;
    }

    @Override
    public void getClientResponse(Response response) {
        if (validateNodeResponse(response)) {
            this.trackResponse = response;
        }
    }
}
