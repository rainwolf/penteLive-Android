# Extract PenteApi Transport Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (- [ ]) syntax for tracking.

**Goal:** Replace ~47 hand-rolled HttpsURLConnection sites and 35 AsyncTasks with one deep, testable transport module (PenteApi), migrated endpoint-by-endpoint.

**Architecture:** One fat, synchronous, thread-agnostic `PenteApi` interface returning typed `Result<T>`. A real adapter `OkHttpPenteApi` (OkHttp, single `CookieJar`, single-flight re-auth `Authenticator`) and an in-memory `FakePenteApi` are the two adapters. A `Session` + `BaseUrlProvider` own credentials, the single cookie store, and dev/prod resolution. A thin lifecycle-aware `PenteApiClient` (single serial Executor + main Handler) runs calls off-main and returns a `Cancelable`. Migration is a strangler: one low-risk GET first, then task-by-task.

**Tech Stack:** Java, Android (minSdk 26), OkHttp 4.x, GSON 2.13.2, JUnit 4.13.2.

---

## File Structure

- Modify: `app/build.gradle` (add OkHttp)
- Create: `app/src/main/java/be/submanifold/pentelive/net/` — `Json.java`, `Result.java`, `Session.java`, `SharedPrefsSession.java`, `BaseUrlProvider.java`, `PenteApi.java`, `FakePenteApi.java`, `OkHttpPenteApi.java`, `PenteApiClient.java`
- Create: `app/src/test/java/be/submanifold/pentelive/net/` — unit tests (Result, Session, FakePenteApi, OkHttpPenteApi re-auth, PenteApiClient lifecycle)
- Modify: `app/src/main/java/be/submanifold/pentelive/MainActivity.java` (first strangled endpoint), then per the migration checklist: `PentePlayer.java`, `Game.java`, `LoginActivity.java`, `KingOfTheHillActivity.java`, `SettingsActivity.java`, `ReplyMessageActivity.java`, `SocialActivity.java`, `liveGameRoom/LobbyActivity.java`, FCM services, …

---

### Task 0: Add OkHttp dependency + shared `Json.GSON`

**Files:**
- Create: `/Users/waliedothman/mariposa/coding/pente.org-project/penteLive-Android/app/src/main/java/be/submanifold/pentelive/net/Json.java` (new file, new package `be.submanifold.pentelive.net`)
- Modify: `/Users/waliedothman/mariposa/coding/pente.org-project/penteLive-Android/app/build.gradle` (dependencies block, lines 76–94; insert one line after the gson line at 90)
- Test: `/Users/waliedothman/mariposa/coding/pente.org-project/penteLive-Android/app/src/test/java/be/submanifold/pentelive/net/JsonTest.java` (new file)

Version note: pin **`com.squareup.okhttp3:okhttp:4.12.0`** — the final 4.x release. It targets Java 8 bytecode (runtime min Java 8 / Android API 21+), matching this module's Java 8 baseline (no `compileOptions` override, minSdk 26). OkHttp 5.x raises the floor and is avoided so no toolchain bump is forced. It pulls `kotlin-stdlib` transitively, which is harmless for a Java-only consumer.

- [ ] **Step 1: Branch off main.** Run exactly:
  ```bash
  git -C /Users/waliedothman/mariposa/coding/pente.org-project/penteLive-Android checkout -b net-layer-strangler
  ```

- [ ] **Step 2: Add the OkHttp dependency.** In `app/build.gradle`, replace the gson line (line 90) so OkHttp follows it. Change:
  ```gradle
      implementation 'com.google.code.gson:gson:2.13.2'
  ```
  to:
  ```gradle
      implementation 'com.google.code.gson:gson:2.13.2'
      implementation 'com.squareup.okhttp3:okhttp:4.12.0'
  ```

- [ ] **Step 3: Verify OkHttp resolves (no native build needed).** Run exactly:
  ```bash
  ./gradlew :app:dependencies --configuration debugCompileClasspath | grep -i okhttp
  ```
  Expected: build succeeds and output includes `com.squareup.okhttp3:okhttp:4.12.0` (and transitively `okio` + `kotlin-stdlib`).

- [ ] **Step 4: Write the FAILING test (real code).** Create `app/src/test/java/be/submanifold/pentelive/net/JsonTest.java` with the full contents below. It references `be.submanifold.pentelive.net.Json.GSON`, which does not exist yet, so compilation must fail. It serializes the real `JsonModels.PlayerInfo` DTO (fields in declaration order: `name, color, showAds, subscriber, livePlayers, dbAccess, emailMe, onlineFollowing, personalizeAds`).
  ```java
  package be.submanifold.pentelive.net;

  import static org.junit.Assert.assertEquals;
  import static org.junit.Assert.assertNotNull;
  import static org.junit.Assert.assertSame;
  import static org.junit.Assert.assertTrue;

  import be.submanifold.pentelive.JsonModels;

  import com.google.gson.Gson;

  import org.junit.Test;

  public class JsonTest {

      @Test
      public void gsonIsANonNullSingleton() {
          assertNotNull(Json.GSON);
          assertTrue(Json.GSON instanceof Gson);
          assertSame(Json.GSON, Json.GSON);
      }

      @Test
      public void serializesPlayerInfoDtoInDeclarationOrder() {
          JsonModels.PlayerInfo info = new JsonModels.PlayerInfo();
          info.name = "rainwolf";
          info.color = 2;
          info.subscriber = true;
          info.livePlayers = 7;

          String json = Json.GSON.toJson(info);

          assertEquals(
              "{\"name\":\"rainwolf\",\"color\":2,\"showAds\":false,"
                  + "\"subscriber\":true,\"livePlayers\":7,\"dbAccess\":false,"
                  + "\"emailMe\":false,\"onlineFollowing\":0,\"personalizeAds\":false}",
              json);
      }

      @Test
      public void roundTripsPlayerInfoDto() {
          JsonModels.PlayerInfo info = new JsonModels.PlayerInfo();
          info.name = "alice";
          info.color = 1;
          info.subscriber = true;

          String json = Json.GSON.toJson(info);
          JsonModels.PlayerInfo back =
              Json.GSON.fromJson(json, JsonModels.PlayerInfo.class);

          assertEquals("alice", back.name);
          assertEquals(1, back.color);
          assertTrue(back.subscriber);
      }
  }
  ```

- [ ] **Step 5: Run the test, expect RED.** Run exactly:
  ```bash
  ./gradlew :app:testDebugUnitTest --tests "be.submanifold.pentelive.net.JsonTest"
  ```
  Expected: **FAIL** with a compilation error such as `error: package be.submanifold.pentelive.net does not exist` / `cannot find symbol: variable GSON` (the `Json` class is not yet created).

- [ ] **Step 6: Create the minimal REAL implementation.** Create `app/src/main/java/be/submanifold/pentelive/net/Json.java` with exactly:
  ```java
  package be.submanifold.pentelive.net;

  import com.google.gson.Gson;

  /**
   * The single shared {@link Gson} instance for the whole app, replacing the
   * six inline {@code new Gson()} call sites (LobbyActivity, MainActivity,
   * KingOfTheHillActivity, PentePlayer, Game x2).
   */
  public final class Json {

      /** Shared, thread-safe Gson instance. Reuse this everywhere. */
      public static final Gson GSON = new Gson();

      private Json() {
          // No instances.
      }
  }
  ```

- [ ] **Step 7: Run the test, expect GREEN.** Run exactly:
  ```bash
  ./gradlew :app:testDebugUnitTest --tests "be.submanifold.pentelive.net.JsonTest"
  ```
  Expected: **PASS** — `BUILD SUCCESSFUL`, all 3 `JsonTest` methods green.

- [ ] **Step 8: Confirm main sources still compile with the new dependency.** Run exactly:
  ```bash
  ./gradlew :app:compileDebugJavaWithJavac
  ```
  Expected: `BUILD SUCCESSFUL` (this triggers `buildNative`; ensure `ndk.dir` is set in `local.properties` per project setup).

- [ ] **Step 9: Commit.** Run exactly:
  ```bash
  git -C /Users/waliedothman/mariposa/coding/pente.org-project/penteLive-Android add app/build.gradle app/src/main/java/be/submanifold/pentelive/net/Json.java app/src/test/java/be/submanifold/pentelive/net/JsonTest.java
  git -C /Users/waliedothman/mariposa/coding/pente.org-project/penteLive-Android commit -m "Add OkHttp 4.12.0 and shared Json.GSON net layer foundation

Introduce be.submanifold.pentelive.net package with a single shared Gson
instance to replace the six inline new Gson() call sites, and add the
OkHttp 4.12.0 dependency for the upcoming PenteApi strangler migration.

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
  ```

---

### Task 1: Result<T> + Reason + Failure (immutable network outcome type)

**Files:**
- Create: `/Users/waliedothman/mariposa/coding/pente.org-project/penteLive-Android/app/src/main/java/be/submanifold/pentelive/net/Result.java` (new file, ~46 lines)
- Test: `/Users/waliedothman/mariposa/coding/pente.org-project/penteLive-Android/app/src/test/java/be/submanifold/pentelive/net/ResultTest.java` (new file, ~58 lines)

Test infra already present: `testImplementation 'junit:junit:4.13.2'` (app/build.gradle line 93); existing unit tests live under `app/src/test/java/be/submanifold/pentelive/`. The package `be.submanifold.pentelive.net` does not exist yet and is created by this task.

- [ ] **Step 1: Branch off main (we are on the default branch).**
  ```bash
  cd /Users/waliedothman/mariposa/coding/pente.org-project/penteLive-Android && git checkout -b net-strangler-result
  ```

- [ ] **Step 2: Create the package dirs for source and test.**
  ```bash
  mkdir -p /Users/waliedothman/mariposa/coding/pente.org-project/penteLive-Android/app/src/main/java/be/submanifold/pentelive/net /Users/waliedothman/mariposa/coding/pente.org-project/penteLive-Android/app/src/test/java/be/submanifold/pentelive/net
  ```

- [ ] **Step 3: Write the failing test `ResultTest.java` with REAL code.** Create `/Users/waliedothman/mariposa/coding/pente.org-project/penteLive-Android/app/src/test/java/be/submanifold/pentelive/net/ResultTest.java`:
  ```java
  package be.submanifold.pentelive.net;

  import static org.junit.Assert.assertEquals;
  import static org.junit.Assert.assertFalse;
  import static org.junit.Assert.assertNull;
  import static org.junit.Assert.assertSame;
  import static org.junit.Assert.assertTrue;

  import be.submanifold.pentelive.net.Result.Failure;
  import be.submanifold.pentelive.net.Result.Reason;

  import org.junit.Test;

  public class ResultTest {

      @Test
      public void ok_carriesValue_andIsOk() {
          Result<String> r = Result.ok("hello");
          assertTrue(r.isOk());
          assertEquals("hello", r.value);
          assertNull(r.failure);
      }

      @Test
      public void fail_carriesFailure_andIsNotOk() {
          Throwable cause = new RuntimeException("boom");
          Failure f = new Failure(Reason.SERVER, 500, cause);
          Result<String> r = Result.fail(f);
          assertFalse(r.isOk());
          assertNull(r.value);
          assertSame(f, r.failure);
          assertEquals(Reason.SERVER, r.failure.reason);
          assertEquals(500, r.failure.httpCode);
          assertSame(cause, r.failure.cause);
      }

      @Test
      public void everyReason_isRepresentable() {
          Reason[] reasons = {
              Reason.NETWORK,
              Reason.AUTH_EXPIRED,
              Reason.INVALID_CREDENTIALS,
              Reason.SERVER,
              Reason.PARSE
          };
          assertEquals(5, Reason.values().length);
          for (Reason reason : reasons) {
              Result<Void> r = Result.fail(new Failure(reason, 0, null));
              assertFalse(r.isOk());
              assertEquals(reason, r.failure.reason);
          }
      }
  }
  ```

- [ ] **Step 4: Run the test and confirm it FAILS (RED — `Result` symbol does not exist, compilation error).**
  ```bash
  cd /Users/waliedothman/mariposa/coding/pente.org-project/penteLive-Android && ./gradlew :app:testDebugUnitTest --tests "be.submanifold.pentelive.net.ResultTest"
  ```
  Expected: BUILD FAILED with `error: cannot find symbol ... class Result` (test source does not compile because `Result.java` is absent).

