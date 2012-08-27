package edgruberman.bukkit.timeservice.commands;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import edgruberman.bukkit.timeservice.Horologist;
import edgruberman.bukkit.timeservice.Main;

public final class TimeZone extends Executor {

    private static final int PAGE_SIZE = 9;

    private final List<Pattern> exclude;

    public TimeZone(final List<Pattern> exclude) {
        this.exclude = exclude;
    }

    // usage: /<command>[ <TimeZone>[ <Page>]]
    @Override
    public boolean execute(final CommandSender sender, final Command command, final String label, final List<String> args) {
        if (!(sender instanceof Player)) {
            Main.courier.send(sender, "requiresPlayer", label);
            return true;
        }

        final String value = ( args.size() >= 1 ? args.get(0): null );
        if (value == null) {
            // show current zone for sender
            final java.util.TimeZone zone = Horologist.getTimeZone((Player) sender);
            Main.courier.send(sender, "timeZoneCurrent"
                    , sender.getName(), zone.getID(), zone.getDisplayName()
                    , TimeZone.formatOffset(zone.getRawOffset()), TimeZone.formatOffset(zone.getDSTSavings()));
            return true;
        }

        // set or show matches
        final List<String> available = new ArrayList<String>();

        if (TimeZone.isDouble(value)) {
            // offset filter in hours
            final int offset = (int) (Double.parseDouble(value) * 60 * 60 * 1000);
            available.clear();
            available.addAll(Arrays.asList(java.util.TimeZone.getAvailableIDs(offset)));

        } else if (value.matches("\\d{1,2}:\\d{1,2}")) {
            // offset filter in HH:mm format
            final Matcher m = Pattern.compile("(\\d{1,2}):(\\d{1,2})").matcher(value);
            m.find();
            final int offset = ((Integer.parseInt(m.group(1)) * 60) + Integer.parseInt(m.group(2))) * 60 * 1000;
            available.clear();
            available.addAll(Arrays.asList(java.util.TimeZone.getAvailableIDs(offset)));

        } else {
            // id/name filter
            for (final String id : java.util.TimeZone.getAvailableIDs()) {
                if (this.isExcluded(id)) continue;

                if (id.toLowerCase().equals(value.toLowerCase())) {
                    // case insensitive id match
                    available.clear();
                    available.add(id);
                    break;
                }

                if (TimeZone.matches(id, value)) available.add(id);
            }
        }

        if (available.size() == 0) {
            Main.courier.send(sender, "timeZoneNotFound", value);
            return true;

        } else if (available.size() > 1) {
            // time zone is ambiguous, show possibilities
            final int pageCurrent = ( args.size() >= 2 ? TimeZone.parseInt(args.get(1), 1) : 1 );
            final int pageTotal = (available.size() / TimeZone.PAGE_SIZE) + 1;
            final int lineFirst = (pageCurrent - 1) * TimeZone.PAGE_SIZE;
            final int lineLast = Math.min(lineFirst + TimeZone.PAGE_SIZE - 1, available.size() - 1);

            for (int i = lineFirst; i <= lineLast; i++)
                Main.courier.send(sender, "timeZoneResult.line"
                        , available.get(i), java.util.TimeZone.getTimeZone(available.get(i)).getDisplayName());

            Main.courier.send(sender, "timeZoneResult.footer", pageCurrent, pageTotal, available.size());
            return true;
        }

        // set time zone
        final java.util.TimeZone zone = java.util.TimeZone.getTimeZone(available.get(0));
        Main.horologist.set(sender.getName(), zone);
        Main.courier.send(sender, "timeZoneSuccess", sender.getName(), zone.getID(), zone.getDisplayName());
        this.execute(sender, command, label, Collections.<String>emptyList());
        return true;
    }

    private boolean isExcluded(final String id) {
        for (final Pattern pattern : this.exclude)
            if (pattern.matcher(id).find())
                return true;

        return false;
    }

    private static boolean isDouble(final String s) {
        try {
            Double.parseDouble(s);
            return true;

        } catch(final Exception e) {
            return false;
        }
    }

    private static boolean matches(final String id, final String search) {
        final String compare = search.toLowerCase();
        if (id.toLowerCase().contains(compare)) return true;

        if (java.util.TimeZone.getTimeZone(id).getDisplayName().toLowerCase().contains(compare)) return true;

        return false;
    }

    private static Integer parseInt(final String s, final Integer def) {
        try { return Integer.parseInt(s);
        } catch (final NumberFormatException e) { return def; }
    }

    /** @param offset milliseconds */
    private static String formatOffset(final int offset) {
        long total = Math.abs(TimeUnit.MILLISECONDS.toMinutes(offset));

        final long minutes = total % 60;  total /= 60;
        final long hours = total % 24;    total /= 24;

        return MessageFormat.format("{0,choice,-1#-|0#+}{1,number,00}:{2,number,00}", offset, hours, minutes);
     }

}
