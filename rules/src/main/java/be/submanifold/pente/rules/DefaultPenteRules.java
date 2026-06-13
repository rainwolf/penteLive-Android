package be.submanifold.pente.rules;

import java.util.List;

public final class DefaultPenteRules implements PenteRules {

    @Override
    public BoardState replay(List<Integer> moves, Variant v, int untilMove) {
        int size = Variants.gridSize(v);
        byte[][] board = new byte[size][size];
        int[] caps = new int[2];                 // caps[0] = white stones removed, caps[1] = black stones removed
        int lastMove = -1;
        int n = Math.min(untilMove, moves == null ? 0 : moves.size());
        for (int i = 0; i < n; i++) {
            int m = moves.get(i);
            int mi = m / size;
            int mj = m % size;
            // Per-move colour assignment mirrors the legacy replay* workers in Game.java:
            // every variant alternates one stone/turn (white on even index) EXCEPT Connect6,
            // which places two stones/turn following the i%4 ∈ {0,3} → white pattern.
            byte color = v.isConnect6()
                    ? (byte) ((((i % 4) == 0) || ((i % 4) == 3)) ? 1 : 2)
                    : (byte) (1 + (i % 2));
            board[mi][mj] = color;
            lastMove = m;
            // Apply captures. Detector dispatch + ORDER mirror the legacy replay* workers in Game.java:
            //   PENTE_PAIR  → detectPenteCapture
            //   KERYO_TRIO  → detectPenteCapture, detectKeryoPenteCapture
            //   POOF        → detectPoof, detectPenteCapture                (replayPoofPenteGame)
            //   KERYO_POOF  → detectPoof, detectKeryoPoof, detectPenteCapture, detectKeryoPenteCapture (replayOPenteGame)
            CaptureRule rule = Variants.captureRule(v);
            switch (rule) {
                case PENTE_PAIR:
                    detectPenteCapture(board, size, mi, mj, color, caps);
                    break;
                case KERYO_TRIO:
                    detectPenteCapture(board, size, mi, mj, color, caps);
                    detectKeryoPenteCapture(board, size, mi, mj, color, caps);
                    break;
                case POOF:
                    detectPoof(board, size, mi, mj, color, caps);
                    detectPenteCapture(board, size, mi, mj, color, caps);
                    break;
                case KERYO_POOF:
                    detectPoof(board, size, mi, mj, color, caps);
                    detectKeryoPoof(board, size, mi, mj, color, caps);
                    detectPenteCapture(board, size, mi, mj, color, caps);
                    detectKeryoPenteCapture(board, size, mi, mj, color, caps);
                    break;
                case NONE:
                default:
                    break;
            }
        }
        return new BoardState(board, caps[0], caps[1], size, lastMove, null, false, false, -1);
    }

    @Override
    public boolean isWin(BoardState s, int color, int lastMove) {
        return false;   // implemented in Task 5
    }

    // ── Capture detectors ─────────────────────────────────────────────────────
    // Faithful ports of Game.detectPenteCapture / detectKeryoPenteCapture / detectPoof / detectKeryoPoof.
    // opp = 1 + (my % 2) (white↔black). caps[0] counts white stones removed, caps[1] black stones removed.