- [ ] **Step 5: Write the minimal REAL implementation `Result.java`.** Create `/Users/waliedothman/mariposa/coding/pente.org-project/penteLive-Android/app/src/main/java/be/submanifold/pentelive/net/Result.java`:
  ```java
  package be.submanifold.pentelive.net;

  /**
   * Immutable outcome of a network operation: either a value ({@link #ok})
   * or a {@link Failure} ({@link #fail}). Exactly one of {@link #value} /
   * {@link #failure} is non-null for a failure; {@link #isOk()} keys off
   * {@code failure == null} so {@code ok(null)} (e.g. {@code Result<Void>})
   * is still considered ok.
   */
  public final class Result<T> {

      /** Categories of network failure, mapped to caller-visible handling. */
      public enum Reason {
          NETWORK,
          AUTH_EXPIRED,
          INVALID_CREDENTIALS,
          SERVER,
          PARSE
      }

      /** Immutable description of why an operation failed. */
      public static final class Failure {
          public final Reason reason;
          public final int httpCode;
          public final Throwable cause;

          public Failure(Reason reason, int httpCode, Throwable cause) {
              this.reason = reason;
              this.httpCode = httpCode;
              this.cause = cause;
          }
      }

      public final T value;
      public final Failure failure;

      private Result(T value, Failure failure) {
          this.value = value;
          this.failure = failure;
      }

      public boolean isOk() {
          return failure == null;
      }

      public static <T> Result<T> ok(T value) {
          return new Result<>(value, null);
      }

      public static <T> Result<T> fail(Failure failure) {
          return new Result<>(null, failure);
      }
  }
  ```

- [ ] **Step 6: Run the test and confirm it PASSES (GREEN).**
  ```bash
  cd /Users/waliedothman/mariposa/coding/pente.org-project/penteLive-Android && ./gradlew :app:testDebugUnitTest --tests "be.submanifold.pentelive.net.ResultTest"
  ```
  Expected: BUILD SUCCESSFUL, 3 tests passed.

- [ ] **Step 7: Verify the new source compiles cleanly against the app module.**
  ```bash
  cd /Users/waliedothman/mariposa/coding/pente.org-project/penteLive-Android && ./gradlew :app:compileDebugJavaWithJavac
  ```
  Expected: BUILD SUCCESSFUL.

- [ ] **Step 8: Commit both files.**
  ```bash
  cd /Users/waliedothman/mariposa/coding/pente.org-project/penteLive-Android && git add app/src/main/java/be/submanifold/pentelive/net/Result.java app/src/test/java/be/submanifold/pentelive/net/ResultTest.java && git commit -m "Add immutable Result<T> with Reason and Failure for net layer

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
  ```

---

### Task 2: Session + BaseUrlProvider (testable cookie store + single dev/prod flag)

**Files:**
- Modify: `/Users/waliedothman/mariposa/coding/pente.org-project/penteLive-Android/app/build.gradle` (lines 90-93, dependencies block)
- Create: `/Users/waliedothman/mariposa/coding/pente.org-project/penteLive-Android/app/src/main/java/be/submanifold/pentelive/net/Session.java`
- Create: `/Users/waliedothman/mariposa/coding/pente.org-project/penteLive-Android/app/src/main/java/be/submanifold/pentelive/net/InMemoryCookieJar.java`
- Create: `/Users/waliedothman/mariposa/coding/pente.org-project/penteLive-Android/app/src/main/java/be/submanifold/pentelive/net/BaseUrlProvider.java`
- Create: `/Users/waliedothman/mariposa/coding/pente.org-project/penteLive-Android/app/src/main/java/be/submanifold/pentelive/net/StaticBaseUrlProvider.java`
- Create: `/Users/waliedothman/mariposa/coding/pente.org-project/penteLive-Android/app/src/main/java/be/submanifold/pentelive/net/SharedPrefsSession.java`
- Test: `/Users/waliedothman/mariposa/coding/pente.org-project/penteLive-Android/app/src/test/java/be/submanifold/pentelive/net/FakeSession.java` (test double)
- Test: `/Users/waliedothman/mariposa/coding/pente.org-project/penteLive-Android/app/src/test/java/be/submanifold/pentelive/net/SessionTest.java`

Context being strangled: `LoginActivity.java:365` (raw `CookieManager`/`CookiePolicy.ACCEPT_ALL`), `PentePlayer.java:33` (`public static Boolean development = false`), `PentePlayer.java:35-36` (`mPlayerName`/`mPassword` statics), `PentePlayer.java:650-651` + `Game.java:398-413` (per-call `if (development)` URL rebuild + manual `name2`/`password2` cookie string assembly). This task introduces the testable replacements; callers are migrated in later tasks.

- [ ] **Step 1: Add OkHttp dependency (prerequisite for `okhttp3.CookieJar` in the Session contract).**
  In `app/build.gradle`, change the dependencies block. Replace:
  ```gradle
      implementation 'com.google.code.gson:gson:2.13.2'
      implementation 'com.github.kobakei:Android-RateThisApp:1.2.0'
      implementation 'com.android.support:multidex:1.0.3'
      testImplementation 'junit:junit:4.13.2'
  ```
  with:
  ```gradle
      implementation 'com.google.code.gson:gson:2.13.2'
      implementation 'com.github.kobakei:Android-RateThisApp:1.2.0'
      implementation 'com.android.support:multidex:1.0.3'
      implementation 'com.squareup.okhttp3:okhttp:4.12.0'
      testImplementation 'junit:junit:4.13.2'
  ```

- [ ] **Step 2: Verify the dependency resolves.**
  Run: `./gradlew :app:compileDebugJavaWithJavac`
  Expected: PASS (BUILD SUCCESSFUL; OkHttp 4.12.0 downloaded/resolved, no source changes yet).

- [ ] **Step 3: Create the in-memory Session test double `FakeSession.java` (test source).** It references production types that do not exist yet — this is intentional for the RED run.
  ```java
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
  ```

- [ ] **Step 4: Write the failing test `SessionTest.java` (test source).**
  ```java
  package be.submanifold.pentelive.net;

  import static org.junit.Assert.assertEquals;
  import static org.junit.Assert.assertTrue;

  import java.util.Collections;
  import java.util.List;

  import okhttp3.Cookie;
  import okhttp3.HttpUrl;

  import org.junit.Test;

  public class SessionTest {

      private static final HttpUrl PENTE = HttpUrl.get("https://www.pente.org/");

      @Test
      public void cookieJar_persistsSetCookie_andReplaysIt() {
          InMemoryCookieJar jar = new InMemoryCookieJar();

          Cookie setCookie = Cookie.parse(PENTE, "JSESSIONID=abc123; domain=pente.org; path=/");
          jar.saveFromResponse(PENTE, Collections.singletonList(setCookie));

          List<Cookie> replayed = jar.loadForRequest(PENTE);

          assertEquals(1, replayed.size());
          assertEquals("JSESSIONID", replayed.get(0).name());
          assertEquals("abc123", replayed.get(0).value());
      }

      @Test
      public void cookieJar_returnsEmpty_forUnknownHost() {
          InMemoryCookieJar jar = new InMemoryCookieJar();
          List<Cookie> replayed = jar.loadForRequest(HttpUrl.get("https://example.com/"));
          assertTrue(replayed.isEmpty());
      }

      @Test
      public void fakeSession_replaysCookies_throughCookieJar() {
          FakeSession session = new FakeSession("alice", "secret");

          Cookie setCookie = Cookie.parse(PENTE, "name2=alice; domain=pente.org; path=/");
          session.cookieJar().saveFromResponse(PENTE, Collections.singletonList(setCookie));

          List<Cookie> replayed = session.cookieJar().loadForRequest(PENTE);

          assertEquals(1, replayed.size());
          assertEquals("alice", replayed.get(0).value());
      }

      @Test
      public void session_updateCredentials_changesNameAndPassword() {
          FakeSession session = new FakeSession("alice", "secret");
          assertEquals("alice", session.name());
          assertEquals("secret", session.password());

          session.updateCredentials("bob", "hunter2");

          assertEquals("bob", session.name());
          assertEquals("hunter2", session.password());
      }

      @Test
      public void baseUrlProvider_returnsProd_whenNotDevelopment() {
          BaseUrlProvider provider = new StaticBaseUrlProvider(false);
          assertEquals("https://www.pente.org", provider.baseUrl());
      }

      @Test
      public void baseUrlProvider_returnsEmulatorHost_whenDevelopment() {
          BaseUrlProvider provider = new StaticBaseUrlProvider(true);
          assertEquals("https://10.0.2.2", provider.baseUrl());
      }
  }
  ```

- [ ] **Step 5: Run the test and confirm it FAILS.**
  Run: `./gradlew :app:testDebugUnitTest --tests "be.submanifold.pentelive.net.SessionTest"`
  Expected: FAIL — compilation error `cannot find symbol` for `Session`, `InMemoryCookieJar`, `BaseUrlProvider`, `StaticBaseUrlProvider` (production classes not created yet).

- [ ] **Step 6: Create the `Session` interface `Session.java` (main source).**
  ```java
  package be.submanifold.pentelive.net;

  import okhttp3.CookieJar;

  /**
   * Holds the authenticated identity (name/password) plus the cookie store used
   * to persist and replay the server session cookie. Backed by SharedPreferences
   * in production ({@link SharedPrefsSession}) or in-memory in tests.
   */
  public interface Session {

      String name();

      String password();

      CookieJar cookieJar();

      void updateCredentials(String name, String password);
  }
  ```

- [ ] **Step 7: Create `InMemoryCookieJar.java` (main source) — persists every Set-Cookie and replays still-valid matching cookies.**
  ```java
  package be.submanifold.pentelive.net;

  import java.util.ArrayList;
  import java.util.Collections;
  import java.util.LinkedHashMap;
  import java.util.List;
  import java.util.Map;

  import okhttp3.Cookie;
  import okhttp3.CookieJar;
  import okhttp3.HttpUrl;

  /**
   * Thread-safe in-memory CookieJar. Stores each response cookie keyed by
   * name+path per host and replays the non-expired, matching cookies on later
   * requests. Pure JVM (no Android deps) so it is unit-testable. Replaces the
   * manual name2/password2 cookie-string assembly in PentePlayer/Game.
   */
  public final class InMemoryCookieJar implements CookieJar {

      /** host -> (name\u0000path -> cookie) */
      private final Map<String, Map<String, Cookie>> store = new LinkedHashMap<>();

      @Override
      public synchronized void saveFromResponse(HttpUrl url, List<Cookie> cookies) {
          Map<String, Cookie> hostCookies = store.get(url.host());
          if (hostCookies == null) {
              hostCookies = new LinkedHashMap<>();
              store.put(url.host(), hostCookies);
          }
          for (Cookie cookie : cookies) {
              hostCookies.put(cookie.name() + "\u0000" + cookie.path(), cookie);
          }
      }

      @Override
      public synchronized List<Cookie> loadForRequest(HttpUrl url) {
          Map<String, Cookie> hostCookies = store.get(url.host());
          if (hostCookies == null) {
              return Collections.emptyList();
          }
          long now = System.currentTimeMillis();
          List<Cookie> result = new ArrayList<>();
          for (Cookie cookie : hostCookies.values()) {
              if (cookie.expiresAt() < now) {
                  continue;
              }
              if (cookie.matches(url)) {
                  result.add(cookie);
              }
          }
          return result;
      }
  }
  ```

- [ ] **Step 8: Create `BaseUrlProvider.java` (main source).**
  ```java
  package be.submanifold.pentelive.net;

  /** Supplies the resolved server base URL (scheme + host, no trailing slash). */
  public interface BaseUrlProvider {
      String baseUrl();
  }
  ```

- [ ] **Step 9: Create `StaticBaseUrlProvider.java` (main source) — resolves dev/prod ONCE from a single flag.**
  ```java
  package be.submanifold.pentelive.net;

  /**
   * Resolves the base URL exactly once at construction from a single dev/prod
   * flag, replacing the scattered per-call {@code if (development)} branches in
   * PentePlayer.java:650-651 and Game.java:398-401.
   */
  public final class StaticBaseUrlProvider implements BaseUrlProvider {

      public static final String PROD_BASE_URL = "https://www.pente.org";
      public static final String DEV_BASE_URL = "https://10.0.2.2";

      private final String baseUrl;

      public StaticBaseUrlProvider(boolean development) {
          this.baseUrl = development ? DEV_BASE_URL : PROD_BASE_URL;
      }

      @Override
      public String baseUrl() {
          return baseUrl;
      }
  }
  ```

