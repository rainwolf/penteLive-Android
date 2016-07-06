package be.submanifold.pentelive;

/**
 * Created by waliedothman on 06/07/16.
 */
public class KothPlayer {
    private String name, rating, lastGame;
    private boolean canBeChallenged;
    private int crown, color;

    public KothPlayer(String name, String rating, String lastGame, boolean canBeChallenged, int crown, int color) {
        this.name = name;
        this.rating = rating;
        this.lastGame = lastGame;
        this.canBeChallenged = canBeChallenged;
        this.crown = crown;
        this.color = color;
    }

    public boolean isCanBeChallenged() {
        return canBeChallenged;
    }

    public String getName() {
        return name;
    }

    public String getRating() {
        return rating;
    }

    public String getLastGame() {
        return lastGame;
    }

    public int getCrown() {
        return crown;
    }

    public int getColor() {
        return color;
    }
}
