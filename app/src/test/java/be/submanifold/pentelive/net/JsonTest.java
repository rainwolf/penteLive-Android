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
        JsonModels.IndexResponse.PlayerInfo info = new JsonModels.IndexResponse.PlayerInfo();
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
        JsonModels.IndexResponse.PlayerInfo info = new JsonModels.IndexResponse.PlayerInfo();
        info.name = "alice";
        info.color = 1;
        info.subscriber = true;

        String json = Json.GSON.toJson(info);
        JsonModels.IndexResponse.PlayerInfo back =
            Json.GSON.fromJson(json, JsonModels.IndexResponse.PlayerInfo.class);

        assertEquals("alice", back.name);
        assertEquals(1, back.color);
        assertTrue(back.subscriber);
    }
}
