package be.submanifold.pentelive.db;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class FilterSummaryTest {
    @Test public void example() {
        DbFilters f = DbFilters.defaults().toBuilder().player1Name("alice").player1MinRating(1700)
                .eitherSeat(true).build();
        assertEquals("Pente · alice 1700+ · either seat", FilterSummary.text(f));
    }

    @Test public void defaultsShowGameOnly() {
        assertEquals("Pente", FilterSummary.text(DbFilters.defaults()));
        assertEquals("Speed Keryo-Pente",
                FilterSummary.text(DbFilters.defaults().toBuilder().baseGameId(3).speed(true).build()));
    }

    @Test public void b3b4PartsOnlyWhenCapabilityOn() {
        DbFilters f = DbFilters.defaults().toBuilder().excludeTimeouts(true).liveOrTurnBased("live").build();
        assertEquals("Pente", FilterSummary.text(f, false));
        assertEquals("Pente · no timeouts · live only", FilterSummary.text(f, true));
    }

    @Test public void ratingWithoutNameUsesSeatLabel() {
        DbFilters f = DbFilters.defaults().toBuilder().player2MinRating(1800).winner(1).scope("both").build();
        assertEquals("Pente · P2 1800+ · P1 won · Archive + Mine", FilterSummary.text(f));
    }
}
