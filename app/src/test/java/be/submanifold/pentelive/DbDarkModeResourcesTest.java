package be.submanifold.pentelive;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Night-mode guard for the DB screens (JVM, no device): every db_* colour has a values-night variant, text
 * stays readable on its surface in both modes, and the DB layouts use no literal colours or fixed-light themes.
 */
public class DbDarkModeResourcesTest {
    private static final File RES = new File("src/main/res");
    private static final List<String> DB_LAYOUTS = Arrays.asList(
            "activity_database.xml", "activity_db_game_viewer.xml", "db_ai_settings.xml", "db_subscribers_only.xml",
            "item_db_filter_chip.xml", "item_db_game.xml", "item_db_games_footer.xml", "item_db_games_header.xml",
            "item_db_move.xml", "item_db_move_cell.xml", "sheet_db_filters.xml");

    private static Map<String, String> colours(String qualifier) throws Exception {
        File f = new File(RES, qualifier + "/db_colors.xml");
        assertTrue(f.getPath() + " exists", f.isFile());
        Document d = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(f);
        NodeList nodes = d.getElementsByTagName("color");
        Map<String, String> out = new LinkedHashMap<>();
        for (int i = 0; i < nodes.getLength(); i++) {
            Element e = (Element) nodes.item(i);
            out.put(e.getAttribute("name"), e.getTextContent().trim());
        }
        return out;
    }

    private static double luminance(String hex) {
        int rgb = Integer.parseInt(hex.substring(1), 16);
        double[] c = {(rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF};
        for (int i = 0; i < 3; i++) {
            double v = c[i] / 255.0;
            c[i] = v <= 0.03928 ? v / 12.92 : Math.pow((v + 0.055) / 1.055, 2.4);
        }
        return 0.2126 * c[0] + 0.7152 * c[1] + 0.0722 * c[2];
    }

    private static double contrast(String a, String b) {
        double la = luminance(a);
        double lb = luminance(b);
        return (Math.max(la, lb) + 0.05) / (Math.min(la, lb) + 0.05);
    }

    private static String read(File f) throws IOException {
        return new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
    }

    @Test
    public void everyDbColourHasANightVariantThatDiffersFromLight() throws Exception {
        Map<String, String> light = colours("values");
        Map<String, String> night = colours("values-night");
        assertEquals(light.keySet(), night.keySet());
        for (String name : light.keySet()) {
            assertFalse(name + " must change in night mode", light.get(name).equalsIgnoreCase(night.get(name)));
        }
    }

    @Test
    public void textIsReadableOnTheSheetInBothModes() throws Exception {
        for (String q : Arrays.asList("values", "values-night")) {
            Map<String, String> c = colours(q);
            String bg = c.get("db_sheet_background");
            assertTrue(q + " ink", contrast(c.get("db_ink"), bg) >= 4.5);
            assertTrue(q + " secondary", contrast(c.get("db_secondary_text"), bg) >= 4.5);
            assertTrue(q + " secondary alt", contrast(c.get("db_secondary_text_alt"), bg) >= 4.5);
            assertTrue(q + " accent", contrast(c.get("db_accent"), bg) >= 3.0);
            assertTrue(q + " selected row keeps ink readable", contrast(c.get("db_ink"), c.get("db_chip_selected")) >= 4.5);
        }
    }

    @Test
    public void dbLayoutsUseNoLiteralColours() throws IOException {
        Pattern literal = Pattern.compile("(textColor|background|tint|textColorHint|foreground)=\"#[0-9a-fA-F]{3,8}\"");
        List<String> offending = new ArrayList<>();
        for (String name : DB_LAYOUTS) {
            File f = new File(RES, "layout/" + name);
            assertTrue(name, f.isFile());
            Matcher m = literal.matcher(read(f));
            while (m.find()) offending.add(name + ": " + m.group());
        }
        assertEquals(new ArrayList<String>(), offending);
    }

    @Test
    public void dbLayoutsDoNotPinTextToBlackOrWhite() throws IOException {
        Pattern fixed = Pattern.compile("=\"@android:color/(black|white|background_light|background_dark)\"");
        List<String> offending = new ArrayList<>();
        for (String name : DB_LAYOUTS) {
            Matcher m = fixed.matcher(read(new File(RES, "layout/" + name)));
            while (m.find()) offending.add(name + ": " + m.group());
        }
        assertEquals(new ArrayList<String>(), offending);
    }

    @Test
    public void theMovesSheetFollowsTheNightTheme() throws IOException {
        String xml = read(new File(RES, "layout/activity_database.xml"));
        int at = xml.indexOf("android:id=\"@+id/dbSheet\"");
        assertTrue(at > 0);
        String sheet = xml.substring(at, xml.indexOf(">", at));
        Matcher m = Pattern.compile("android:theme=\"@style/(\\S+?)\"").matcher(sheet);
        assertTrue(m.find());
        assertEquals("ThemeOverlay.AppCompat.DayNight", m.group(1));
    }

    @Test
    public void filtersSheetUsesADayNightMaterialTheme() throws IOException {
        String styles = read(new File(RES, "values/styles.xml"));
        assertTrue(styles.contains("name=\"Theme.PenteDb.FiltersSheet\" parent=\"Theme.Material3.DayNight.BottomSheetDialog\""));
        String sheet = read(new File(RES, "layout/sheet_db_filters.xml"));
        assertNotNull(sheet);
        assertFalse("filters sheet text colours come from the theme, not db_ink", sheet.contains("@color/db_ink"));
    }
}
