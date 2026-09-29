package be.submanifold.pentelive.net;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class FormsTest {

    @Test
    public void encode_joinsPairsInOrder() throws Exception {
        assertEquals("mobile=&name2=alice&password2=secret",
                Forms.encode("mobile", "", "name2", "alice", "password2", "secret"));
    }

    @Test
    public void encode_escapesFormSpecialCharactersAsUtf8() throws Exception {
        assertEquals("password2=a%26b%2Bc%25d%3De+f%C3%BC%23",
                Forms.encode("password2", "a&b+c%d=e fü#"));
    }

    @Test
    public void encode_escapesBase64Tokens() throws Exception {
        assertEquals("password=x%2Fy%2Bz%3D%3D", Forms.encode("password", "x/y+z=="));
    }

    @Test(expected = IllegalArgumentException.class)
    public void encode_rejectsAnOddNumberOfArguments() throws Exception {
        Forms.encode("name2");
    }
}
