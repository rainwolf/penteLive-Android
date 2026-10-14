package be.submanifold.pentelive.billing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

/** Play Billing types stay inside BillingManager, so every decision class is unit-testable. */
public class BillingIsolationTest {

    @Test
    public void onlyBillingManagerUsesPlayBilling() throws IOException {
        File root = new File("src/main/java");
        assertTrue("source root not found: " + root.getAbsolutePath(), root.isDirectory());
        List<String> users = new ArrayList<>();
        collect(root, users);
        assertEquals(Collections.singletonList("BillingManager.java"), users);
    }

    private static void collect(File dir, List<String> users) throws IOException {
        File[] children = dir.listFiles();
        if (children == null) {
            return;
        }
        Arrays.sort(children);
        for (File child : children) {
            if (child.isDirectory()) {
                collect(child, users);
            } else if (child.getName().endsWith(".java")) {
                String text = new String(Files.readAllBytes(child.toPath()), StandardCharsets.UTF_8);
                if (text.contains("com.android.billingclient")) {
                    users.add(child.getName());
                }
            }
        }
    }
}