- [ ] **Step 10: Run the test and confirm it PASSES.**
  Run: `./gradlew :app:testDebugUnitTest --tests "be.submanifold.pentelive.net.SessionTest"`
  Expected: PASS — all 6 tests green (BUILD SUCCESSFUL).

- [ ] **Step 11: Create the production Android impl `SharedPrefsSession.java` (main source) — creds from `PrefUtils`.** Not unit-tested (needs Android `Context`); validated by compilation. `PrefUtils.getFromPrefs(Context, String, String)` returns the stored value or the default; `PrefUtils.saveToPrefs(Context, String, String)` writes it.
  ```java
  package be.submanifold.pentelive.net;

  import android.content.Context;

  import be.submanifold.pentelive.PrefUtils;

  import okhttp3.CookieJar;

  /**
   * Production Session backed by SharedPreferences via {@link PrefUtils}.
   * Credentials are read from (and credential updates written back to) the
   * PREFS_LOGIN_USERNAME_KEY / PREFS_LOGIN_PASSWORD_KEY entries, replacing the
   * PentePlayer.mPlayerName/mPassword statics (PentePlayer.java:35-36). The
   * cookie store lives in memory for the process lifetime.
   */
  public final class SharedPrefsSession implements Session {

      private final Context appContext;
      private final InMemoryCookieJar cookieJar = new InMemoryCookieJar();

      public SharedPrefsSession(Context context) {
          this.appContext = context.getApplicationContext();
      }

      @Override
      public String name() {
          return PrefUtils.getFromPrefs(appContext, PrefUtils.PREFS_LOGIN_USERNAME_KEY, "");
      }

      @Override
      public String password() {
          return PrefUtils.getFromPrefs(appContext, PrefUtils.PREFS_LOGIN_PASSWORD_KEY, "");
      }

      @Override
      public CookieJar cookieJar() {
          return cookieJar;
      }

      @Override
      public void updateCredentials(String name, String password) {
          PrefUtils.saveToPrefs(appContext, PrefUtils.PREFS_LOGIN_USERNAME_KEY, name);
          PrefUtils.saveToPrefs(appContext, PrefUtils.PREFS_LOGIN_PASSWORD_KEY, password);
      }
  }
  ```

- [ ] **Step 12: Compile the full debug source set to verify the Android impl wires up.**
  Run: `./gradlew :app:compileDebugJavaWithJavac`
  Expected: PASS (BUILD SUCCESSFUL; `SharedPrefsSession`, `Session`, `InMemoryCookieJar`, `BaseUrlProvider`, `StaticBaseUrlProvider` all compile against the `:app` classpath).

- [ ] **Step 13: Commit.**
  Run: `git add app/build.gradle app/src/main/java/be/submanifold/pentelive/net app/src/test/java/be/submanifold/pentelive/net && git commit -m "Add testable Session + BaseUrlProvider for net strangle (Task 2)" -m "Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"`

---

### Task 3 — PenteApi interface + FakePenteApi (first method slice)

**Files:**
- Create: `/Users/waliedothman/mariposa/coding/pente.org-project/penteLive-Android/app/src/main/java/be/submanifold/pentelive/net/WhosOnline.java` (new, ~20 lines)
- Create: `/Users/waliedothman/mariposa/coding/pente.org-project/penteLive-Android/app/src/main/java/be/submanifold/pentelive/net/PenteApi.java` (new, ~30 lines)
- Create: `/Users/waliedothman/mariposa/coding/pente.org-project/penteLive-Android/app/src/main/java/be/submanifold/pentelive/net/FakePenteApi.java` (new, ~80 lines)
- Test: `/Users/waliedothman/mariposa/coding/pente.org-project/penteLive-Android/app/src/test/java/be/submanifold/pentelive/net/FakePenteApiTest.java` (new, ~120 lines)
- Read (context, DO NOT modify): `app/src/main/java/be/submanifold/pentelive/JsonModels.java:121-178` (`GameResponse`, `RoomEntry`, `OnlinePlayerEntry` DTOs reused below) and `MainActivity.java:531-585` (whos-online endpoint shape: GET `mobile/json/whosonlineandlive.jsp` → `List<JsonModels.RoomEntry>`).
- Depends on Task 1 types in `be.submanifold.pentelive.net`: `Result<T>` (`isOk()`, `ok(v)`, `fail(Failure)`, `public final T value`, `public final Failure failure`), nested `Result.Reason` enum (`NETWORK`, `AUTH_EXPIRED`, `INVALID_CREDENTIALS`, `SERVER`, `PARSE`), and `static final class Result.Failure` with constructor `Failure(Reason reason, int httpCode, Throwable cause)`.

Steps (strict TDD):

- [ ] **Step 1: Write the failing test `FakePenteApiTest.java` (REAL code, full file).** Create `/Users/waliedothman/mariposa/coding/pente.org-project/penteLive-Android/app/src/test/java/be/submanifold/pentelive/net/FakePenteApiTest.java`:
```java
package be.submanifold.pentelive.net;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import be.submanifold.pentelive.JsonModels;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FakePenteApiTest {

    @Test
    public void recordsCallsInOrderWithArguments() {
        FakePenteApi api = new FakePenteApi();

        api.whosOnline();
        api.avatar("bob");
        api.login("alice", "secret");
        api.loadGame("42");
        api.submitMove("42", "j10", "good luck");

        assertEquals(
                Arrays.asList(
                        "whosOnline",
                        "avatar:bob",
                        "login:alice",
                        "loadGame:42",
                        "submitMove:42|j10|good luck"),
                api.calls);
    }

    @Test
    public void returnsCannedWhosOnline() {
        FakePenteApi api = new FakePenteApi();
        List<JsonModels.RoomEntry> rooms = new ArrayList<>();
        JsonModels.RoomEntry room = new JsonModels.RoomEntry();
        room.name = "Main";
        rooms.add(room);
        WhosOnline canned = new WhosOnline(rooms);
        api.whosOnlineResponse = canned;

        Result<WhosOnline> result = api.whosOnline();

        assertTrue(result.isOk());
        assertSame(canned, result.value);
        assertEquals(1, result.value.rooms.size());
        assertEquals("Main", result.value.rooms.get(0).name);
    }

    @Test
    public void whosOnlineNeverReturnsNullRooms() {
        FakePenteApi api = new FakePenteApi();
        api.whosOnlineResponse = new WhosOnline(null);

        Result<WhosOnline> result = api.whosOnline();

        assertTrue(result.isOk());
        assertTrue(result.value.rooms.isEmpty());
    }

    @Test
    public void returnsCannedGameKeyedByGid() {
        FakePenteApi api = new FakePenteApi();
        JsonModels.GameResponse game = new JsonModels.GameResponse();
        game.gid = "42";
        game.moves = "j10k11";
        api.games.put("42", game);

        Result<JsonModels.GameResponse> result = api.loadGame("42");

        assertTrue(result.isOk());
        assertSame(game, result.value);
        assertEquals("j10k11", result.value.moves);
    }

    @Test
    public void loginDefaultsToTrueWhenNoCannedValue() {
        FakePenteApi api = new FakePenteApi();

        Result<Boolean> result = api.login("alice", "secret");

        assertTrue(result.isOk());
        assertEquals(Boolean.TRUE, result.value);
    }

    @Test
    public void loginReturnsCannedFalse() {
        FakePenteApi api = new FakePenteApi();
        api.logins.put("alice", false);

        Result<Boolean> result = api.login("alice", "wrong");

        assertTrue(result.isOk());
        assertEquals(Boolean.FALSE, result.value);
    }

    @Test
    public void submitMoveReturnsOkVoidAndRecordsArgs() {
        FakePenteApi api = new FakePenteApi();

        Result<Void> result = api.submitMove("7", "m13", "");

        assertTrue(result.isOk());
        assertNull(result.value);
        assertEquals(Arrays.asList("submitMove:7|m13|"), api.calls);
    }

    @Test
    public void nextFailureForcesSingleFailureThenRecovers() {
        FakePenteApi api = new FakePenteApi();
        api.nextFailure = Result.Reason.NETWORK;

        Result<WhosOnline> failed = api.whosOnline();
        assertFalse(failed.isOk());
        assertEquals(Result.Reason.NETWORK, failed.failure.reason);
        assertNull(failed.value);

        // nextFailure is single-shot: the following call succeeds.
        Result<WhosOnline> ok = api.whosOnline();
        assertTrue(ok.isOk());

        assertEquals(Arrays.asList("whosOnline", "whosOnline"), api.calls);
    }
}
```

- [ ] **Step 2: Run the test and confirm RED (compile failure).** Run exactly:
```bash
./gradlew :app:testDebugUnitTest --tests "be.submanifold.pentelive.net.FakePenteApiTest"
```
Expected: **FAIL** — `compileDebugUnitTestJavaWithJavac` errors `cannot find symbol: class FakePenteApi`, `class PenteApi`, `class WhosOnline` (Result/Result.Reason already resolve from Task 1).

- [ ] **Step 3: Create `WhosOnline.java` (REAL code, full file).** Create `/Users/waliedothman/mariposa/coding/pente.org-project/penteLive-Android/app/src/main/java/be/submanifold/pentelive/net/WhosOnline.java`:
```java
package be.submanifold.pentelive.net;

import be.submanifold.pentelive.JsonModels;

import java.util.Collections;
import java.util.List;

/**
 * Typed result of GET mobile/json/whosonlineandlive.jsp. Wraps the raw
 * List&lt;JsonModels.RoomEntry&gt; produced by that endpoint (see
 * MainActivity#LoadWhosOnlineTask, lines 531-585). Never holds a null list.
 */
public final class WhosOnline {

    public final List<JsonModels.RoomEntry> rooms;

    public WhosOnline(List<JsonModels.RoomEntry> rooms) {
        this.rooms = rooms == null
                ? Collections.<JsonModels.RoomEntry>emptyList()
                : rooms;
    }
}
```

- [ ] **Step 4: Create `PenteApi.java` (REAL code, full file).** Create `/Users/waliedothman/mariposa/coding/pente.org-project/penteLive-Android/app/src/main/java/be/submanifold/pentelive/net/PenteApi.java`:
```java
package be.submanifold.pentelive.net;

import android.graphics.Bitmap;

import be.submanifold.pentelive.JsonModels;

/**
 * Synchronous, thread-agnostic pente.org HTTP API. Implementations MUST be safe to call
 * from a background thread and MUST NOT touch the UI thread or an Android Looper. Every
 * method returns a {@link Result} that is either {@code ok(value)} or {@code fail(Failure)};
 * methods never throw for expected network/auth/parse conditions.
 *
 * <p>First strangle slice: whos-online, avatar, login, single turn-based game load, move submit.
 */
public interface PenteApi {

    /** GET mobile/json/whosonlineandlive.jsp. */
    Result<WhosOnline> whosOnline();

    /** GET the avatar image for {@code name}; PARSE failure if the bytes are not a decodable image. */
    Result<Bitmap> avatar(String name);

    /** Authenticate; {@code ok(true)} on success, {@code fail(INVALID_CREDENTIALS)} on bad credentials. */
    Result<Boolean> login(String name, String password);

    /** GET mobile/json/game.jsp?gid=... for one turn-based game. */
    Result<JsonModels.GameResponse> loadGame(String gid);

    /** POST a move (and optional chat {@code message}) for {@code gid}. */
    Result<Void> submitMove(String gid, String moves, String message);
}
```

