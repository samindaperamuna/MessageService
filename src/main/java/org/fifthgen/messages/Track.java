package org.fifthgen.messages;

import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Logger;

@Data
public class Track implements Runnable {

    private Logger log;

    @Setter(AccessLevel.NONE)
    private String id;

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private final Stack<Node> nodes;

    @Setter(AccessLevel.NONE)
    private final List<Future<Response>> responses;

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private final ScheduledExecutorService executorService;

    @Setter(AccessLevel.NONE)
    private Date startTime;

    @Setter(AccessLevel.NONE)
    private boolean isDone;

    @Setter(AccessLevel.NONE)
    private Response response;

    @Setter(AccessLevel.NONE)
    private int nodeCounter = 0;

    private Track next;

    public Track() {
        this.id = UUID.randomUUID().toString();
        this.log = Logger.getLogger(this.id);
        this.nodes = new Stack<>();
        this.responses = new ArrayList<>();
        this.executorService = Executors.newScheduledThreadPool(10);
    }

    /**
     * Start executing the nodes one by one after the set delay.
     */
    public void run() {
        boolean isReadingResponses = false;
        long delay = 0;

        log.info("Executing track: " + id + System.lineSeparator());

        while (!nodes.empty()) {
            Node node = nodes.pop();
            delay += node.getDelay();

            // Submit the node and add the response to the list to be called later
            responses.add(executorService.schedule(() -> execute(node), delay, TimeUnit.MINUTES));

            if (!isReadingResponses) {
                executorService.execute(this::processResponses);
                isReadingResponses = true;
            }
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

        // Set mock response ID
        if (node.getResponse() != null) {
            node.getResponse().setId(node.getId());
        }

        this.nodes.push(node);
    }

    /**
     * Shutdown the executor and cancel all the non executing tasks.
     */
    public void shutdown() {
        log.info("Track: " + id + " execution complete" + System.lineSeparator());

        // Cancel all the scheduled tasks
        for (Future<Response> responseFuture : this.responses) {
            responseFuture.cancel(true);
        }

        executorService.shutdown();

        // Start the next track
        this.next.run();
    }

    /**
     * Execute the instructions in the node. Needs to be run on a separate thread.
     *
     * @param node {@link Node} object to be executed
     * @return Response
     */
    private Response execute(Node node) {
        node.setStartTime(new Date());

        // Simulating sending message to the user
        log.info(node.getMessage() + System.lineSeparator());

        // Simulate response delay of 5 seconds
        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            log.severe("Thread interrupted for node : " + node.getId());
        }

        return node.getResponse();
    }

    /**
     * Process the list of responses; loop through all the futures till they are completed.
     * Needs to be run on a separate thread because of the blocking functionality.
     */
    private void processResponses() {
        String message = "";

        while (message.isBlank()) {
            for (int i = 0; i < responses.size(); i++) {
                var responseFuture = responses.get(i);

                if (responseFuture.isDone()) {
                    try {
                        var result = responseFuture.get();

                        if (result != null && !result.getMessage().isBlank()) {
                            response = result;
                            message = result.getMessage();
                        }
                    } catch (InterruptedException | ExecutionException e) {
                        log.severe("An error occurred while processing node");
                    }

                    responses.remove(responseFuture);
                }
            }
        }

        // Print message to the screen
        log.info(message + System.lineSeparator());

        // End of while loop means a response has been received
        shutdown();
    }
}
