package be.submanifold.pentelive.liveGameRoom;

import android.content.Context;
import android.content.Intent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import be.submanifold.pentelive.MyApplication;
import be.submanifold.pentelive.PrefUtils;
import be.submanifold.pentelive.R;

/**
 * Created by waliedothman on 08/01/2017.
 */

public class TablesAndPlayers {
    public Map<String, LivePlayer> players = new HashMap<>();
    public Map<Integer, Table> tables = new HashMap<>();
    public String mainRoomText = "";
    private final Context ctx = MyApplication.getContext();

    private String me = PrefUtils.getFromPrefs(ctx, PrefUtils.PREFS_LOGIN_USERNAME_KEY, "guest").toLowerCase();

    public String getMe() {
        return me;
    }

    public void joinMainRoom(Map<String, ?> data) {
        String playerName = (String) data.get("player");
        Map<String, ?> playerData = (Map<String, ?>) data.get("dsgPlayerData");
        List<Map<String, Object>> gameData = (List<Map<String, Object>>) playerData.get("gameData");
        Map<String, Object> colorData = (Map<String, Object>) playerData.get("nameColor");
        int color = 0;
        if (colorData != null) {
            color = (int) colorData.get("value");
        }
        boolean subscriber = false;
        subscriber = (Boolean) playerData.get("unlimitedTBGames");
        LivePlayer player = new LivePlayer(playerName, subscriber, 0, color);

        int myCrown = 0, myKotHCrown = 0;
        for (Map<String, Object> singleGame : gameData) {
            if ("N".equals(singleGame.get("computer"))) {
                int game = (int) singleGame.get("game");
                int rating = (int) ((double) singleGame.get("rating"));
                player.addRating(game, rating);
                int tourneyWinner = (int) singleGame.get("tourneyWinner");
                if (tourneyWinner > 0) {
                    if (tourneyWinner == 4) {
                        myKotHCrown = myKotHCrown + 1;
                    } else if (0 == myCrown) {
                        myCrown = tourneyWinner;
                    } else if (tourneyWinner < myCrown) {
                        myCrown = tourneyWinner;
                    }
                }
            }
        }
        if (myCrown > 0) {
            player.setCrown(myCrown);
        } else if (myKotHCrown > 0) {
            player.setCrown(myKotHCrown + 3);
        } else {
            player.setCrown(0);
        }
        players.put(playerName, player);
        mainRoomText = mainRoomText + ctx.getString(R.string.has_joined_main_room, playerName) + "\n";
    }

    public void addMainRoomText(Map<String, ?> data) {
        String playerName = (String) data.get("player");
        String text = (String) data.get("text");
        mainRoomText = mainRoomText + playerName + ": " + text + "\n";
    }

    public void exitMainRoom(Map<String, ?> data) {
        String playerName = (String) data.get("player");
        players.remove(playerName);
        mainRoomText = mainRoomText + ctx.getString(R.string.has_exited_main_room, playerName) + "\n";
    }

    public void updatePlayerData(Map<String, ?> data) {
        Map<String, ?> playerData = (Map<String, ?>) data.get("data");
        String playerName = (String) playerData.get("name");
        List<Map<String, Object>> gameData = (List<Map<String, Object>>) playerData.get("gameData");
        Map<String, Object> colorData = (Map<String, Object>) playerData.get("nameColor");
        int color = 0;
        if (colorData != null) {
            color = (int) colorData.get("value");
        }
        boolean subscriber = false;
        subscriber = (Boolean) playerData.get("unlimitedTBGames");
        LivePlayer player = new LivePlayer(playerName, subscriber, 0, color);

        int myCrown = 0, myKotHCrown = 0;
        for (Map<String, Object> singleGame : gameData) {
            if (singleGame.get("computer").equals("N")) {
                int game = (int) singleGame.get("game");
                int rating = (int) ((double) singleGame.get("rating"));
                player.addRating(game, rating);
                int tourneyWinner = (int) singleGame.get("tourneyWinner");
                if (tourneyWinner > 0) {
                    if (tourneyWinner == 4) {
                        myKotHCrown = myKotHCrown + 1;
                    } else if (0 == myCrown) {
                        myCrown = tourneyWinner;
                    } else if (tourneyWinner < myCrown) {
                        myCrown = tourneyWinner;
                    }
                }
            }
        }
        if (myCrown > 0) {
            player.setCrown(myCrown);
        } else if (myKotHCrown > 0) {
            player.setCrown(myKotHCrown + 3);
        } else {
            player.setCrown(0);
        }
        players.put(playerName, player);
    }