- [ ] **Step 5: Create `FakePenteApi.java` (REAL code, full file).** Create `/Users/waliedothman/mariposa/coding/pente.org-project/penteLive-Android/app/src/main/java/be/submanifold/pentelive/net/FakePenteApi.java`:
```java
package be.submanifold.pentelive.net;

import android.graphics.Bitmap;

import be.submanifold.pentelive.JsonModels;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * In-memory {@link PenteApi} for unit tests. Records every call (with arguments) into
 * {@link #calls}, returns canned Results from the per-method fields/maps, and can force the
 * NEXT call to fail by setting {@link #nextFailure} (single-shot: consumed on use).
 */
public final class FakePenteApi implements PenteApi {

    /**
     * Ordered log of calls, e.g. "whosOnline", "avatar:bob", "login:alice",
     * "loadGame:42", "submitMove:42|j10|good luck".
     */
    public final List<String> calls = new ArrayList<>();

    /** Canned responses. Map keys: avatar/login by name, game by gid. */
    public WhosOnline whosOnlineResponse;
    public final Map<String, Bitmap> avatars = new HashMap<>();
    public final Map<String, Boolean> logins = new HashMap<>();
    public final Map<String, JsonModels.GameResponse> games = new HashMap<>();

    /** When non-null, the next call returns {@code fail(reason)} and this field is reset to null. */
    public Result.Reason nextFailure;

    @Override
    public Result<WhosOnline> whosOnline() {
        calls.add("whosOnline");
        if (nextFailure != null) {
            return consumeFailure();
        }
        return Result.ok(whosOnlineResponse);
    }

    @Override
    public Result<Bitmap> avatar(String name) {
        calls.add("avatar:" + name);
        if (nextFailure != null) {
            return consumeFailure();
        }
        return Result.ok(avatars.get(name));
    }

    @Override
    public Result<Boolean> login(String name, String password) {
        calls.add("login:" + name);
        if (nextFailure != null) {
            return consumeFailure();
        }
        Boolean canned = logins.get(name);
        return Result.ok(canned != null ? canned : Boolean.TRUE);
    }

    @Override
    public Result<JsonModels.GameResponse> loadGame(String gid) {
        calls.add("loadGame:" + gid);
        if (nextFailure != null) {
            return consumeFailure();
        }
        return Result.ok(games.get(gid));
    }

    @Override
    public Result<Void> submitMove(String gid, String moves, String message) {
        calls.add("submitMove:" + gid + "|" + moves + "|" + message);
        if (nextFailure != null) {
            return consumeFailure();
        }
        return Result.<Void>ok(null);
    }

    private <T> Result<T> consumeFailure() {
        Result.Reason reason = nextFailure;
        nextFailure = null;
        return Result.fail(new Result.Failure(reason, 0, null));
    }
}
```

- [ ] **Step 6: Run the test and confirm GREEN.** Run exactly:
```bash
./gradlew :app:testDebugUnitTest --tests "be.submanifold.pentelive.net.FakePenteApiTest"
```
Expected: **PASS** — `BUILD SUCCESSFUL`, all 8 `FakePenteApiTest` cases green.

- [ ] **Step 7: Verify main-source compiles (no test-only leakage).** Run exactly:
```bash
./gradlew :app:compileDebugJavaWithJavac
```
Expected: **PASS** — `BUILD SUCCESSFUL`; `PenteApi`, `FakePenteApi`, and `WhosOnline` compile against existing `JsonModels`/`Result`/`android.graphics.Bitmap`.

