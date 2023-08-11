package be.submanifold.pentelive.liveGameRoom;


import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class GameState {
    public State state = State.NOTSTARTED;
    public DPenteState dPenteState = DPenteState.NOCHOICE;
    public Swap2State swap2State = Swap2State.NOCHOICE;
    public GoState goState = GoState.PLAY;
    public Map<Integer, Map<String, Integer>> timers = new ConcurrentHashMap<>();

    public GameState() {
        Map<String, Integer> timer = new HashMap<>();
        timer.put("minutes", 0);
        timer.put("seconds", 0);
        timers.put(1, timer);
        timer = new HashMap<>();
        timer.put("minutes", 0);
        timer.put("seconds", 0);
        timers.put(2, timer);
    }
}
