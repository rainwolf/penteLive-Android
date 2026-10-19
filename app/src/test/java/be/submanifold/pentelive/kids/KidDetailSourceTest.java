package be.submanifold.pentelive.kids;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.Test;

/** There is no Activity test harness, so this pins the KidDetailActivity wiring at source level. */
public class KidDetailSourceTest {

    private static final String ACTIVITY =
            "src/main/java/be/submanifold/pentelive/kids/KidDetailActivity.java";
    private static final String LAYOUT = "src/main/res/layout/activity_kid_detail.xml";

    private static String read(String path) throws IOException {
        return new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
    }

    private static int count(String s, String needle) {
        int n = 0;
        for (int i = s.indexOf(needle); i >= 0; i = s.indexOf(needle, i + 1)) {
            n++;
        }
        return n;
    }

    @Test
    public void layoutHasTheControls() throws IOException {
        String layout = read(LAYOUT);
        for (String id : new String[] {"my_toolbar", "copyMessagesSwitch", "copyLiveChatSwitch",
                "approvedList", "emptyText", "addApprovedButton", "setPasswordButton",
                "releaseButton"}) {
            assertTrue("missing id " + id, layout.contains("@+id/" + id + "\""));
        }
        assertEquals(2, count(layout, "<androidx.appcompat.widget.SwitchCompat"));
        assertTrue(layout.contains("@string/kid_copy_live_chat_note"));
    }

    @Test
    public void activityIsWiredToTheClient() throws IOException {
        String src = read(ACTIVITY);
        assertTrue(src.contains("EXTRA_KID_PID = \"kidPid\""));
        assertTrue(src.contains("KidAccounts.findKid("));
        assertTrue(src.contains("KidAccountsClient.settings(kidPid, flag, null)"));
        assertTrue(src.contains("KidAccountsClient.settings(kidPid, null, flag)"));
        assertTrue(src.contains("KidAccountsClient.whitelistRemove("));
        assertTrue(src.contains("KidAccountsClient.whitelistAdd("));
        assertTrue(src.contains("KidAccountsClient.setPassword("));
        assertTrue(src.contains("KidAccountsClient.release("));
        assertTrue(src.contains("TYPE_TEXT_VARIATION_PASSWORD"));
        assertTrue(src.contains("ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT"));
        assertTrue(src.contains("R.string.kid_password_set"));
        assertTrue(src.contains("R.string.kid_remove_approved_title"));
        assertTrue(src.contains("R.string.kid_remove_approved_text"));
        assertTrue(src.contains("R.string.kid_release_text"));
        assertTrue(src.contains("R.string.try_again"));
    }

    /** German "freigeben" means approve (kid_add_approved); release must use a different verb. */
    @Test
    public void germanReleaseDoesNotReuseTheApproveVerb() throws IOException {
        String de = read("src/main/res/values-de/strings.xml");
        assertTrue(de.contains("<string name=\"kid_release\">Kinderkonto auflösen</string>"));
    }
}