- [ ] **Step 8: Commit.** Run exactly:
```bash
git add app/src/main/java/be/submanifold/pentelive/net/WhosOnline.java \
        app/src/main/java/be/submanifold/pentelive/net/PenteApi.java \
        app/src/main/java/be/submanifold/pentelive/net/FakePenteApi.java \
        app/src/test/java/be/submanifold/pentelive/net/FakePenteApiTest.java
git commit -m "Add PenteApi interface and FakePenteApi test double (first slice)

Define synchronous PenteApi (whosOnline/avatar/login/loadGame/submitMove)
returning Result<T>, the WhosOnline DTO wrapping List<JsonModels.RoomEntry>,
and an in-memory FakePenteApi that records calls, returns canned responses,
and forces single-shot failures via nextFailure.

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

### Task 4: OkHttpPenteApi real adapter + single-flight Authenticator (the prod credential-drop fix)

**Files:**
- Modify: `/Users/waliedothman/mariposa/coding/pente.org-project/penteLive-Android/app/build.gradle` (dependencies block, lines 90-93 — add OkHttp impl if absent + MockWebServer test dep)
- Create: `/Users/waliedothman/mariposa/coding/pente.org-project/penteLive-Android/app/src/main/java/be/submanifold/pentelive/net/OkHttpPenteApi.java` (new, ~190 lines)
- Test: `/Users/waliedothman/mariposa/coding/pente.org-project/penteLive-Android/app/src/test/java/be/submanifold/pentelive/net/OkHttpPenteApiTest.java` (new)

> Depends on Tasks 1-3 in package `be.submanifold.pentelive.net`: `Result<T>` (with nested `Result.Failure`, `Result.Reason`, `Result.ok`, `Result.fail`, `isOk()`), `Session`, `BaseUrlProvider` (functional, `String baseUrl()`), `Json.GSON`, `PenteApi` (the 5 typed methods), and `WhosOnline` (wrapper with `public final java.util.List<JsonModels.RoomEntry> rooms;` and ctor `WhosOnline(List<JsonModels.RoomEntry>)`). Legacy endpoints quoted from real source: whos-online `gameServer/mobile/json/whosonlineandlive.jsp` (`MainActivity.java:531`), login `gameServer/login.jsp?mobile=&name2=&password2=` (`PentePlayer.java:668`), the dropped-credentials bug `index.jsp?name=&password=` with NO name2/password2 (`PentePlayer.java:678`), loadGame `gameServer/mobile/json/game.jsp` (`Game.java:396`), avatar `gameServer/avatar?name=` (`PentePlayer.java:594`), submitMove `gameServer/tb/game?command=move&...&moves=&message=` (`Game.java:498`). OkHttp's default TLS uses the platform `SSLSocketFactory`, which honors `res/xml/network_security_configuration.xml` (ISRG roots) automatically — no custom factory needed.

- [ ] **Step 1: Add dependencies.** In `app/build.gradle`, current lines are:
  ```gradle
      implementation 'com.google.code.gson:gson:2.13.2'
      implementation 'com.github.kobakei:Android-RateThisApp:1.2.0'
      implementation 'com.android.support:multidex:1.0.3'
      testImplementation 'junit:junit:4.13.2'
  ```
  If `com.squareup.okhttp3:okhttp` is NOT already declared (it was introduced in Task 2 for `Session.cookieJar()` — verify with `grep -n okhttp app/build.gradle`), add it after the gson line, then always add the MockWebServer test dep after the junit line, producing:
  ```gradle
      implementation 'com.google.code.gson:gson:2.13.2'
      implementation 'com.squareup.okhttp3:okhttp:4.12.0'
      implementation 'com.github.kobakei:Android-RateThisApp:1.2.0'
      implementation 'com.android.support:multidex:1.0.3'
      testImplementation 'junit:junit:4.13.2'
      testImplementation 'com.squareup.okhttp3:mockwebserver:4.12.0'
  ```

- [ ] **Step 2: Create the test package dir.** Run:
  ```bash
  mkdir -p /Users/waliedothman/mariposa/coding/pente.org-project/penteLive-Android/app/src/test/java/be/submanifold/pentelive/net
  ```

- [ ] **Step 3: Write the failing test (real code).** Create `app/src/test/java/be/submanifold/pentelive/net/OkHttpPenteApiTest.java`:
  ```java
  package be.submanifold.pentelive.net;

  import static org.junit.Assert.assertEquals;
  import static org.junit.Assert.assertTrue;

  import java.util.ArrayList;
  import java.util.Collections;
  import java.util.List;
  import java.util.concurrent.CountDownLatch;
  import java.util.concurrent.ExecutorService;
  import java.util.concurrent.Executors;
  import java.util.concurrent.Future;
  import java.util.concurrent.atomic.AtomicBoolean;
  import java.util.concurrent.atomic.AtomicInteger;

  import okhttp3.CookieJar;
  import okhttp3.HttpUrl;
  import okhttp3.mockwebserver.Dispatcher;
  import okhttp3.mockwebserver.MockResponse;
  import okhttp3.mockwebserver.MockWebServer;
  import okhttp3.mockwebserver.RecordedRequest;

  import org.junit.After;
  import org.junit.Before;
  import org.junit.Test;

  public class OkHttpPenteApiTest {

      private MockWebServer server;
      private OkHttpPenteApi api;

      private static final class TestSession implements Session {
          @Override public String name() { return "alice"; }
          @Override public String password() { return "secret"; }
          @Override public CookieJar cookieJar() { return CookieJar.NO_COOKIES; }
          @Override public void updateCredentials(String name, String password) { /* no-op */ }
      }

      @Before
      public void setUp() throws Exception {
          server = new MockWebServer();
          server.start();
          String base = server.url("/").toString();
          final String baseUrl = base.substring(0, base.length() - 1); // drop trailing '/'
          BaseUrlProvider urls = () -> baseUrl;
          api = new OkHttpPenteApi(new TestSession(), urls);
      }

      @After
      public void tearDown() throws Exception {
          server.shutdown();
      }

      /** (a) N concurrent AUTH_EXPIRED responses trigger exactly ONE re-login. */
      @Test
      public void concurrentAuthExpired_triggersExactlyOneReLogin() throws Exception {
          final AtomicInteger loginHits = new AtomicInteger(0);
          final AtomicBoolean authed = new AtomicBoolean(false);
          server.setDispatcher(new Dispatcher() {
              @Override public MockResponse dispatch(RecordedRequest request) {
                  String path = request.getRequestUrl().encodedPath();
                  if (path.endsWith("/gameServer/login.jsp")) {
                      loginHits.incrementAndGet();
                      authed.set(true);
                      return new MockResponse().setResponseCode(200).setBody("ok");
                  }
                  if (path.endsWith("/whosonlineandlive.jsp")) {
                      if (!authed.get()) {
                          return new MockResponse().setResponseCode(401);
                      }
                      return new MockResponse().setResponseCode(200).setBody("[]");
                  }
                  return new MockResponse().setResponseCode(404);
              }
          });

          final int n = 6;
          ExecutorService pool = Executors.newFixedThreadPool(n);
          final CountDownLatch start = new CountDownLatch(1);
          List<Future<Result<WhosOnline>>> futures = new ArrayList<>();
          for (int i = 0; i < n; i++) {
              futures.add(pool.submit(() -> {
                  start.await();
                  return api.whosOnline();
              }));
          }
          start.countDown(); // release all threads simultaneously
          for (Future<Result<WhosOnline>> f : futures) {
              assertTrue("each concurrent call must succeed after re-auth", f.get().isOk());
          }
          pool.shutdown();

          assertEquals("exactly one re-login despite N concurrent 401s", 1, loginHits.get());
      }

      /** (b) credential-drop regression: the retried request still carries name2/password2. */
      @Test
      public void reAuthRetry_keepsName2AndPassword2() throws Exception {
          final List<HttpUrl> dataRequests = Collections.synchronizedList(new ArrayList<>());
          final AtomicBoolean authed = new AtomicBoolean(false);
          server.setDispatcher(new Dispatcher() {
              @Override public MockResponse dispatch(RecordedRequest request) {
                  HttpUrl u = request.getRequestUrl();
                  String path = u.encodedPath();
                  if (path.endsWith("/gameServer/login.jsp")) {
                      authed.set(true);
                      return new MockResponse().setResponseCode(200).setBody("ok");
                  }
                  if (path.endsWith("/whosonlineandlive.jsp")) {
                      dataRequests.add(u);
                      if (!authed.get()) {
                          return new MockResponse().setResponseCode(401);
                      }
                      return new MockResponse().setResponseCode(200).setBody("[]");
                  }
                  return new MockResponse().setResponseCode(404);
              }
          });

          Result<WhosOnline> result = api.whosOnline();

          assertTrue("call succeeds after single re-auth retry", result.isOk());
          assertEquals("initial 401 + one retry = two data requests", 2, dataRequests.size());
          HttpUrl retry = dataRequests.get(1);
          assertEquals("name2 preserved on retry (regression for PentePlayer.java:678)",
                  "alice", retry.queryParameter("name2"));
          assertEquals("password2 preserved on retry",
                  "secret", retry.queryParameter("password2"));
      }
  }
  ```

- [ ] **Step 4: Run the test — expect FAIL (red).** Run:
  ```bash
  ./gradlew :app:testDebugUnitTest --tests "be.submanifold.pentelive.net.OkHttpPenteApiTest"
  ```
  Expected: compilation failure `error: cannot find symbol ... class OkHttpPenteApi` (the production class does not exist yet). This is the red state.

- [ ] **Step 5: Implement `OkHttpPenteApi` (minimal real code).** Create `app/src/main/java/be/submanifold/pentelive/net/OkHttpPenteApi.java`:
  ```java
  package be.submanifold.pentelive.net;

  import android.graphics.Bitmap;
  import android.graphics.BitmapFactory;

  import be.submanifold.pentelive.JsonModels;

  import com.google.gson.reflect.TypeToken;

  import java.io.IOException;
  import java.io.InputStream;
  import java.lang.reflect.Type;
  import java.util.List;

  import okhttp3.Authenticator;
  import okhttp3.HttpUrl;
  import okhttp3.OkHttpClient;
  import okhttp3.Request;
  import okhttp3.Response;
  import okhttp3.ResponseBody;
  import okhttp3.Route;

  /**
   * Synchronous, thread-agnostic OkHttp implementation of {@link PenteApi}.
   * One shared OkHttpClient carries the {@link Session} cookie jar and a
   * single-flight re-auth {@link Authenticator}. Default TLS honors
   * res/xml/network_security_configuration.xml automatically.
   */
  public final class OkHttpPenteApi implements PenteApi {

      private final Session session;
      private final BaseUrlProvider urls;
      private final OkHttpClient client;
      private final ReAuthenticator authenticator;

      public OkHttpPenteApi(Session session, BaseUrlProvider urls) {
          this.session = session;
          this.urls = urls;
          // Dedicated client WITHOUT the authenticator performs the re-login,
          // so a 401 on login.jsp can never recurse into authenticate().
          OkHttpClient loginClient = new OkHttpClient.Builder()
                  .cookieJar(session.cookieJar())
                  .build();
          this.authenticator = new ReAuthenticator(session, urls, loginClient);
          // Main client shares the same cookie jar (cookies set during re-login
          // are visible) and installs the single-flight authenticator.
          this.client = new OkHttpClient.Builder()
                  .cookieJar(session.cookieJar())
                  .authenticator(authenticator)
                  .build();
      }

      private interface Parser<T> { T parse(String body) throws Exception; }

      private HttpUrl.Builder base() {
          return HttpUrl.parse(urls.baseUrl()).newBuilder();
      }

      /** GET stamped with the live auth generation so cross-wave single-flight works. */
      private Request authedGet(HttpUrl url) {
          return new Request.Builder()
                  .url(url)
                  .header(ReAuthenticator.GEN_HEADER,
                          Integer.toString(authenticator.currentGeneration()))
                  .get()
                  .build();
      }

      private <T> Result<T> exchange(HttpUrl url, Parser<T> parser) {
          try (Response response = client.newCall(authedGet(url)).execute()) {
              int code = response.code();
              if (code == 401 || code == 403) {
                  // Reached only after the authenticator exhausted its single retry.
                  return Result.fail(new Result.Failure(Result.Reason.AUTH_EXPIRED, code, null));
              }
              if (code != 200) {
                  return Result.fail(new Result.Failure(Result.Reason.SERVER, code, null));
              }
              ResponseBody body = response.body();
              String text = body == null ? "" : body.string();
              try {
                  T value = parser.parse(text);
                  if (value == null) {
                      return Result.fail(new Result.Failure(Result.Reason.PARSE, code, null));
                  }
                  return Result.ok(value);
              } catch (Exception e) {
                  return Result.fail(new Result.Failure(Result.Reason.PARSE, code, e));
              }
          } catch (IOException e) {
              return Result.fail(new Result.Failure(Result.Reason.NETWORK, 0, e));
          }
      }

      @Override
      public Result<WhosOnline> whosOnline() {
          HttpUrl url = base()
                  .addPathSegments("gameServer/mobile/json/whosonlineandlive.jsp")
                  .addQueryParameter("name2", session.name())
                  .addQueryParameter("password2", session.password())
                  .build();
          return exchange(url, body -> {
              Type listType = new TypeToken<List<JsonModels.RoomEntry>>() {}.getType();
              List<JsonModels.RoomEntry> rooms = Json.GSON.fromJson(body, listType);
              if (rooms == null) return null;
              return new WhosOnline(rooms);
          });
      }

      @Override
      public Result<Bitmap> avatar(String name) {
          HttpUrl url = base()
                  .addPathSegments("gameServer/avatar")
                  .addQueryParameter("name", name)
                  .build();
          try (Response response = client.newCall(authedGet(url)).execute()) {
              int code = response.code();
              if (code == 401 || code == 403) {
                  return Result.fail(new Result.Failure(Result.Reason.AUTH_EXPIRED, code, null));
              }
              if (code != 200) {
                  return Result.fail(new Result.Failure(Result.Reason.SERVER, code, null));
              }
              ResponseBody body = response.body();
              if (body == null) {
                  return Result.fail(new Result.Failure(Result.Reason.PARSE, code, null));
              }
              InputStream in = body.byteStream();
              Bitmap bitmap = BitmapFactory.decodeStream(in);
              if (bitmap == null) {
                  return Result.fail(new Result.Failure(Result.Reason.PARSE, code, null));
              }
              return Result.ok(bitmap);
          } catch (IOException e) {
              return Result.fail(new Result.Failure(Result.Reason.NETWORK, 0, e));
          }
      }

      @Override
      public Result<Boolean> login(String name, String password) {
          HttpUrl url = base()
                  .addPathSegments("gameServer/login.jsp")
                  .addQueryParameter("mobile", "")
                  .addQueryParameter("name2", name)
                  .addQueryParameter("password2", password)
                  .build();
          // Plain request (no gen stamp): this IS the auth action.
          Request request = new Request.Builder().url(url).get().build();
          try (Response response = client.newCall(request).execute()) {
              int code = response.code();
              if (code == 200) {
                  return Result.ok(Boolean.TRUE);
              }
              if (code == 401 || code == 403) {
                  return Result.fail(new Result.Failure(Result.Reason.INVALID_CREDENTIALS, code, null));
              }
              return Result.fail(new Result.Failure(Result.Reason.SERVER, code, null));
          } catch (IOException e) {
              return Result.fail(new Result.Failure(Result.Reason.NETWORK, 0, e));
          }
      }

      @Override
      public Result<JsonModels.GameResponse> loadGame(String gid) {
          HttpUrl url = base()
                  .addPathSegments("gameServer/mobile/json/game.jsp")
                  .addQueryParameter("gid", gid)
                  .addQueryParameter("name2", session.name())
                  .addQueryParameter("password2", session.password())
                  .build();
          return exchange(url, body -> {
              JsonModels.GameResponse game = Json.GSON.fromJson(body, JsonModels.GameResponse.class);
              if (game == null || game.gameName == null) return null;
              return game;
          });
      }

      @Override
      public Result<Void> submitMove(String gid, String moves, String message) {
          HttpUrl url = base()
                  .addPathSegments("gameServer/tb/game")
                  .addQueryParameter("command", "move")
                  .addQueryParameter("mobile", "")
                  .addQueryParameter("gid", gid)
                  .addQueryParameter("moves", moves)
                  .addQueryParameter("message", message)
                  .addQueryParameter("name2", session.name())
                  .addQueryParameter("password2", session.password())
                  .build();
          try (Response response = client.newCall(authedGet(url)).execute()) {
              int code = response.code();
              if (code == 200) {
                  return Result.ok(null);
              }
              if (code == 401 || code == 403) {
                  return Result.fail(new Result.Failure(Result.Reason.AUTH_EXPIRED, code, null));
              }
              return Result.fail(new Result.Failure(Result.Reason.SERVER, code, null));
          } catch (IOException e) {
              return Result.fail(new Result.Failure(Result.Reason.NETWORK, 0, e));
          }
      }

      /**
       * Single-flight re-authenticator. On 401/403 OkHttp calls authenticate():
       *   - serializes concurrent attempts (synchronized);
       *   - performs the network login exactly ONCE per generation (a request's
       *     stamped generation == the live counter means it is the first to fail
       *     under that credential set) so N concurrent 401s trigger ONE login;
       *   - retries the ORIGINAL request unchanged, so name2/password2 (already in
       *     the original URL) are KEPT — fixing the prod bug at PentePlayer.java:678
       *     where the retried index.jsp dropped name2/password2;
       *   - gives up after a single retry (returns null) to avoid infinite loops.
       */
      static final class ReAuthenticator implements Authenticator {

          static final String GEN_HEADER = "X-Pente-AuthGen";

          private final Session session;
          private final BaseUrlProvider urls;
          private final OkHttpClient loginClient;
          private int authGeneration = 0;

          ReAuthenticator(Session session, BaseUrlProvider urls, OkHttpClient loginClient) {
              this.session = session;
              this.urls = urls;
              this.loginClient = loginClient;
          }

          synchronized int currentGeneration() {
              return authGeneration;
          }

          @Override
          public synchronized Request authenticate(Route route, Response response) {
              // One retry only: a request that already carried a refreshed gen and
              // STILL got 401 must stop.
              if (responseCount(response) >= 2) {
                  return null;
              }
              String header = response.request().header(GEN_HEADER);
              int requestGen = header == null ? 0 : Integer.parseInt(header);
              if (requestGen == authGeneration) {
                  // First failure under the current credential set: refresh once.
                  if (!doLogin()) {
                      return null;
                  }
                  authGeneration++;
              }
              // else: another thread already refreshed; just retry with newest gen.
              return response.request().newBuilder()
                      .header(GEN_HEADER, Integer.toString(authGeneration))
                      .build();
          }

          private boolean doLogin() {
              HttpUrl url = HttpUrl.parse(urls.baseUrl()).newBuilder()
                      .addPathSegments("gameServer/login.jsp")
                      .addQueryParameter("mobile", "")
                      .addQueryParameter("name2", session.name())
                      .addQueryParameter("password2", session.password())
                      .build();
              Request request = new Request.Builder().url(url).get().build();
              try (Response response = loginClient.newCall(request).execute()) {
                  return response.isSuccessful();
              } catch (IOException e) {
                  return false;
              }
          }

          private static int responseCount(Response response) {
              int count = 1;
              Response prior = response.priorResponse();
              while (prior != null) {
                  count++;
                  prior = prior.priorResponse();
              }
              return count;
          }
      }
  }
  ```

- [ ] **Step 6: Run the test — expect PASS (green).** Run:
  ```bash
  ./gradlew :app:testDebugUnitTest --tests "be.submanifold.pentelive.net.OkHttpPenteApiTest"
  ```
  Expected: `BUILD SUCCESSFUL`, both `concurrentAuthExpired_triggersExactlyOneReLogin` and `reAuthRetry_keepsName2AndPassword2` pass.

- [ ] **Step 7: Verify the whole module still compiles.** Run:
  ```bash
  ./gradlew :app:compileDebugJavaWithJavac
  ```
  Expected: `BUILD SUCCESSFUL` (confirms `OkHttpPenteApi` satisfies the full `PenteApi` interface and the new deps resolve).

- [ ] **Step 8: Commit.** Run:
  ```bash
  git add app/build.gradle app/src/main/java/be/submanifold/pentelive/net/OkHttpPenteApi.java app/src/test/java/be/submanifold/pentelive/net/OkHttpPenteApiTest.java
  git commit -m "Add OkHttpPenteApi with single-flight re-auth Authenticator

