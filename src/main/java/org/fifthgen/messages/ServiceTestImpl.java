package org.fifthgen.messages;

import org.fifthgen.messages.server.ServerApplication;
import org.fifthgen.messages.server.ServerClient;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ServiceTestImpl {
    protected final int PORT = 1212;

    protected final List<String> initMsgs = List.of(
            "Hi {user}! We are Candles. Are you interested in working for us? Reply to this message with email",
            "Hi {user}! I am contacting you regarding a job opportunity at our company. Send us your email address.",
            "Hi {user}! New vacancies at Candles. Please reply with email if interested."
    );

    protected final List<String> interviewMsgs = List.of(
            "Hi {user}! We have registered your request for candidacy. Whats the best time for an interview?",
            "Hi {user}! Please reply with a suitable time for an interview.",
            "Hi {user}! We would like to schedule an interview for the vacancy you have applied. Please reply with a suitable date and time."
    );

    protected final String interviewConfirmation = "Hi {user}! We have scheduled your interview on {date}";

    protected final Map<Integer, String> users = new HashMap<>();
    protected final Map<Integer, String> userEmails = new HashMap<>();
    protected final Map<Integer, String> userInterviewSchedules = new HashMap<>();
    protected final String RFC_5322_EMAIL_PATTERN = "^[a-zA-Z0-9_!#$%&'*+/=?`{|}~^.-]+@[a-zA-Z0-9.-]+$";

    protected final String GREGORIAN_DATE_PATTERN = "^((2000|2400|2800|(19|2[0-9])(0[48]|[2468][048]|[13579][26]))-02-29)$"
            + "|^(((19|2[0-9])[0-9]{2})-02-(0[1-9]|1[0-9]|2[0-8]))$"
            + "|^(((19|2[0-9])[0-9]{2})-(0[13578]|10|12)-(0[1-9]|[12][0-9]|3[01]))$"
            + "|^(((19|2[0-9])[0-9]{2})-(0[469]|11)-(0[1-9]|[12][0-9]|30))$";

    protected Set<Track> tracks = new HashSet<>();

    protected ExecutorService executorService = Executors.newFixedThreadPool(10);

    protected ServerApplication server;

    protected int trackCnt = 0;

    private void init() {
        System.out.println("Starting server ...");
        server = new ServerApplication(PORT);

        // Run the server on a new thread to stop blocking
        new Thread(() -> server.startServer()).start();

        // Wait for clients to connect
        System.out.println("Please connect the clients within 20 seconds");

        try {
            Thread.sleep(20000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        System.out.println("Setting up tracks and nodes ...");
        for (ServerClient client : server.getClients()) {
            users.put(client.getId(), client.getName());
        }

        if (users.size() < 1) {
            System.out.println("No clients connected. Existing ...");
            return;
        }

        for (var key : users.keySet()) {
            // -------------------------------------
            // Set up first track, contacting users.
            // -------------------------------------
            Track initTrack = new Track(++trackCnt, server);
            initTrack.setClientRef(key);
            initTrack.setValidator(response -> true);
            initTrack.setResponseCallback(response -> userEmails.put(response.getId(), response.getMessage()));

            int delay = 0;
            for (String msg : initMsgs) {
                delay += 2;
                Node node = new Node(delay, msg.replace("{user}", users.get(key)));
                initTrack.addNode(node);
            }

            // --------------------------
            // End setting up first track
            // --------------------------

            // ------------------------------------------------
            // Set up second track which schedules an interview
            // ------------------------------------------------
            Track interviewTrack = new Track(++trackCnt, server);
            interviewTrack.setClientRef(key);
            interviewTrack.setValidator(response -> true);
            interviewTrack.setResponseCallback(response -> userInterviewSchedules.put(response.getId(),
                    response.getMessage()));

            delay = 0;
            for (String msg : interviewMsgs) {
                delay += 5;
                Node node = new Node(delay, msg.replace("{user}", users.get(key)));
                interviewTrack.addNode(node);
            }

            initTrack.setNext(interviewTrack);
            // -------------------------------
            // End setting up interview track
            // -------------------------------

            // ------------------------------------------------
            // Set up confirmation track
            // ------------------------------------------------
            Track confirmationTrack = new Track(++trackCnt, server);
            confirmationTrack.setClientRef(key);
            confirmationTrack.setDependsOnPrevTrack(true);
            confirmationTrack.setValidator(response -> true);
            confirmationTrack.setResponseCallback(new TrackResponseCallBackWithOnStart() {
                @Override
                public void onStart(Response response, List<Node> nodes) {
                    // Replace date from the previous response
                    nodes.forEach(node -> node.setMessage(node.getMessage().replace("{date}",
                            response.getMessage())));
                }

                @Override
                public void onSuccess(Response response) {
                }
            });

            Node node = new Node(0, interviewConfirmation.replace("{user}", users.get(key)));
            confirmationTrack.addNode(node);
            interviewTrack.setNext(confirmationTrack);
            // ---------------------------------
            // End setting up confirmation track
            // ---------------------------------

            tracks.add(initTrack);
        }
    }

    void run() {
        init();
        tracks.forEach(track -> executorService.execute(track));

        // TODO: Add a countdown latch and remove the following block
        try {
            Thread.sleep(360000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) {
        new ServiceTestImpl().run();
    }
}
