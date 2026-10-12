package be.submanifold.pentelive;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.junit.Test;

/** StoreActivity is registered, deep-links to Play's subscription page and has every buy button (spec 3.4). */
public class StoreActivityConfigTest {

    private static String read(String path) throws IOException {
        return new String(Files.readAllBytes(new File(path).toPath()), StandardCharsets.UTF_8);
    }

    @Test
    public void manageSubscriptionDeepLinkMatchesSpec() {
        assertEquals("https://play.google.com/store/account/subscriptions?sku=yearly_subscription"
                + "&package=be.submanifold.pentelive", StoreActivity.MANAGE_SUBSCRIPTION_URL);
    }

    @Test
    public void manifestRegistersStoreActivity() throws IOException {
        assertTrue(read("src/main/AndroidManifest.xml").contains("android:name=\".StoreActivity\""));
    }

    @Test
    public void layoutHasEveryButtonAndBuyButtonsStartDisabled() throws IOException {
        String layout = read("src/main/res/layout/activity_store.xml");
        String[] ids = {"buyYearlyButton", "buyMonthPassButton", "buyVacation10Button", "buyVacation30Button",
                "buyVacation60Button", "manageSubscriptionButton", "privacyPolicyButton", "eulaButton",
                "storeStatus", "subscribedUntilText", "yearlyPrice", "yearlyOverlapWarning", "yearlyOwnedHint", "monthPassCard",
                "monthPassStacksNote", "vacationCard", "vacationNote"};
        for (String id : ids) {
            assertTrue("missing @+id/" + id, layout.contains("@+id/" + id + "\""));
        }
        int disabled = 0;
        for (int i = layout.indexOf("android:enabled=\"false\""); i >= 0;
             i = layout.indexOf("android:enabled=\"false\"", i + 1)) {
            disabled++;
        }
        assertEquals(5, disabled);
    }
}
