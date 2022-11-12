package org.fifthgen.messages;

import java.util.List;

public interface TrackResponseCallBackWithOnStart extends TrackResponseCallback {

    void onStart(Response response, List<Step> steps);
}
