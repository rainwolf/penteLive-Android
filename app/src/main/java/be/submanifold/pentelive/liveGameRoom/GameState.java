package be.submanifold.pentelive.liveGameRoom;


import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class GameState {
    public State state = State.NOTSTARTED;
    public DPenteState dPenteState = DPenteState.NOCHOICE;
    public Swap2State swap2State = Swap2State.NOCHOICE;
    public GoState goState = GoState.PLAY;
    public Map<Integer, Map<String, Long>> timers = new ConcurrentHashMap<>();

    public GameState() {
        Map<String, Long> timer = new HashMap<>();
        timer.put("millis", 0L);
        timers.put(1, timer);
        timer = new HashMap<>();
        timer.put("millis", 0L);
        timers.put(2, timer);
    }
}
