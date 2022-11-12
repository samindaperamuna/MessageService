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

    private final Logger log = Logger.getLogger(this.getClass().getName());
    private final List<Step> steps = new ArrayList<>();
    private final ExecutorService executorService = Executors.newFixedThreadPool(10);
    private final int id;
    private int clientRef;
    private final ServerApplication server;
    private int nodeDuration;
    private boolean isDone;
    private Response trackResponse;
    private Response prevTrackResponse;
    private int nodeCounter = 0;
    private Track next;
    private TrackResponseValidator validator;
    private TrackResponseCallback responseCallback;
    private boolean dependsOnPrevTrack;

    public int getId() {
        return id;
    }

    public int getClientRef() {
        return clientRef;
    }

    public void setClientRef(int clientRef) {
        this.clientRef = clientRef;
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

    public void setValidator(TrackResponseValidator validator) {
        this.validator = validator;
    }

    public TrackResponseCallback getResponseCallback() {
        return responseCallback;
    }

    public void setResponseCallback(TrackResponseCallback responseCallback) {
        this.responseCallback = responseCallback;
    }

    protected void setPrevTrackResponse(Response response) {
        this.prevTrackResponse = response;
    }

    public boolean getDependsOnPrevTrack() {
        return dependsOnPrevTrack;
    }

    public void setDependsOnPrevTrack(boolean dependsOnPrevTrack) {
        this.dependsOnPrevTrack = dependsOnPrevTrack;
    }

    public Track(int id, ServerApplication server) {
        this.id = id;
        this.server = server;
    }

    /**
     * Start executing the nodes one by one after the set delay.
     */
    public void run() {
        log.info("Executing track: " + id + System.lineSeparator());

        if (dependsOnPrevTrack) {
            if (this.responseCallback instanceof TrackResponseCallBackWithOnStart) {
                TrackResponseCallBackWithOnStart callback = (TrackResponseCallBackWithOnStart) this.responseCallback;
                callback.onStart(prevTrackResponse, steps);
            }
        }

        executeNextNode();
    }

    private void executeNextNode() {
        if (!steps.isEmpty()) {
            Step step = steps.get(0);
            steps.remove(step);
            nodeDuration = step.getDuration();

            executorService.execute(() -> execute(step));
        }
    }

    /**
     * Add a node to the track to be processed. This method is useful for manipulating the node in the context
     * of the track before it's being processed. Getters and setters to this field is disabled by design to allow
     * insertions of nodes only through this method.
     *
     * @param step Node to be added to the internal stack
     */
    public void addNode(Step step) {
        step.setId(++nodeCounter);
        this.steps.add(step);
    }

    /**
     * Shutdown the executor and cancel all the non executing tasks.
     */
    public void shutdown() {
        log.info("Track: " + id + " execution complete" + System.lineSeparator());

        executorService.shutdown();

        // Start the next track if available
        if (this.next != null) {
            this.next.setPrevTrackResponse(trackResponse);
            this.next.run();
        }
    }

    /**
     * Execute the instructions in the node. Needs to be run on a separate thread.
     *
     * @param step {@link Step} object to be executed
     */
    private void execute(Step step) {
        step.setStartedAt(Instant.now());

        for (ServerClient client : server.getClients()) {
            if (client.getId() == this.clientRef) {
                // Register instance with the client for callback
                client.setCallback(this);
                log.info("Sending message to client: " + client.getName());
                client.sendMessage(step.getMessage());

                break;
            }
        }

        // Time elapsed in seconds, node duration is in minutes
        long timeElapsed = Duration.between(step.getStartedAt(), Instant.now()).toSeconds();

        // Block the thread till client response is received or time expires;
        while (trackResponse == null && (timeElapsed / 60) < nodeDuration) {
            // Recalculated time elapsed every 5 seconds
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                System.out.println("Thread interrupted: " + e.getLocalizedMessage());
            }

            timeElapsed = Duration.between(step.getStartedAt(), Instant.now()).toSeconds();
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