Real OkHttp adapter for PenteApi: synchronous exchange() maps HTTP
codes/bodies to Result.Reason, and an okhttp3.Authenticator does
single-flight re-login (synchronized + generation counter, one retry).
Retrying the original request preserves name2/password2, fixing the prod
credential-drop at PentePlayer.java:678. MockWebServer tests cover
single-flight (N concurrent 401s -> one login) and the credential-drop
regression.

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
  ```

---

### Task 5: PenteApiClient scheduler + lifecycle (serial executor + cancelable main-thread delivery)

**Files:**
- Create: `/Users/waliedothman/mariposa/coding/pente.org-project/penteLive-Android/app/src/main/java/be/submanifold/pentelive/net/PenteApiClient.java` (new file, final ~89 lines)
- Test: `/Users/waliedothman/mariposa/coding/pente.org-project/penteLive-Android/app/src/test/java/be/submanifold/pentelive/net/PenteApiClientTest.java` (new file, final ~115 lines)
- Depends on (do NOT modify; from earlier task): `/Users/waliedothman/mariposa/coding/pente.org-project/penteLive-Android/app/src/main/java/be/submanifold/pentelive/net/Result.java` — uses `Result.ok(...)`, `Result.<T>fail(...)`, nested `Result.Failure(Result.Reason, int, Throwable)`, `Result.Reason.SERVER`.

> Design note: unit tests here are plain JVM JUnit 4.13.2 (no Robolectric — `app/build.gradle` line 93 is the only `testImplementation`). So `PenteApiClient` exposes a **package-private** constructor `PenteApiClient(ExecutorService worker, Executor main)` as a test seam; the public no-arg constructor wires the real `Handler(Looper.getMainLooper())`. The test (same package `be.submanifold.pentelive.net`) injects a real single-thread worker + a `ManualExecutor`, so it never touches `Looper`.

- [ ] **Step 1: Write the failing test with real code.** Create `/Users/waliedothman/mariposa/coding/pente.org-project/penteLive-Android/app/src/test/java/be/submanifold/pentelive/net/PenteApiClientTest.java`:
```java
package be.submanifold.pentelive.net;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class PenteApiClientTest {

    /** Collects posted runnables; runs them only when the test asks. */
    static final class ManualExecutor implements Executor {
        private final List<Runnable> tasks = new ArrayList<>();
        private final CountDownLatch posted;

        ManualExecutor() { this(null); }
        ManualExecutor(CountDownLatch posted) { this.posted = posted; }

        @Override public synchronized void execute(Runnable r) {
            tasks.add(r);
            if (posted != null) {
                posted.countDown();
            }
        }

        synchronized void runAll() {
            for (Runnable r : new ArrayList<>(tasks)) {
                r.run();
            }
            tasks.clear();
        }
    }

    @Test
    public void enqueue_runsOffCallerThread_andPreservesOrder() throws Exception {
        ExecutorService worker = Executors.newSingleThreadExecutor();
        ManualExecutor main = new ManualExecutor();
        PenteApiClient client = new PenteApiClient(worker, main);

        final List<String> runOrder = Collections.synchronizedList(new ArrayList<String>());
        final CountDownLatch both = new CountDownLatch(2);
        final Thread callerThread = Thread.currentThread();
        final AtomicReference<Thread> workerThread = new AtomicReference<>();
        final PenteApiClient.Cb<String> noop = new PenteApiClient.Cb<String>() {
            @Override public void onResult(Result<String> r) { }
        };

        client.enqueue(new Callable<Result<String>>() {
            @Override public Result<String> call() {
                workerThread.set(Thread.currentThread());
                runOrder.add("a");
                both.countDown();
                return Result.ok("a");
            }
        }, noop);

        client.enqueue(new Callable<Result<String>>() {
            @Override public Result<String> call() {
                runOrder.add("b");
                both.countDown();
                return Result.ok("b");
            }
        }, noop);

        assertTrue("both callables should run", both.await(2, TimeUnit.SECONDS));
        assertEquals(Arrays.asList("a", "b"), runOrder);
        assertNotSame("callable must run off the caller thread", callerThread, workerThread.get());

        worker.shutdownNow();
    }

    @Test
    public void cancel_beforeCompletion_callbackNeverFires() throws Exception {
        ExecutorService worker = Executors.newSingleThreadExecutor();
        final CountDownLatch posted = new CountDownLatch(1);
        ManualExecutor main = new ManualExecutor(posted);
        PenteApiClient client = new PenteApiClient(worker, main);

        final CountDownLatch started = new CountDownLatch(1);
        final CountDownLatch proceed = new CountDownLatch(1);
        final AtomicBoolean cbFired = new AtomicBoolean(false);

        PenteApiClient.Cancelable c = client.enqueue(new Callable<Result<String>>() {
            @Override public Result<String> call() throws Exception {
                started.countDown();
                proceed.await(2, TimeUnit.SECONDS);
                return Result.ok("hi");
            }
        }, new PenteApiClient.Cb<String>() {
            @Override public void onResult(Result<String> r) {
                cbFired.set(true);
            }
        });

        assertTrue("callable should have started", started.await(2, TimeUnit.SECONDS));
        c.cancel();                 // cancel while the callable is still blocked
        proceed.countDown();        // let the callable finish
        assertTrue("callback should be posted to main", posted.await(2, TimeUnit.SECONDS));
        main.runAll();              // execute the posted callback runnable
        assertFalse("canceled callback must not fire", cbFired.get());

        worker.shutdownNow();
    }
}
```

- [ ] **Step 2: Run the test and confirm it FAILS.** `./gradlew :app:testDebugUnitTest --tests "be.submanifold.pentelive.net.PenteApiClientTest"` — expected: `BUILD FAILED`, compilation error `cannot find symbol: class PenteApiClient` (and `Cb`/`Cancelable`), because `PenteApiClient` does not exist yet.

- [ ] **Step 3: Write the minimal real implementation.** Create `/Users/waliedothman/mariposa/coding/pente.org-project/penteLive-Android/app/src/main/java/be/submanifold/pentelive/net/PenteApiClient.java`:
```java
package be.submanifold.pentelive.net;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Schedules synchronous {@link PenteApi}-style calls on a single serial
 * background thread and delivers their {@link Result} on the Android main
 * thread. A returned {@link Cancelable} suppresses the callback after
 * cancellation so a destroyed Activity is never touched (no leak).
 */
public final class PenteApiClient {

    public interface Cb<T> {
        void onResult(Result<T> r);
    }

    public interface Cancelable {
        void cancel();
    }

    private final ExecutorService worker;
    private final Executor main;

    /** Production: one serial worker thread, callbacks delivered on the UI thread. */
    public PenteApiClient() {
        this(Executors.newSingleThreadExecutor(), mainThreadExecutor());
    }

    /** Package-private seam for unit tests: inject worker + main executors. */
    PenteApiClient(ExecutorService worker, Executor main) {
        this.worker = worker;
        this.main = main;
    }

    public <T> Cancelable enqueue(final Callable<Result<T>> call, final Cb<T> cb) {
        final Task<T> task = new Task<>(call, cb);
        worker.execute(task);
        return task;
    }

    private static Executor mainThreadExecutor() {
        final Handler handler = new Handler(Looper.getMainLooper());
        return new Executor() {
            @Override public void execute(Runnable r) {
                handler.post(r);
            }
        };
    }

    private final class Task<T> implements Runnable, Cancelable {
        private final Callable<Result<T>> call;
        private final Cb<T> cb;
        private volatile boolean canceled;

        Task(Callable<Result<T>> call, Cb<T> cb) {
            this.call = call;
            this.cb = cb;
        }

        @Override public void cancel() {
            canceled = true;
        }

        @Override public void run() {
            if (canceled) {
                return;
            }
            Result<T> r;
            try {
                r = call.call();
            } catch (Exception e) {
                r = Result.<T>fail(new Result.Failure(Result.Reason.SERVER, 0, e));
            }
            final Result<T> result = r;
            main.execute(new Runnable() {
                @Override public void run() {
                    if (!canceled) {
                        cb.onResult(result);
                    }
                }
            });
        }
    }
}
```

- [ ] **Step 4: Run the test and confirm it PASSES.** `./gradlew :app:testDebugUnitTest --tests "be.submanifold.pentelive.net.PenteApiClientTest"` — expected: `BUILD SUCCESSFUL`, both `enqueue_runsOffCallerThread_andPreservesOrder` and `cancel_beforeCompletion_callbackNeverFires` pass.

- [ ] **Step 5: Verify the production (Looper) path compiles.** `./gradlew :app:compileDebugJavaWithJavac` — expected: `BUILD SUCCESSFUL` (confirms the `Handler(Looper.getMainLooper())` production constructor compiles against the Android SDK).

- [ ] **Step 6: Commit.** `git add app/src/main/java/be/submanifold/pentelive/net/PenteApiClient.java app/src/test/java/be/submanifold/pentelive/net/PenteApiClientTest.java && git commit -m "Add PenteApiClient serial scheduler with cancelable main-thread delivery

Single serial ExecutorService runs Callables off the main thread (FIFO
ordering preserved); a Handler(Looper.getMainLooper()) delivers Results on
the UI thread. Cancelable.cancel() sets a volatile flag so the callback is
suppressed after Activity onDestroy. Package-private (ExecutorService,
Executor) constructor is the test seam.

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"`

---

### Task 6: STRANGLE the whos-online endpoint end-to-end

**Files:**
- Create: `app/src/main/java/be/submanifold/pentelive/net/WhosOnlineView.java`
- Create: `app/src/main/java/be/submanifold/pentelive/net/WhosOnlinePresenter.java`
- Modify: `app/src/main/java/be/submanifold/pentelive/MainActivity.java` — imports (lines 12, 21, 35–36, 39–43, 49; add net imports), fields (lines 56–60), onCreate wiring (after line 73), `onlineUsers` case (lines 379–386), add `showWhosOnlinePopup(...)` method, add `onDestroy()` (after line 438), delete `LoadWhosOnlineTask` (lines 515–665, incl. inline `new Gson()` at 585 and the `if (PentePlayer.development)` URL branch at 532–534)
- Test: `app/src/test/java/be/submanifold/pentelive/net/WhosOnlinePresenterTest.java`

> Consumes from earlier net-package tasks: `Result` (+ nested `Result.Reason`, `Result.Failure`), `WhosOnline` (POJO exposing `public final List<JsonModels.RoomEntry> rooms`), `PenteApi.whosOnline()`, `FakePenteApi` (public surface: `final Map<String,Result<?>> canned`, `final List<String> calls`, `Result.Reason nextFailure`), `PenteApiClient` (+ nested `Cb<T>`, `Cancelable`), `Session`/`SharedPrefsSession`, `BaseUrlProvider`/`DefaultBaseUrlProvider`, `OkHttpPenteApi`.

- [ ] **Step 1: Write the failing fake-backed presenter test (REAL code).** Create `app/src/test/java/be/submanifold/pentelive/net/WhosOnlinePresenterTest.java`:
```java
package be.submanifold.pentelive.net;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import be.submanifold.pentelive.JsonModels;

public class WhosOnlinePresenterTest {

    static final class RecordingView implements WhosOnlineView {
        WhosOnline rendered;
        Result.Reason errorShown;

        @Override
        public void renderWhosOnline(WhosOnline data) {
            this.rendered = data;
        }

        @Override
        public void showError(Result.Reason reason) {
            this.errorShown = reason;
        }
    }

    private WhosOnline sampleWhosOnline() {
        JsonModels.RoomEntry room = new JsonModels.RoomEntry();
        room.name = "Mobile";
        room.players = new ArrayList<>();
        List<JsonModels.RoomEntry> rooms = new ArrayList<>();
        rooms.add(room);
        return new WhosOnline(rooms);
    }

    @Test
    public void rendersOnOk() {
        FakePenteApi api = new FakePenteApi();
        api.canned.put("whosOnline", Result.ok(sampleWhosOnline()));
        RecordingView view = new RecordingView();
        WhosOnlinePresenter presenter = new WhosOnlinePresenter(view);

        presenter.onResult(api.whosOnline());

        assertNotNull(view.rendered);
        assertEquals(1, view.rendered.rooms.size());
        assertEquals("Mobile", view.rendered.rooms.get(0).name);
        assertNull(view.errorShown);
        assertEquals(1, api.calls.size());
        assertEquals("whosOnline", api.calls.get(0));
    }

    @Test
    public void showsTypedErrorOnFailure() {
        FakePenteApi api = new FakePenteApi();
        api.nextFailure = Result.Reason.NETWORK;
        RecordingView view = new RecordingView();
        WhosOnlinePresenter presenter = new WhosOnlinePresenter(view);

        presenter.onResult(api.whosOnline());

        assertNull(view.rendered);
        assertEquals(Result.Reason.NETWORK, view.errorShown);
    }
}
```

- [ ] **Step 2: Run the test — expect FAIL (compile error: `WhosOnlineView` / `WhosOnlinePresenter` do not exist).**
```
./gradlew :app:testDebugUnitTest --tests "be.submanifold.pentelive.net.WhosOnlinePresenterTest"
```
Expected: `> Task :app:compileDebugUnitTestJavaWithJavac FAILED` — `cannot find symbol: class WhosOnlineView` / `class WhosOnlinePresenter`.