    public void login(Map<String, ?> data) {
        Map<String, ?> serverData = (Map<String, ?>) data.get("serverData");
        List<String> messages = (List<String>) serverData.get("loginMessages");
        if (messages != null) {
            for (String message : messages) {
                mainRoomText = mainRoomText + message + "\n";
            }
        }
        this.me = (String) data.get("player");
    }

    public int changeTableState(Map<String, Object> data) {
        boolean timed = (Boolean) data.get("timed");
        int initialMinutes = (Integer) data.get("initialMinutes");
        int incrementalSeconds = (Integer) data.get("incrementalSeconds");
        boolean rated = (Boolean) data.get("rated");
        int game = (Integer) data.get("game");
        boolean open = data.get("tableType").equals(1);
        int tableId = (Integer) data.get("table");
        Table table = tables.get(tableId);
        if (table == null) {
            table = new Table();
            tables.put(tableId, table);
        }
        table.setTimed(timed);
        table.getTimer().put("initialMinutes", initialMinutes);
        table.getTimer().put("incrementalSeconds", incrementalSeconds);
        table.setRated(rated);
        table.setGame(game);
        table.setOpen(open);
        table.setId(tableId);
        table.resetState();
        return tableId;
    }

    public Table tableJoin(int tableId, String player) {
        LivePlayer livePlayer = players.get(player);
        Table table = tables.get(tableId);
        if (table == null) {
            table = new Table();
            table.setId(tableId);
            tables.put(tableId, table);
        }
        if (livePlayer != null) {
            table.getPlayers().put(player, livePlayer);
        }
        if (table.getPlayers().size() == 1) {
            table.setOwner(player);
        }
        if (me.equals(player)) {
            return table;
        }
        return null;
    }

    public void tableOwner(int tableId, String player) {
        Table table = tables.get(tableId);
        if (table != null) {
            table.setOwner(player);
        }
    }

    public int tableSit(Map<String, Object> data) {
        String player = (String) data.get("player");
        int tableId = (Integer) data.get("table");
        int seat = (Integer) data.get("seat");
        Table table = tables.get(tableId);
        if (table != null) {
            table.sit(seat, player);
        }
        return tableId;
    }

    public int tableStand(Map<String, Object> data) {
        String player = (String) data.get("player");
        int tableId = (Integer) data.get("table");
        Table table = tables.get(tableId);
        if (table != null) {
            table.stand(player);
        }
        return tableId;
    }

    public boolean tableExit(int tableId, String player) {
        Table table = tables.get(tableId);
        if (table != null) {
            table.exit(player);
            if (table.getPlayers().size() == 0) {
                tables.remove(tableId);
            }
        }
        return me.equals(player);
    }

    public void updateGameState(int tableId, int state) {
        Table table = tables.get(tableId);
        if (table != null) {
            table.updateGameState(state);
        }
    }

    public void updateTableTimer(Map<String, Object> data) {
        int tableId = (int) data.get("table");
        int minutes = (int) data.get("minutes");
        int seconds = (int) data.get("seconds");
        String playerName = (String) data.get("player");
        Table table = tables.get(tableId);
        if (table != null) {
            Map<Integer, LivePlayer> seats = table.getSeats();
            LivePlayer player = seats.get(1);
            int seat = 2;
            if (player != null && player.getName().equals(playerName)) {
                seat = 1;
            }
            table.updateTimer(false, seat, minutes, seconds);
        }
    }

    public void undoMove(int tableId) {
        Table table = tables.get(tableId);
        if (table != null) {
            table.undoMove();
        }
    }


}
