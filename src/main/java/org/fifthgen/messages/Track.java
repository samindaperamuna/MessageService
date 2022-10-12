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

    private final int clientRef;
    private final List<Node> nodes;
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

    public Track(int clientRef, ServerApplication server, TrackResponseValidator validator,
                 TrackResponseCallback responseCallback) {

        this.id = UUID.randomUUID().toString();
        this.clientRef = clientRef;
        this.log = Logger.getLogger(this.id);
        this.nodes = new ArrayList<>();
        this.executorService = Executors.newFixedThreadPool(10);
        this.validator = validator;
        this.server = server;

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
        if (!nodes.isEmpty()) {
            Node node = nodes.get(0);
            nodes.remove(node);
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
        this.nodes.add(node);
    }

    /**
     * Shutdown the executor and cancel all the non executing tasks.
     */
    public void shutdown() {
        log.info("Track: " + id + " execution complete" + System.lineSeparator());

        executorService.shutdown();

        // Start the next track if available
        if (this.next != null) {
            this.next.run();
        }
    }

    /**
     * Execute the instructions in the node. Needs to be run on a separate thread.
     *
     * @param node {@link Node} object to be executed
     */
    private void execute(Node node) {
        node.setStartedAt(Instant.now());

        for (ServerClient client : server.getClients()) {
            if (client.getId() == this.clientRef) {
                // Register instance with the client for callback
                client.setCallback(this);
                log.info("Sending message to client: " + client.getName());
                client.sendMessage(node.getMessage());

                break;
            }
        }

        // Time elapsed in seconds, node duration is in minutes
        long timeElapsed = Duration.between(node.getStartedAt(), Instant.now()).toSeconds();

        // Block the thread till client response is received or time expires;
        while (trackResponse == null && (timeElapsed / 60) < nodeDuration) {
            // Recalculated time elapsed every 5 seconds
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                System.out.println("Thread interrupted: " + e.getLocalizedMessage());
            }

            timeElapsed = Duration.between(node.getStartedAt(), Instant.now()).toSeconds();
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
        this.trackResponse = response;
    }
}
