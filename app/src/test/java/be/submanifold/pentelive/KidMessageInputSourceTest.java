package be.submanifold.pentelive;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.Test;

/** Kid wiring of the broadcast menu and the in-game message hint (source-level: no Robolectric here). */
public class KidMessageInputSourceTest {

    private static final String JAVA = "src/main/java/be/submanifold/pentelive/";
    private static final String RES = "src/main/res/";

    private static String read(String path) throws IOException {
        return new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
    }

    @Test
    public void lobbyHidesBroadcastForKids() throws IOException {
        String s = read(JAVA + "liveGameRoom/LobbyActivity.java");
        assertTrue(s.contains("menu.findItem(R.id.broadcast).setVisible(!PentePlayer.isKid())"));
    }

    @Test
    public void broadcastTaskHandlesKidRefusalFirst() throws IOException {
        String s = read(JAVA + "liveGameRoom/LobbyActivity.java");
        int kid = s.indexOf("dashboardString.contains(\"Kid accounts can't broadcast.\")");
        int subs = s.indexOf("dashboardString.contains(\"Broadcasting to followers");
        assertTrue("kid branch missing", kid >= 0);
        assertTrue("kid branch must come first", kid < subs);
        assertTrue(s.contains("R.string.kid_broadcast_refused"));
    }

    @Test
    public void boardHintsKidsOnMessageInput() throws IOException {
        String s = read(JAVA + "BoardActivity.java");
        assertTrue(s.contains("PentePlayer.isKid()"));
        assertTrue(s.contains("R.id.messageInput"));
        assertTrue(s.contains("setHint(R.string.kid_chat_hint)"));
    }

    @Test
    public void stringsExistInBothLocales() throws IOException {
        String en = read(RES + "values/strings.xml");
        String de = read(RES + "values-de/strings.xml");
        assertTrue(en.contains("<string name=\"kid_broadcast_refused\">Kid accounts can\\'t broadcast.</string>"));
        assertTrue(en.contains("<string name=\"kid_chat_hint\">Only players your parent approved will see your messages.</string>"));
        assertTrue(de.contains("<string name=\"kid_broadcast_refused\">Kinderkonten können nicht rundsenden.</string>"));
        assertTrue(de.contains("<string name=\"kid_chat_hint\">Nur von deinen Eltern freigegebene Spieler sehen deine Nachrichten.</string>"));
    }
}