    private static void detectPenteCapture(byte[][] b, int n, int i, int j, byte my, int[] caps) {
        byte opp = (byte) (1 + (my % 2));
        // Capture-count index = OPPONENT index: pair/trio detectors remove the opponent's stones. caps[0] = white removed, caps[1] = black removed.
        int oi = (opp == 1) ? 0 : 1;
        if (i - 3 > -1 && b[i-3][j] == my && b[i-1][j] == opp && b[i-2][j] == opp) { b[i-1][j] = 0; b[i-2][j] = 0; caps[oi] += 2; }
        if (i - 3 > -1 && j - 3 > -1 && b[i-3][j-3] == my && b[i-1][j-1] == opp && b[i-2][j-2] == opp) { b[i-1][j-1] = 0; b[i-2][j-2] = 0; caps[oi] += 2; }
        if (j - 3 > -1 && b[i][j-3] == my && b[i][j-1] == opp && b[i][j-2] == opp) { b[i][j-1] = 0; b[i][j-2] = 0; caps[oi] += 2; }
        if (i + 3 < n && j - 3 > -1 && b[i+3][j-3] == my && b[i+1][j-1] == opp && b[i+2][j-2] == opp) { b[i+1][j-1] = 0; b[i+2][j-2] = 0; caps[oi] += 2; }
        if (i + 3 < n && b[i+3][j] == my && b[i+1][j] == opp && b[i+2][j] == opp) { b[i+1][j] = 0; b[i+2][j] = 0; caps[oi] += 2; }
        if (i + 3 < n && j + 3 < n && b[i+3][j+3] == my && b[i+1][j+1] == opp && b[i+2][j+2] == opp) { b[i+1][j+1] = 0; b[i+2][j+2] = 0; caps[oi] += 2; }
        if (j + 3 < n && b[i][j+3] == my && b[i][j+1] == opp && b[i][j+2] == opp) { b[i][j+1] = 0; b[i][j+2] = 0; caps[oi] += 2; }
        if (i - 3 > -1 && j + 3 < n && b[i-3][j+3] == my && b[i-1][j+1] == opp && b[i-2][j+2] == opp) { b[i-1][j+1] = 0; b[i-2][j+2] = 0; caps[oi] += 2; }
    }

    private static void detectKeryoPenteCapture(byte[][] b, int n, int i, int j, byte my, int[] caps) {
        byte opp = (byte) (1 + (my % 2));
        int oi = (opp == 1) ? 0 : 1;
        if (i - 4 > -1 && b[i-4][j] == my && b[i-1][j] == opp && b[i-2][j] == opp && b[i-3][j] == opp) { b[i-1][j] = 0; b[i-2][j] = 0; b[i-3][j] = 0; caps[oi] += 3; }
        if (i - 4 > -1 && j - 4 > -1 && b[i-4][j-4] == my && b[i-1][j-1] == opp && b[i-2][j-2] == opp && b[i-3][j-3] == opp) { b[i-1][j-1] = 0; b[i-2][j-2] = 0; b[i-3][j-3] = 0; caps[oi] += 3; }
        if (j - 4 > -1 && b[i][j-4] == my && b[i][j-1] == opp && b[i][j-2] == opp && b[i][j-3] == opp) { b[i][j-1] = 0; b[i][j-2] = 0; b[i][j-3] = 0; caps[oi] += 3; }
        if (i + 4 < n && j - 4 > -1 && b[i+4][j-4] == my && b[i+1][j-1] == opp && b[i+2][j-2] == opp && b[i+3][j-3] == opp) { b[i+1][j-1] = 0; b[i+2][j-2] = 0; b[i+3][j-3] = 0; caps[oi] += 3; }
        if (i + 4 < n && b[i+4][j] == my && b[i+1][j] == opp && b[i+2][j] == opp && b[i+3][j] == opp) { b[i+1][j] = 0; b[i+2][j] = 0; b[i+3][j] = 0; caps[oi] += 3; }
        if (i + 4 < n && j + 4 < n && b[i+4][j+4] == my && b[i+1][j+1] == opp && b[i+2][j+2] == opp && b[i+3][j+3] == opp) { b[i+1][j+1] = 0; b[i+2][j+2] = 0; b[i+3][j+3] = 0; caps[oi] += 3; }
        if (j + 4 < n && b[i][j+4] == my && b[i][j+1] == opp && b[i][j+2] == opp && b[i][j+3] == opp) { b[i][j+1] = 0; b[i][j+2] = 0; b[i][j+3] = 0; caps[oi] += 3; }
        if (i - 4 > -1 && j + 4 < n && b[i-4][j+4] == my && b[i-1][j+1] == opp && b[i-2][j+2] == opp && b[i-3][j+3] == opp) { b[i-1][j+1] = 0; b[i-2][j+2] = 0; b[i-3][j+3] = 0; caps[oi] += 3; }
    }