- [ ] **Step 3: Create the view seam interface (minimal REAL impl).** Create `app/src/main/java/be/submanifold/pentelive/net/WhosOnlineView.java`:
```java
package be.submanifold.pentelive.net;

public interface WhosOnlineView {
    void renderWhosOnline(WhosOnline data);

    void showError(Result.Reason reason);
}
```

- [ ] **Step 4: Create the presenter (minimal REAL impl).** Create `app/src/main/java/be/submanifold/pentelive/net/WhosOnlinePresenter.java`:
```java
package be.submanifold.pentelive.net;

public final class WhosOnlinePresenter {

    private final WhosOnlineView view;

    public WhosOnlinePresenter(WhosOnlineView view) {
        this.view = view;
    }

    public void onResult(Result<WhosOnline> r) {
        if (r.isOk()) {
            view.renderWhosOnline(r.value);
        } else {
            view.showError(r.failure.reason);
        }
    }
}
```

- [ ] **Step 5: Run the test — expect PASS.**
```
./gradlew :app:testDebugUnitTest --tests "be.submanifold.pentelive.net.WhosOnlinePresenterTest"
```
Expected: `BUILD SUCCESSFUL`, 2 tests passing (`rendersOnOk`, `showsTypedErrorOnFailure`).

- [ ] **Step 6: Commit the green seam.**
```
git add app/src/main/java/be/submanifold/pentelive/net/WhosOnlineView.java app/src/main/java/be/submanifold/pentelive/net/WhosOnlinePresenter.java app/src/test/java/be/submanifold/pentelive/net/WhosOnlinePresenterTest.java
git commit -m "Add WhosOnlinePresenter view seam for whos-online strangle

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

- [ ] **Step 7: Add net imports to `MainActivity.java`.** After line 51 (`import be.submanifold.pentelive.liveGameRoom.LobbyActivity;`) insert:
```java
import be.submanifold.pentelive.net.BaseUrlProvider;
import be.submanifold.pentelive.net.DefaultBaseUrlProvider;
import be.submanifold.pentelive.net.OkHttpPenteApi;
import be.submanifold.pentelive.net.PenteApi;
import be.submanifold.pentelive.net.PenteApiClient;
import be.submanifold.pentelive.net.Result;
import be.submanifold.pentelive.net.Session;
import be.submanifold.pentelive.net.SharedPrefsSession;
import be.submanifold.pentelive.net.WhosOnline;
import be.submanifold.pentelive.net.WhosOnlinePresenter;
import be.submanifold.pentelive.net.WhosOnlineView;
```

- [ ] **Step 8: Add the api/client/cancelable fields.** Replace the field block (lines 56–60):
```java
    private PentePlayer player;
    private SwipeRefreshLayout swipeRefreshLayout;
    private DashboardListAdapter listAdapter;
    private View viewWithOpenButtons = null;
    private PopupWindow popupWindow;
```
with:
```java
    private PentePlayer player;
    private SwipeRefreshLayout swipeRefreshLayout;
    private DashboardListAdapter listAdapter;
    private View viewWithOpenButtons = null;
    private PopupWindow popupWindow;
    private PenteApi api;
    private PenteApiClient client;
    private PenteApiClient.Cancelable whosOnlineCancelable;
```

- [ ] **Step 9: Wire the production api + client ONCE in onCreate.** Replace line 73:
```java
        this.player = getIntent().getParcelableExtra("pentePlayer");
```
with:
```java
        this.player = getIntent().getParcelableExtra("pentePlayer");

        Session session = new SharedPrefsSession(getApplicationContext());
        BaseUrlProvider baseUrl = new DefaultBaseUrlProvider();
        this.api = new OkHttpPenteApi(session, baseUrl);
        this.client = new PenteApiClient();
```

- [ ] **Step 10: Strangle the `onlineUsers` case (replace AsyncTask call site).** Replace lines 379–386:
```java
                case R.id.onlineUsers:
                    WhosOnlineListAdapter onlineListAdapter = new WhosOnlineListAdapter(player);
                    onlineListAdapter.setInflater((LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE), MainActivity.this);

                    LoadWhosOnlineTask loadWhosOnlineTask = new LoadWhosOnlineTask(player, onlineListAdapter);
                    loadWhosOnlineTask.execute((Void) null);

                    return true;
```
with:
```java
                case R.id.onlineUsers:
                    WhosOnlineListAdapter onlineListAdapter = new WhosOnlineListAdapter(player);
                    onlineListAdapter.setInflater((LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE), MainActivity.this);

                    WhosOnlinePresenter whosOnlinePresenter = new WhosOnlinePresenter(new WhosOnlineView() {
                        @Override
                        public void renderWhosOnline(WhosOnline data) {
                            showWhosOnlinePopup(onlineListAdapter, data.rooms);
                        }

                        @Override
                        public void showError(Result.Reason reason) {
                            Toast.makeText(MainActivity.this, getString(R.string.error_connecting), Toast.LENGTH_SHORT).show();
                        }
                    });
                    whosOnlineCancelable = client.enqueue(() -> api.whosOnline(), whosOnlinePresenter::onResult);

                    return true;
```

- [ ] **Step 11: Delete the entire `LoadWhosOnlineTask` inner class and extract its rendering into `showWhosOnlinePopup`.** Replace the whole class block (lines 515–665, from `private class LoadWhosOnlineTask extends AsyncTask<Void, Void, Boolean> {` through its closing `}` after `onCancelled()`) with the render-only method (no networking, no `new Gson()`, no `if (development)`):
```java
    private void showWhosOnlinePopup(WhosOnlineListAdapter listAdapter, List<JsonModels.RoomEntry> rooms) {
        final Map<String, List<KothPlayer>> onlinePlayers = new HashMap<>();
        int total = 0;
        Map<String, String> onlinePlayerNames = new HashMap<>();
        if (rooms != null) {
            for (JsonModels.RoomEntry room : rooms) {
                List<KothPlayer> playersList = new ArrayList<>();
                if (room.players != null) {
                    for (JsonModels.OnlinePlayerEntry entry : room.players) {
                        KothPlayer kothPlayer = new KothPlayer(entry.name, String.valueOf(entry.rating), "", false, entry.tourneyWinner, entry.color);
                        if (PentePlayer.loadAvatars && kothPlayer.getColor() != 0) {
                            this.player.addUserAvatar(kothPlayer.getName());
                        }
                        onlinePlayerNames.put(kothPlayer.getName(), "");
                        total = total + 1;
                        playersList.add(kothPlayer);
                    }
                }
                onlinePlayers.put(room.name, playersList);
            }
        }
        PentePlayer.setOnlinePlayerNames(onlinePlayerNames);
        listAdapter.setOnlinePlayers(onlinePlayers);
        Point size = new Point();
        Display display = getWindowManager().getDefaultDisplay();
        display.getSize(size);

        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View popUpView = inflater.inflate(R.layout.onlineusers_listview, null);
        final float scale = getResources().getDisplayMetrics().density;
        popupWindow = new PopupWindow(popUpView, size.x * 4 / 5, (int) ((30 + Math.min(Math.floor((((size.y / scale) * 2 / 3) / 44)) * 44, 30 + total * 44)) * scale), true);
        ExpandableListView onlineUsersListView = popupWindow.getContentView().findViewById(R.id.onlineUsersListView);
        onlineUsersListView.setDividerHeight(0);
        onlineUsersListView.setAdapter(listAdapter);
        for (int i = 0; i < onlinePlayers.size(); i++) {
            onlineUsersListView.expandGroup(i);
        }
        onlineUsersListView.setOnGroupClickListener((parent, v, groupPosition, id) -> {
            return true; // This way the expander cannot be collapsed
        });
        onlineUsersListView.setOnChildClickListener((parent, v, groupPosition, childPosition, id) -> {
            KothPlayer onlinePlayer = onlinePlayers.get(listAdapter.sections.get(groupPosition)).get(childPosition);
            if (!listAdapter.sections.get(groupPosition).equals("Mobile")) {
                String url = "https://www.pente.org/gameServer/profile?viewName=" + onlinePlayer.getName() + "&name2=" + PentePlayer.mPlayerName + "&password2=" + PentePlayer.mPassword;
                Intent intent = new Intent(MainActivity.this, WebViewActivity.class);
                intent.putExtra("url", url);
                startActivity(intent);

                return false;
            }
            if (player.getPlayerName().equals(onlinePlayer.getName())) {
                return false;
            }
            Intent intent = new Intent(getApplicationContext(), InvitationActivity.class);
            intent.putExtra("opponent", onlinePlayer.getName());
            startActivity(intent);

            return true;
        });

        popupWindow.setFocusable(true);
        popupWindow.setOutsideTouchable(true);
        popupWindow.showAtLocation(findViewById(R.id.list), Gravity.TOP, 0, 260);
        popupWindow.setOnDismissListener(() -> findViewById(R.id.list).setAlpha(1.0f));
        findViewById(R.id.list).setAlpha(0.05f);
    }
```

- [ ] **Step 12: Cancel the in-flight call in `onDestroy` (after the existing `onPause`, line 438 `}`).** Insert:
```java
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (whosOnlineCancelable != null) {
            whosOnlineCancelable.cancel();
        }
    }
```

- [ ] **Step 13: Remove the now-dead imports (each was used only inside the deleted AsyncTask).** Delete these import lines from `MainActivity.java`: `import android.os.AsyncTask;` (12), `import android.webkit.CookieManager;` (21), `import com.google.gson.Gson;` (35), `import com.google.gson.reflect.TypeToken;` (36), `import java.io.BufferedReader;` (39), `import java.io.IOException;` (40), `import java.io.InputStreamReader;` (41), `import java.lang.reflect.Type;` (42), `import java.net.URL;` (43), `import javax.net.ssl.HttpsURLConnection;` (49). Keep `ArrayList`, `HashMap`, `List`, `Map`, `Point`, `Display` (still used by `showWhosOnlinePopup`).

- [ ] **Step 14: Compile the app — expect PASS (proves the strangle wired correctly, no dangling refs to deleted task/imports).**
```
./gradlew :app:compileDebugJavaWithJavac
```
Expected: `BUILD SUCCESSFUL` with no `cannot find symbol` for `Gson`, `AsyncTask`, `LoadWhosOnlineTask`, `URL`, or `PentePlayer.development`.

- [ ] **Step 15: Run the full app unit test suite — expect PASS.**
```
./gradlew :app:testDebugUnitTest
```
Expected: `BUILD SUCCESSFUL`; `WhosOnlinePresenterTest` green; no regressions.

- [ ] **Step 16: Commit the migration.**
```
git add app/src/main/java/be/submanifold/pentelive/MainActivity.java
git commit -m "Strangle whos-online onto PenteApi + WhosOnlinePresenter

Replace LoadWhosOnlineTask AsyncTask with client.enqueue(api::whosOnline)
routed through WhosOnlinePresenter; cancel in onDestroy. Drops the inline
new Gson(), the manual cookie handling, and the if(development) URL branch.

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

### Task 7: Strangle the remaining ~34 call sites onto `PenteApi` (per-endpoint migration checklist)

