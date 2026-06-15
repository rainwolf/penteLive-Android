package be.submanifold.pentelive.net;

import okhttp3.CookieJar;

/** In-memory Session test double with no Android dependencies. */
public final class FakeSession implements Session {

    private String name;
    private String password;
    private final InMemoryCookieJar cookieJar = new InMemoryCookieJar();

    public FakeSession(String name, String password) {
        this.name = name;
        this.password = password;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public String password() {
        return password;
    }

    @Override
    public CookieJar cookieJar() {
        return cookieJar;
    }

    @Override
    public void updateCredentials(String name, String password) {
        this.name = name;
        this.password = password;
    }
}