    private static void detectPoof(byte[][] b, int n, int i, int j, byte my, int[] caps) {
        byte opp = (byte) (1 + (my % 2));
        // Capture-count index = MOVER index: poof detectors self-destruct the mover's OWN stones (not the opponent's). caps[0] = white removed, caps[1] = black removed.
        int mi = (my == 1) ? 0 : 1;
        boolean poofed = false;
        if (i - 2 > -1 && i + 1 < n && b[i-1][j] == my && b[i-2][j] == opp && b[i+1][j] == opp) { b[i-1][j] = 0; b[i][j] = 0; caps[mi]++; poofed = true; }
        if (i - 2 > -1 && j - 2 > -1 && i + 1 < n && j + 1 < n && b[i-1][j-1] == my && b[i-2][j-2] == opp && b[i+1][j+1] == opp) { b[i-1][j-1] = 0; b[i][j] = 0; caps[mi]++; poofed = true; }
        if (j - 2 > -1 && j + 1 < n && b[i][j-1] == my && b[i][j-2] == opp && b[i][j+1] == opp) { b[i][j-1] = 0; b[i][j] = 0; caps[mi]++; poofed = true; }
        if (i - 1 > -1 && j - 2 > -1 && i + 2 < n && j + 1 < n && b[i+1][j-1] == my && b[i-1][j+1] == opp && b[i+2][j-2] == opp) { b[i+1][j-1] = 0; b[i][j] = 0; caps[mi]++; poofed = true; }
        if (i + 2 < n && i - 1 > -1 && b[i+1][j] == my && b[i+2][j] == opp && b[i-1][j] == opp) { b[i+1][j] = 0; b[i][j] = 0; caps[mi]++; poofed = true; }
        if (i - 1 > -1 && j - 1 > -1 && i + 2 < n && j + 2 < n && b[i+1][j+1] == my && b[i-1][j-1] == opp && b[i+2][j+2] == opp) { b[i+1][j+1] = 0; b[i][j] = 0; caps[mi]++; poofed = true; }
        if (j + 2 < n && j - 1 > -1 && b[i][j+1] == my && b[i][j-1] == opp && b[i][j+2] == opp) { b[i][j+1] = 0; b[i][j] = 0; caps[mi]++; poofed = true; }
        if (i - 2 > -1 && j - 1 > -1 && i + 1 < n && j + 2 < n && b[i-1][j+1] == my && b[i+1][j-1] == opp && b[i-2][j+2] == opp) { b[i-1][j+1] = 0; b[i][j] = 0; caps[mi]++; poofed = true; }
        // Legacy double-count: in-branch increments count removed opponent-adjacent stones; this bonus +1 counts the placed stone self-poofing. +2 total per single poof event — matches Game.replayPoofPenteGame. DO NOT remove.
        if (poofed) caps[mi]++;
    }

