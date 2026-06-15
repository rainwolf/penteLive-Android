package be.submanifold.pentelive.net;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import be.submanifold.pentelive.JsonModels;

public class WhosOnlinePresenterTest {

    static final class RecordingView implements WhosOnlineView {
        WhosOnline rendered;
        Result.Reason errorShown;

        @Override
        public void renderWhosOnline(WhosOnline data) {
            this.rendered = data;
        }

        @Override
        public void showError(Result.Reason reason) {
            this.errorShown = reason;
        }
    }

    private WhosOnline sampleWhosOnline() {
        JsonModels.RoomEntry room = new JsonModels.RoomEntry();
        room.name = "Mobile";
        room.players = new ArrayList<>();
        List<JsonModels.RoomEntry> rooms = new ArrayList<>();
        rooms.add(room);
        return new WhosOnline(rooms);
    }

    @Test
    public void rendersOnOk() {
        FakePenteApi api = new FakePenteApi();
        api.whosOnlineResponse = sampleWhosOnline();
        RecordingView view = new RecordingView();
        WhosOnlinePresenter presenter = new WhosOnlinePresenter(view);

        presenter.onResult(api.whosOnline());

        assertNotNull(view.rendered);
        assertEquals(1, view.rendered.rooms.size());
        assertEquals("Mobile", view.rendered.rooms.get(0).name);
        assertNull(view.errorShown);
        assertEquals(1, api.calls.size());
        assertEquals("whosOnline", api.calls.get(0));
    }

    @Test
    public void showsTypedErrorOnFailure() {
        FakePenteApi api = new FakePenteApi();
        api.nextFailure = Result.Reason.NETWORK;
        RecordingView view = new RecordingView();
        WhosOnlinePresenter presenter = new WhosOnlinePresenter(view);

        presenter.onResult(api.whosOnline());

        assertNull(view.rendered);
        assertEquals(Result.Reason.NETWORK, view.errorShown);
    }

    @Test
    public void showsAuthExpiredErrorAndDoesNotRender() {
        FakePenteApi api = new FakePenteApi();
        api.nextFailure = Result.Reason.AUTH_EXPIRED;
        RecordingView view = new RecordingView();
        WhosOnlinePresenter presenter = new WhosOnlinePresenter(view);

        presenter.onResult(api.whosOnline());

        assertNull(view.rendered);
        assertEquals(Result.Reason.AUTH_EXPIRED, view.errorShown);
    }
}
