package be.submanifold.pentelive;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.google.gson.Gson;

import org.junit.After;
import org.junit.Test;

public class KidFlagTest {
    @After public void resetKidFlag() { PentePlayer.parentPid = null; } // the static must not leak into other suites in the same JVM

    @Test public void indexJsonWithParentPid_isKid() {
        JsonModels.IndexResponse r = new Gson().fromJson(
            "{\"player\":{\"name\":\"kid_one\",\"parentPid\":23000000012345}}", JsonModels.IndexResponse.class);
        assertEquals(Long.valueOf(23000000012345L), r.player.parentPid);
    }

    @Test public void indexJsonWithoutParentPid_isNotKid() {
        JsonModels.IndexResponse r = new Gson().fromJson("{\"player\":{\"name\":\"iostest\"}}", JsonModels.IndexResponse.class);
        assertNull(r.player.parentPid);
    }

    @Test public void isKid_followsParentPid() {
        PentePlayer.parentPid = 23000000012345L; assertTrue(PentePlayer.isKid());
        PentePlayer.parentPid = null; assertFalse(PentePlayer.isKid());
    }

    @Test public void newPlayer_resetsKidFlag() {
        PentePlayer.parentPid = 23000000012345L;
        new PentePlayer("iostest", "pw");
        assertFalse(PentePlayer.isKid());
    }
}