    private static void detectKeryoPoof(byte[][] b, int n, int i, int j, byte my, int[] caps) {
        byte opp = (byte) (1 + (my % 2));
        int mi = (my == 1) ? 0 : 1;
        boolean poofed = false;
        if (i - 3 > -1 && i + 1 < n && b[i-1][j] == my && b[i-2][j] == my && b[i-3][j] == opp && b[i+1][j] == opp) { b[i-2][j] = 0; b[i-1][j] = 0; b[i][j] = 0; caps[mi] += 2; poofed = true; }
        if (i - 3 > -1 && j - 3 > -1 && i + 1 < n && j + 1 < n && b[i-1][j-1] == my && b[i-2][j-2] == my && b[i-3][j-3] == opp && b[i+1][j+1] == opp) { b[i-2][j-2] = 0; b[i-1][j-1] = 0; b[i][j] = 0; caps[mi] += 2; poofed = true; }
        if (j - 3 > -1 && j + 1 < n && b[i][j-1] == my && b[i][j-2] == my && b[i][j-3] == opp && b[i][j+1] == opp) { b[i][j-2] = 0; b[i][j-1] = 0; b[i][j] = 0; caps[mi] += 2; poofed = true; }
        if (i - 1 > -1 && j - 3 > -1 && i + 3 < n && j + 1 < n && b[i+1][j-1] == my && b[i+2][j-2] == my && b[i-1][j+1] == opp && b[i+3][j-3] == opp) { b[i+2][j-2] = 0; b[i+1][j-1] = 0; b[i][j] = 0; caps[mi] += 2; poofed = true; }
        if (i + 3 < n && i - 1 > -1 && b[i+1][j] == my && b[i+2][j] == my && b[i+3][j] == opp && b[i-1][j] == opp) { b[i+2][j] = 0; b[i+1][j] = 0; b[i][j] = 0; caps[mi] += 2; poofed = true; }
        if (i - 1 > -1 && j - 1 > -1 && i + 3 < n && j + 3 < n && b[i+1][j+1] == my && b[i+2][j+2] == my && b[i-1][j-1] == opp && b[i+3][j+3] == opp) { b[i+2][j+2] = 0; b[i+1][j+1] = 0; b[i][j] = 0; caps[mi] += 2; poofed = true; }
        // Guard covers the j+3 access. Legacy Game.java used j+2<n here (latent AIOOBE); the j+3 pattern needs an off-board stone at the boundary so this changes no valid-board outcome.
        if (j + 3 < n && j - 1 > -1 && b[i][j+1] == my && b[i][j+2] == my && b[i][j-1] == opp && b[i][j+3] == opp) { b[i][j+1] = 0; b[i][j+2] = 0; b[i][j] = 0; caps[mi] += 2; poofed = true; }
        if (i - 3 > -1 && j - 1 > -1 && i + 1 < n && j + 3 < n && b[i-1][j+1] == my && b[i-2][j+2] == my && b[i+1][j-1] == opp && b[i-3][j+3] == opp) { b[i-2][j+2] = 0; b[i-1][j+1] = 0; b[i][j] = 0; caps[mi] += 2; poofed = true; }
        // Symmetric patterns unique to keryo-poof (F flanked by two own stones, opponents at both ends). Deliberately absent from detectPoof (pair-only).
        if (i - 2 > -1 && i + 2 < n && b[i-1][j] == my && b[i+1][j] == my && b[i-2][j] == opp && b[i+2][j] == opp) { b[i+1][j] = 0; b[i-1][j] = 0; b[i][j] = 0; caps[mi] += 2; poofed = true; }
        if (i - 2 > -1 && j - 2 > -1 && i + 2 < n && j + 2 < n && b[i-1][j-1] == my && b[i+1][j+1] == my && b[i-2][j-2] == opp && b[i+2][j+2] == opp) { b[i+1][j+1] = 0; b[i-1][j-1] = 0; b[i][j] = 0; caps[mi] += 2; poofed = true; }
        if (j - 2 > -1 && j + 2 < n && b[i][j-1] == my && b[i][j+1] == my && b[i][j-2] == opp && b[i][j+2] == opp) { b[i][j+1] = 0; b[i][j-1] = 0; b[i][j] = 0; caps[mi] += 2; poofed = true; }
        if (i - 2 > -1 && j - 2 > -1 && i + 2 < n && j + 2 < n && b[i+1][j-1] == my && b[i-1][j+1] == my && b[i-2][j+2] == opp && b[i+2][j-2] == opp) { b[i+1][j-1] = 0; b[i-1][j+1] = 0; b[i][j] = 0; caps[mi] += 2; poofed = true; }
        // Legacy double-count: in-branch increments count removed opponent-adjacent stones; this bonus +1 counts the placed stone self-poofing. +2 total per single poof event — matches Game.replayPoofPenteGame. DO NOT remove.
        if (poofed) caps[mi]++;
    }
}
