package be.submanifold.pentelive.db;

import be.submanifold.pentelive.db.DbDtos.NextMove;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Red-yellow-green heat colours over the displayed candidate set (spec 3.2). */
public final class HeatScale {
    private HeatScale() {}

    public static int argb(double t) {
        double c = Math.max(0.0, Math.min(1.0, t));
        int r;
        int g;
        if (c <= 0.5) {
            r = 255;
            g = (int) Math.round(510.0 * c);
        } else {
            r = (int) Math.round(510.0 - 510.0 * c);
            g = 255;
        }
        return 0xFF000000 | (r << 16) | (g << 8);
    }

    /** cell -> ARGB in input order; occupied cells are excluded before min-max. */
    public static Map<Integer, Integer> colours(List<NextMove> topN, Set<Integer> occupied) {
        Map<Integer, Integer> out = new LinkedHashMap<>();
        List<NextMove> usable = new ArrayList<>();
        for (NextMove m : topN) {
            if (!occupied.contains(m.move)) usable.add(m);
        }
        if (usable.isEmpty()) return out;
        double min = Double.MAX_VALUE;
        double max = -Double.MAX_VALUE;
        for (NextMove m : usable) {
            min = Math.min(min, m.winPct);
            max = Math.max(max, m.winPct);
        }
        for (NextMove m : usable) {
            double t = max == min ? m.winPct / 100.0 : (m.winPct - min) / (max - min);
            out.put(m.move, argb(t));
        }
        return out;
    }
}
