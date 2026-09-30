package be.submanifold.pentelive.net;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.Test;

public class CookieHeaderTest {

    @Test
    public void parse_splitsOnSemicolons_andKeepsEqualsInsideValues() {
        Map<String, String> cookies = CookieHeader.parse("JSESSIONID=abc; password2=x/y+z==; name2=alice");

        assertEquals(Arrays.asList("JSESSIONID", "password2", "name2"), new ArrayList<>(cookies.keySet()));
        assertEquals("abc", cookies.get("JSESSIONID"));
        assertEquals("x/y+z==", cookies.get("password2"));
        assertEquals("alice", cookies.get("name2"));
    }

    @Test
    public void parse_emptyHeader_givesEmptyMap() {
        assertTrue(CookieHeader.parse("").isEmpty());
    }

    @Test
    public void parse_toleratesMissingOrExtraSpacesAndTrailingSemicolon() {
        Map<String, String> cookies = CookieHeader.parse("a=1;b=2 ;  c=3;");

        assertEquals(3, cookies.size());
        assertEquals("1", cookies.get("a"));
        assertEquals("2", cookies.get("b"));
        assertEquals("3", cookies.get("c"));
    }

    @Test
    public void parse_emptyValue_isKept() {
        Map<String, String> cookies = CookieHeader.parse("plugin=; name2=bob");

        assertEquals("", cookies.get("plugin"));
        assertEquals("bob", cookies.get("name2"));
    }

    @Test
    public void parse_duplicateName_firstWins_butPairsKeepsBoth() {
        String header = "name2=first; name2=second";

        assertEquals("first", CookieHeader.parse(header).get("name2"));
        List<String[]> pairs = CookieHeader.pairs(header);
        assertEquals(2, pairs.size());
        assertEquals("second", pairs.get(1)[1]);
    }
}
