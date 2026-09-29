package be.submanifold.pentelive.net;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/** Builds application/x-www-form-urlencoded bodies. */
public final class Forms {

    private Forms() {
    }

    /**
     * {@code encode("a", "1", "b", "x y")} is {@code "a=1&b=x+y"}; names and values are
     * UTF-8 form-encoded.
     *
     * @throws IllegalArgumentException if the arguments are not name/value pairs
     */
    public static String encode(String... namesAndValues) throws UnsupportedEncodingException {
        if (namesAndValues.length % 2 != 0) {
            throw new IllegalArgumentException("name/value pairs expected, got " + namesAndValues.length + " strings");
        }
        StringBuilder form = new StringBuilder();
        for (int i = 0; i < namesAndValues.length; i += 2) {
            if (i > 0) {
                form.append('&');
            }
            form.append(URLEncoder.encode(namesAndValues[i], "UTF-8"))
                    .append('=')
                    .append(URLEncoder.encode(namesAndValues[i + 1], "UTF-8"));
        }
        return form.toString();
    }
}
