package be.submanifold.pentelive.db;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

final class RecordingDbView implements DbView {
    final List<String> log = new ArrayList<>();
    final List<Map<Integer, Integer>> dots = new ArrayList<>();
    final List<List<DbController.MoveRow>> rows = new ArrayList<>();
    final List<List<Integer>> positions = new ArrayList<>();
    final List<DbController.Endpoint> errors = new ArrayList<>();
    final List<List<DbDtos.GameHeader>> games = new ArrayList<>();
    final List<Integer> totals = new ArrayList<>();
    final List<Boolean> hasMores = new ArrayList<>();

    int count(String name) {
        int n = 0;
        for (String s : log) if (s.equals(name)) n++;
        return n;
    }

    @Override
    public void showPosition(List<Integer> moves, CaptureIcons.Counts caps) {
        log.add("showPosition");
        positions.add(new ArrayList<>(moves));
    }

    @Override
    public void showDots(Map<Integer, Integer> cellArgb) {
        log.add("showDots");
        dots.add(cellArgb);
    }

    @Override
    public void showMoves(List<DbController.MoveRow> r) {
        log.add("showMoves");
        rows.add(r);
    }

    @Override
    public void clearResults() {
        log.add("clearResults");
    }

    @Override
    public void showBlocked() {
        log.add("showBlocked");
    }

    @Override
    public void showUnsupported() {
        log.add("showUnsupported");
    }

    @Override
    public void showLoginPrompt() {
        log.add("showLoginPrompt");
    }

    @Override
    public void showSubscribeGate() {
        log.add("showSubscribeGate");
    }

    @Override
    public void showError(DbController.Endpoint e) {
        log.add("showError");
        errors.add(e);
    }

    final List<Boolean> serverFilters = new ArrayList<>();

    @Override
    public void showServerFilters(boolean enabled) {
        log.add("showServerFilters");
        serverFilters.add(enabled);
    }

    @Override
    public void showGames(List<DbDtos.GameHeader> games, int total, boolean hasMore) {
        log.add("showGames");
        this.games.add(new ArrayList<>(games));
        totals.add(total);
        hasMores.add(hasMore);
    }

    final List<Integer> selections = new ArrayList<>();

    @Override
    public void showSelection(Integer cell) {
        log.add("showSelection");
        selections.add(cell);
    }
}
