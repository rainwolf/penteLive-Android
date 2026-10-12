package be.submanifold.pentelive;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

/** Every upsell opens the native StoreActivity; no code path reaches the PayPal page (spec 3.5). */
public class StoreRoutingSourceTest {

    private static final String JAVA = "src/main/java/be/submanifold/pentelive/";
    /** Holds the page paths on purpose: it recognises them. */
    private static final String EXEMPT = "SubscriptionLinks.java";

    @Test
    public void noJavaCodeOpensThePayPalSubscriptionsPage() throws IOException {
        File root = new File("src/main/java");
        assertTrue("source root not found: " + root.getAbsolutePath(), root.isDirectory());
        List<String> hits = new ArrayList<>();
        scan(root, hits);
        assertTrue("subscription page URLs in source:\n" + String.join("\n", hits), hits.isEmpty());
    }

    @Test
    public void everyUpsellOpensStoreActivity() throws IOException {
        assertEquals(1, count(read(JAVA + "SettingsActivity.java"), "StoreActivity.class"));
        assertEquals(2, count(read(JAVA + "MainActivity.java"), "StoreActivity.class"));
        assertEquals(1, count(read(JAVA + "liveGameRoom/LobbyActivity.java"), "StoreActivity.class"));
        assertEquals(1, count(read(JAVA + "KingOfTheHillActivity.java"), "StoreActivity.class"));
        assertEquals(1, count(read(JAVA + "Game.java"), "StoreActivity.class"));
    }

    @Test
    public void subscribeLabelIsPlainSubscribe() throws IOException {
        assertTrue(read("src/main/res/values/strings.xml")
                .contains("<string name=\"subscribe_pente_org\">Subscribe</string>"));
        assertTrue(read("src/main/res/values-de/strings.xml")
                .contains("<string name=\"subscribe_pente_org\">Abonnieren</string>"));
    }

    @Test
    public void webViewHandsSubscriptionLinksToStoreActivity() throws IOException {
        String source = read(JAVA + "WebViewActivity.java");
        assertEquals(2, count(source, "SubscriptionLinks.isSubscriptionPage("));
        assertEquals(2, count(source, "StoreActivity.class"));
    }

    // ---- helpers ----

    private static String read(String path) throws IOException {
        return new String(Files.readAllBytes(new File(path).toPath()), StandardCharsets.UTF_8);
    }

    private static int count(String text, String needle) {
        int count = 0;
        for (int i = text.indexOf(needle); i >= 0; i = text.indexOf(needle, i + needle.length())) {
            count++;
        }
        return count;
    }

    private static void scan(File dir, List<String> hits) throws IOException {
        File[] children = dir.listFiles();
        if (children == null) {
            return;
        }
        Arrays.sort(children);
        for (File child : children) {
            if (child.isDirectory()) {
                scan(child, hits);
            } else if (child.getName().endsWith(".java") && !child.getName().equals(EXEMPT)) {
                List<String> lines = Files.readAllLines(child.toPath(), StandardCharsets.UTF_8);
                for (int i = 0; i < lines.size(); i++) {
                    String line = lines.get(i);
                    if (line.contains("gameServer/subscriptions") || line.contains("gameServer/subscribe.jsp")) {
                        hits.add(child.getPath() + ":" + (i + 1) + ": " + line.trim());
                    }
                }
            }
        }
    }
}
