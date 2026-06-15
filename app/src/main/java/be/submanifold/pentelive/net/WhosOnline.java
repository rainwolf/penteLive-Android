package be.submanifold.pentelive.net;

import be.submanifold.pentelive.JsonModels;

import java.util.Collections;
import java.util.List;

/**
 * Typed result of GET mobile/json/whosonlineandlive.jsp. Wraps the raw
 * List&lt;JsonModels.RoomEntry&gt; produced by that endpoint (see
 * MainActivity#LoadWhosOnlineTask, lines 531-585). Never holds a null list.
 */
public final class WhosOnline {

    public final List<JsonModels.RoomEntry> rooms;

    public WhosOnline(List<JsonModels.RoomEntry> rooms) {
        this.rooms = rooms == null
                ? Collections.<JsonModels.RoomEntry>emptyList()
                : rooms;
    }
}