**Files:**
- Modify `app/src/main/java/be/submanifold/pentelive/net/PenteApi.java` — add one typed method per endpoint below
- Modify `app/src/main/java/be/submanifold/pentelive/net/OkHttpPenteApi.java` — implement each new method (reuses the single `OkHttpClient` + `Authenticator` from Task 1-6; no `if(development)`, no manual cookie filtering)
- Modify `app/src/main/java/be/submanifold/pentelive/net/FakePenteApi.java` — add canned response + recorded call for each new method
- Modify call sites (delete AsyncTasks, inline `new Gson()`, cookie filters, `if(development)` branches):
  - `app/src/main/java/be/submanifold/pentelive/PentePlayer.java` lines 580-632 (avatar), 634-757 (dashboard), 759-846 (reply), 848-end (cancel), 699 (Gson), 655-664 (cookie filter), 650/669/679/703/713 (dev)
  - `app/src/main/java/be/submanifold/pentelive/Game.java` lines 382-477 (loadGame), 428/456 (Gson), 398/432/439 (dev); 479-590 (submitMove), 498/501 (dev)
  - `app/src/main/java/be/submanifold/pentelive/KingOfTheHillActivity.java` lines 333-402 (list), 305 (Gson), 349/356-365 (dev/cookie); 404-488 (join), 432-441 (cookie)
  - `app/src/main/java/be/submanifold/pentelive/liveGameRoom/LobbyActivity.java` line 249 (Gson) — drop alongside whos-online sibling
  - `app/src/main/java/be/submanifold/pentelive/ReplyMessageActivity.java` 205-321 (send), 323-444 (view), 446-end (delete); cookie filters 250-259/345-354/473-482; dev 245/340/468
  - `app/src/main/java/be/submanifold/pentelive/SendMessageActivity.java` 99-end (send)
  - `app/src/main/java/be/submanifold/pentelive/SettingsActivity.java` 245-323 (avatar), 325-376 (color), 378-431 (email), 433-end (ads), dev 449
  - `app/src/main/java/be/submanifold/pentelive/SocialActivity.java` 280-367 (followers list), 369-end (follow toggle)
  - `app/src/main/java/be/submanifold/pentelive/RegisterActivity.java` 120-end (register)
  - `app/src/main/java/be/submanifold/pentelive/MyFcmListenerService.java` 59-end (token), dev 80; and `LoginActivity.java` 505-end (`SendTokenTask`)
- Test: `app/src/test/java/be/submanifold/pentelive/net/OkHttpPenteApiTest.java` (parse/auth via `okhttp3.mockwebserver.MockWebServer`) and `app/src/test/java/be/submanifold/pentelive/net/FakePenteApiTest.java` (call-site behavior). One new `@Test` per endpoint, appended.

Each endpoint below is ONE strangle micro-cycle: (a) add a failing test against `FakePenteApi`/`MockWebServer` → run `./gradlew :app:testDebugUnitTest` → expect **FAIL** (method missing / compile error); (b) add the `PenteApi` method + `OkHttpPenteApi` impl + `FakePenteApi` canned response; (c) run `./gradlew :app:testDebugUnitTest` → expect **PASS**; (d) rewire the call site to `PenteApiClient.enqueue(() -> api.<method>(...), cb)`, delete the listed AsyncTask body, remove the inline `new Gson()` (use `Json.GSON`), drop the cookie-filter loop, remove the `if(development)` branch; (e) `./gradlew :app:compileDebugJavaWithJavac` → expect **success**; (f) `git add -A && git commit`.

- [ ] **Step 1: dashboard (index.jsp)** — Add `Result<JsonModels.IndexResponse> dashboard();` (server returns `IndexResponse` with `.player`; OkHttp impl GETs `baseUrl()+"/gameServer/mobile/json/index.jsp?name="+name+"&password="+pw`, parses with `Json.GSON`, maps `null`/`player==null` → `Result.fail(AUTH_EXPIRED)` so the `Authenticator` re-auths instead of the inline relogin block). Delete `PentePlayer.LoadPlayerTask` (634-757); remove `new Gson()` at 699; drop cookie filter 655-664; remove `if(development)` at 650/669/679/703/713. Commit `feat(net): migrate dashboard index.jsp to PenteApi.dashboard()`.
- [ ] **Step 2: loadGame (game.jsp)** — Use existing `Result<JsonModels.GameResponse> loadGame(String gid)`; impl GETs `/gameServer/mobile/json/game.jsp?gid="+gid`, parses with `Json.GSON`. Delete `Game.RetrieveGame` (382-477); remove both `new Gson()` at 428 and 456; remove `if(development)` at 398/432/439; the 431-440 relogin block is now handled by the `Authenticator`. Commit `feat(net): migrate loadGame game.jsp to PenteApi.loadGame()`.
- [ ] **Step 3: submitMove (tb/game?command=move)** — Use existing `Result<Void> submitMove(String gid,String moves,String message)`; impl GETs `/gameServer/tb/game?command=move"+moves+"&gid="+gid` (keep the `hideStr`/message params currently built at Game:498). Delete `Game.SubmitMoveTask` (479-590); remove `if(development)` at 500. Commit `feat(net): migrate submitMove to PenteApi.submitMove()`.
- [ ] **Step 4: invitation accept/decline (tb/replyInvitation)** — Add `Result<Void> replyInvitation(String sid, boolean accept);` (impl GETs `/gameServer/tb/replyInvitation?mobile=&inviteeMessage=&command="+(accept?"Accept":"Decline")+"&sid="+sid`). Delete `PentePlayer.AcceptDeclineInvitationTask` (759-846); remove `if(development)` at 788/792 and the cookie filter. Commit `feat(net): migrate replyInvitation to PenteApi.replyInvitation()`.
- [ ] **Step 5: invitation cancel (tb/cancelInvitation)** — Add `Result<Void> cancelInvitation(String sid);` (impl GETs `/gameServer/tb/cancelInvitation?mobile=&command=Cancel&sid="+sid`). Delete `PentePlayer.CancelInvitationTask` (848-end); remove `if(development)` at 867 and the cookie filter. Commit `feat(net): migrate cancelInvitation to PenteApi.cancelInvitation()`.
- [ ] **Step 6: KotH list (koth.jsp)** — Add `Result<java.util.List<java.util.List<JsonModels.KothPlayerEntry>>> kothList(String game);` (impl GETs `/gameServer/mobile/json/koth.jsp`, parses with `Json.GSON` + the `TypeToken<List<List<KothPlayerEntry>>>` currently at KingOfTheHillActivity:304). Delete `KingOfTheHillActivity.LoadHillTask` (333-402); remove `new Gson()` at 305 (move `loadHill` to consume the typed result); drop cookie filter 356-365; remove `if(development)` at 349. Commit `feat(net): migrate KotH list koth.jsp to PenteApi.kothList()`.
- [ ] **Step 7: KotH join (stairs.jsp)** — Add `Result<Void> kothJoin(String game, boolean join);` (impl GETs `/gameServer/stairs.jsp?game="+game` with the join/leave command param at KingOfTheHillActivity:418). Delete `KingOfTheHillActivity.JoinLeaveHillTask` (404-488); drop cookie filter 432-441; remove `if(development)`. Commit `feat(net): migrate KotH join stairs.jsp to PenteApi.kothJoin()`.
- [ ] **Step 8: messages send/reply (mymessages command=create)** — Add `Result<Void> sendMessage(String to,String subject,String body);` (impl POSTs form `command=create&to=..&subject=..&body=..` to `/gameServer/mymessages`). Delete `ReplyMessageActivity.SendMessageTask` (205-321) AND `SendMessageActivity.SendMessageTask` (99-end), pointing both at this one method; drop cookie filter 250-259; remove `if(development)` at 245. Commit `feat(net): migrate send/reply message to PenteApi.sendMessage()`.
- [ ] **Step 9: messages view + delete (mymessages command=view/delete)** — Add `Result<String> viewMessage(String mid);` and `Result<Void> deleteMessage(String mid);` (GET `?command=view&mid=` ; POST `command=delete&mid=`). Delete `ReplyMessageActivity.LoadMessageTask` (323-444) and `DeleteMessageTask` (446-end); drop cookie filters 345-354/473-482; remove `if(development)` at 340/468. Commit `feat(net): migrate view/delete message to PenteApi`.
- [ ] **Step 10: settings save (changeAvatar/changeColor/changeEmailPreference/changeAdsPreference)** — Add `Result<Void> uploadAvatar(byte[] jpeg);` (POST multipart to `/gameServer/changeAvatar`), `Result<Void> changeColor(String color);`, `Result<Void> changeEmailPreference(boolean on);`, `Result<Void> changeAdsPreference(boolean personalize);`. Delete `SettingsActivity.UploadAvatarTask` (245-323), `ChangeColorTask` (325-376), `ChangeEmailPreferenceTask` (378-431), `ChangeAdsPersonalizationPreferenceTask` (433-end); remove `if(development)` at 449. Keep `scaleBitmap`/JPEG compression in the Activity, pass the resulting `byte[]` to `uploadAvatar`. Commit `feat(net): migrate settings prefs/avatar to PenteApi`.
- [ ] **Step 11: social search (mobile/followers.jsp + social?)** — There is no existing DTO for this (today `SocialActivity:339-344` splits a delimited/HTML body), so FIRST add a new DTO `JsonModels.Followers` whose fields match that split format, then add `Result<JsonModels.Followers> followers(String game);` (GET `/gameServer/mobile/followers.jsp`) and `Result<Void> follow(String name, boolean follow);` (GET `/gameServer/social?...`). Delete `SocialActivity.LoadFollowersingTask` (280-367) and `FollowersingTask` (369-end); drop cookie filters. Commit `feat(net): migrate social followers/follow to PenteApi`.
- [ ] **Step 12: register (/join)** — Add `Result<Boolean> register(String name,String password,String email);` (impl POSTs form `name=..&registerPassword=..` to `baseUrl()+"/join"` per RegisterActivity:142-146, returns `ok(true)` on HTTP 200). Delete `RegisterActivity.RegisterTask` (120-end). Commit `feat(net): migrate register to PenteApi.register()`.
- [ ] **Step 13: FCM token (gameServer/notification)** — Add `Result<Void> registerDevice(String token);` (GET `/gameServer/notification?device=android&token="+token`). Delete `MyFcmListenerService.SendRegistrationTask` (59-end) and `LoginActivity.SendTokenTask` (505-end), both calling `registerDevice`; remove `if(development)` at MyFcmListenerService:80 and the `PentePlayer.development` check at MyInstanceIDListenerService:52. Commit `feat(net): migrate FCM token registration to PenteApi.registerDevice()`.
- [ ] **Step 14: avatar (gameServer/avatar)** — Use existing `Result<android.graphics.Bitmap> avatar(String name)`; impl GETs `/gameServer/avatar?name="+name` and decodes via `BitmapFactory.decodeStream(response.body().byteStream())`. Delete `PentePlayer.LoadAvatarTask` (580-632). Commit `feat(net): migrate avatar fetch to PenteApi.avatar()`.
- [ ] **Step 15: FINAL cleanup — delete `development` flag and dual `CookieManager`** — Confirm zero remaining `extends AsyncTask` HTTP callers (`grep -rn 'extends AsyncTask' app/src/main/java` returns only non-network tasks, if any) and zero `new Gson()` outside `Json.java` (`grep -rn 'new Gson()' app/src/main/java`). Then delete `public static Boolean development = false;` at `PentePlayer.java:33` and every remaining `if(development)` / `import static ...development` (LiveGameRoomActivity:3/140/243, SplashActivity:60, MyFcmListenerService:80). Remove the `android.webkit.CookieManager.getInstance().getCookie(...)` + `name2`/`password2` filter loops everywhere (`grep -rn 'CookieManager.getInstance' app/src/main/java`) — the session cookie now lives solely in `Session.cookieJar()`. Run `./gradlew :app:compileDebugJavaWithJavac` then `./gradlew :app:testDebugUnitTest` (expect success), and `git commit -m "refactor(net): remove PentePlayer.development flag and webkit CookieManager dual-cookie hack"`.
  - **NOTE (WebView):** any `WebView` that loads `pente.org` pages (e.g. `MainActivity`/`KingOfTheHillActivity`/`SocialActivity` profile + `ReplyMessageActivity` message HTML, and `LiveGameRoomActivity.BootMeTask`) still relies on `android.webkit.CookieManager` for its own auth. Do NOT delete `webkit.CookieManager` outright — instead add a **one-way sync** that copies the authenticated `name2`/`password2` (or `sid`) cookie out of `Session.cookieJar()` into `android.webkit.CookieManager.getInstance().setCookie("https://www.pente.org/", ...)` before any WebView load. The OkHttp `cookieJar()` remains the single source of truth; webkit is a read-only mirror.

Reference data (verbatim from source): dashboard parses `JsonModels.IndexResponse` (`PentePlayer.java:700`); KotH parses `List<List<JsonModels.KothPlayerEntry>>` (`KingOfTheHillActivity.java:304-305`); register POSTs to `https://www.pente.org/join` (`RegisterActivity.java:146`); messages hit `/gameServer/mymessages` (`ReplyMessageActivity.java:244`); FCM hits `/gameServer/notification?device=android&token=` (`MyFcmListenerService.java:79`); avatar hits `/gameServer/avatar?name=` (`PentePlayer.java:594`).