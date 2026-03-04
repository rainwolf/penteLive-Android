package be.submanifold.pentelive;

import java.util.List;

/**
 * POJOs for deserializing mobile JSON endpoint responses via Gson.
 */
public class JsonModels {

    // ── mobile/json/index.jsp (dashboard) ─────────────────────────────────────

    public static class IndexResponse {
        public Settings settings;
        public PlayerInfo player;
        public List<KothEntry> kingOfTheHill;
        public List<RatingStatEntry> ratingStats;
        public List<InvitationEntry> invitationsReceived;
        public List<InvitationEntry> invitationsSent;
        public List<GameEntry> activeGamesMyTurn;
        public List<GameEntry> activeGamesOpponentTurn;
        public List<OpenInvitationEntry> openInvitationGames;
        public List<MessageEntry> messages;
        public List<TournamentEntry> tournaments;
        public List<String> onlinePlayers;

        public static class Settings {
            public boolean noAds;
            public boolean unlimitedGames;
            public int tbGamesLimit;
        }

        public static class PlayerInfo {
            public String name;
            public int color;
            public boolean showAds;
            public boolean subscriber;
            public int livePlayers;
            public boolean dbAccess;
            public boolean emailMe;
            public int onlineFollowing;
            public boolean personalizeAds;
        }

        public static class KothEntry {
            public int numPlayers;
            public boolean amIMember;
            public boolean iAmKing;
            public String kingName;
            public boolean canChallenge;
            public int gameId;
        }

        public static class RatingStatEntry {
            public String gameName;
            public int rating;
            public int totalGames;
            public int tourneyWinner;
            public String lastGameDate;
            public int gameId;
        }

        public static class InvitationEntry {
            public long setId;
            public String gameName;
            public String opponentName;
            public int opponentRating;
            public String color;
            public int daysPerMove;
            public String rated;
            public int opponentColor;
            public int opponentTourneyWinner;
        }

        public static class GameEntry {
            public long gid;
            public String gameName;
            public String opponentName;
            public int opponentRating;
            public String color;
            public int numMoves;
            public String timeLeft;
            public String rated;
            public int opponentColor;
            public int opponentTourneyWinner;
        }

        public static class OpenInvitationEntry {
            public long setId;
            public String gameName;
            public String inviterName;
            public int inviterRating;
            public String color;
            public int daysPerMove;
            public String rated;
            public int inviterColor;
            public int inviterTourneyWinner;
        }

        public static class MessageEntry {
            public int mid;
            public boolean read;
            public String subject;
            public String from;
            public String date;
            public int fromColor;
            public int fromTourneyWinner;
        }

        public static class TournamentEntry {
            public String name;
            public long eventId;
            public int numRounds;
            public String gameName;
            public int status;
            public String date;
        }
    }

    // ── mobile/json/game.jsp (single turn-based game) ─────────────────────────

    public static class GameResponse {
        public String gid;
        public String privateGame;
        public String rated;
        public String gameName;
        public String moves;
        public PlayerRef player1;
        public PlayerRef player2;
        public String messages;
        public String messageNums;
        public Long sid;
        public String currentPlayer;
        public String seqNums;
        public String dates;
        public String players;
        public String state;
        public String goState;
        public Boolean undoRequested;
        public Boolean canHide;
        public Boolean canUnHide;
        public CancelInfo cancel;
        public String dPenteState;
        public Boolean swap2pass;

        public static class PlayerRef {
            public String name;
            public int rating;
        }

        public static class CancelInfo {
            public String name;
            public String message;
        }
    }

    // ── mobile/json/whosonlineandlive.jsp (List<RoomEntry>) ───────────────────

    public static class RoomEntry {
        public String name;
        public List<OnlinePlayerEntry> players;
    }

    // ── mobile/json/liveServers.jsp (List<ServerEntry>) ───────────────────────

    public static class ServerEntry {
        public int port;
        public String name;
        public int playerCount;
        public List<OnlinePlayerEntry> players;
    }

    public static class OnlinePlayerEntry {
        public String name;
        public int rating;
        public int color;
        public int tourneyWinner;
        public int totalGames;
    }

    // ── mobile/json/koth.jsp (List<List<KothPlayerEntry>>) ────────────────────

    public static class KothPlayerEntry {
        public String name;
        public int rating;
        public boolean canChallenge;
        public int color;
        public int tourneyWinner;
        public String lastGame;
    }
}
