package org.fifthgen.messages;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

class TrackTest {

    protected Set<Track> tracks;

    protected ExecutorService executorService;

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

    protected final Map<String, String> users = Map.of(
            "samantha_g@aol.com", "Samantha",
            "gsturt23@gmail.com", "Gavin",
            "kevin_lambert@gmail.com", "Lambert",
            "neonf@aol.com", "Frederich"
    );

    protected final String confirmationMsg = "Hi {user}! Your interview is scheduled at {schedule}.";


    @BeforeEach
    void setUp() {
        System.out.println("Setting up tracks and nodes ...");

        tracks = new HashSet<>();
        executorService = Executors.newFixedThreadPool(10);

        for (var email : users.keySet()) {
            // -------------------------------------
            // Set up first track, contacting users.
            // -------------------------------------
            Track initTrack = new Track();

            int delay = 0;
            for (String msg : initMsgs) {
                delay += 2;
                Node node = new Node(delay, msg.replace("{user}", users.get(email)));
                initTrack.addNode(node);
            }


            // --------------------------
            // End setting up first track
            // --------------------------

            // ------------------------------------------------
            // Set up second track which schedules an interview
            // ------------------------------------------------
            Track interviewTrack = new Track();

            delay = 0;
            for (String msg : interviewMsgs) {
                delay += 5;
                Node node = new Node(delay, msg.replace("{user}", users.get(email)));
                interviewTrack.addNode(node);
            }

            initTrack.setNext(interviewTrack);
            // -------------------------------
            // End setting up interview track
            // -------------------------------

            // ------------------------------------------------
            // Set up confirmation track
            // ------------------------------------------------
            Track confirmationTrack = new Track();
            Node node = new Node(0, confirmationMsg);
            confirmationTrack.addNode(node);
            interviewTrack.setNext(confirmationTrack);
            // ---------------------------------
            // End setting up confirmation track
            // ---------------------------------

            tracks.add(initTrack);
        }

        System.out.println("Done setting up test users ...");
    }

    @AfterEach
    void tearDown() {
        tracks = null;
    }

    @Test
    void start() {
        tracks.forEach(track -> executorService.execute(track));

        try {
            Thread.sleep(360000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}