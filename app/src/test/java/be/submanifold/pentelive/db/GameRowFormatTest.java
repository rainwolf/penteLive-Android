package be.submanifold.pentelive.db;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import be.submanifold.pentelive.KothCrownDrawables;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Locale;
import org.junit.Test;

public class GameRowFormatTest {
    private static final ZonedDateTime NOW_UTC =
            ZonedDateTime.of(2026, 10, 29, 12, 0, 0, 0, ZoneOffset.UTC);

    @Test
    public void score() {
        assertEquals("1 – 0", GameRowFormat.score(1));
        assertEquals("0 – 1", GameRowFormat.score(2));
        assertEquals("–", GameRowFormat.score(0));
    }

    @Test
    public void rating() {
        assertEquals(0xFFFF0000, RatingColors.argb(1900));
        assertEquals(0xFFFAF508, RatingColors.argb(1899));
        assertEquals(0xFFFAF508, RatingColors.argb(1700));
        assertEquals(0xFF0000FF, RatingColors.argb(1699));
        assertEquals(0xFF0000FF, RatingColors.argb(1400));
        assertEquals(0xFF1E824C, RatingColors.argb(1399));
        assertEquals(0xFF1E824C, RatingColors.argb(1000));
        assertEquals(0xFF888888, RatingColors.argb(999));
        assertEquals(0xFF888888, RatingColors.argb(0));
    }

    @Test
    public void crown() {
        assertEquals(Crowns.Kind.GOLD, Crowns.kind(1));
        assertEquals(Crowns.Kind.SILVER, Crowns.kind(2));
        assertEquals(Crowns.Kind.BRONZE, Crowns.kind(3));
        assertEquals(Crowns.Kind.KOTH, Crowns.kind(4));
        assertEquals(0, Crowns.kothIndex(4));
        assertEquals(Crowns.Kind.KOTH, Crowns.kind(39));
        assertEquals(35, Crowns.kothIndex(39));
        assertEquals(Crowns.Kind.KOTH, Crowns.kind(42));
        assertEquals(38, Crowns.kothIndex(42));
        assertEquals(Crowns.Kind.NONE, Crowns.kind(0));
    }

    @Test
    public void crownOutOfRangeIsNone() {
        assertEquals(Crowns.Kind.NONE, Crowns.kind(43));
        assertEquals(Crowns.Kind.NONE, Crowns.kind(99));
        assertEquals(Crowns.Kind.NONE, Crowns.kind(-1));
        assertEquals(0, Crowns.safeCode(99));
        assertEquals(0, Crowns.safeCode(-1));
        assertEquals(42, Crowns.safeCode(42));
    }

    @Test
    public void kothCountMatchesDrawables() {
        assertEquals(KothCrownDrawables.IDS.length, Crowns.KOTH_COUNT);
    }

    @Test
    public void nameColour() {
        assertEquals(0xFF16181D, GameRowFormat.nameArgb(0));
        assertFalse(GameRowFormat.nameBold(0));
        assertEquals(0xFF000001, GameRowFormat.nameArgb((255 << 24) + 1));
        assertTrue(GameRowFormat.nameBold((255 << 24) + 1));
    }

    @Test
    public void dateSameYear() {
        assertEquals("May 1",
                GameRowFormat.date("2026-05-01T12:00:00Z", NOW_UTC, Locale.US));
    }

    @Test
    public void dateOtherYear() {
        assertEquals("May 1, 2019",
                GameRowFormat.date("2019-05-01T12:00:00Z", NOW_UTC, Locale.US));
    }

    @Test
    public void dateUsesNowZone() {
        ZonedDateTime ny = ZonedDateTime.of(2026, 10, 29, 12, 0, 0, 0,
                ZoneId.of("America/New_York"));
        assertEquals("Dec 31, 2025",
                GameRowFormat.date("2026-01-01T02:00:00Z", ny, Locale.US));
    }

    @Test
    public void nullDateIsEmpty() {
        assertEquals("", GameRowFormat.date(null, NOW_UTC, Locale.US));
        assertEquals("", GameRowFormat.date("garbage", NOW_UTC, Locale.US));
    }

    @Test
    public void listHeaderPerScope() {
        String withMoves = "Games through this position (3 moves)";
        String noMoves = "All matching games";
        String plus = ", plus all your games (personal games not position-filtered)";
        assertEquals(withMoves, GameRowFormat.listHeader("archive", 3));
        assertEquals(noMoves, GameRowFormat.listHeader("archive", 0));
        String mine = "My games (position and filters not applied)";
        assertEquals(mine, GameRowFormat.listHeader("mine", 3));
        assertEquals(mine, GameRowFormat.listHeader("mine", 0));
        assertEquals(withMoves + plus, GameRowFormat.listHeader("both", 3));
        assertEquals(noMoves + plus, GameRowFormat.listHeader("both", 0));
    }
}
