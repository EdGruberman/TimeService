package edgruberman.bukkit.timeservice.commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.ChatPaginator;
import org.bukkit.util.ChatPaginator.ChatPage;

import edgruberman.bukkit.timeservice.Horologist;
import edgruberman.bukkit.timeservice.Main;

public final class TimeZone extends Executor {

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
            Main.courier.send(sender, "timeZoneCurrent", sender.getName(), zone.getID(), zone.getDisplayName());
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
            // ID filter

            // populate initial query with all possible IDs
            if (available.size() == 0)
                available.addAll(Arrays.asList(java.util.TimeZone.getAvailableIDs()));

            // reduce list with supplied filter
            final Iterator<String> i = available.iterator();
            while (i.hasNext()) {
                final String id = i.next();
                if (id.equalsIgnoreCase(value)) {
                    available.clear();
                    available.add(id);
                    break;

                } else if (!id.toLowerCase().contains(value.toLowerCase())) {
                    i.remove();
                }
            }

        }

        if (available.size() == 0) {
            Main.courier.send(sender, "timeZoneNotFound", value);
            return true;

        } else if (available.size() > 1) {
            // time zone is ambiguous, show possibilities
            final ChatPage page = ChatPaginator.paginate(Executor.join(available, "\n"), ( args.size() >= 2 ? Integer.valueOf(args.get(1)) : 1 )
                    , ChatPaginator.GUARANTEED_NO_WRAP_CHAT_PAGE_WIDTH, ChatPaginator.CLOSED_CHAT_PAGE_HEIGHT - 1);

            for (int i = 0; i < page.getLines().length; i++)
                Main.courier.send(sender, "timeZoneResult.line", page.getLines()[i], java.util.TimeZone.getTimeZone(page.getLines()[i]).getDisplayName());

            Main.courier.send(sender, "timeZoneResult.footer", page.getPageNumber(), page.getTotalPages(), available.size());
            return true;
        }

        // set time zone
        final java.util.TimeZone zone = java.util.TimeZone.getTimeZone(available.get(0));
        Main.horologist.set(sender.getName(), zone);
        Main.courier.send(sender, "timeZoneSuccess", sender.getName(), zone.getID(), zone.getDisplayName());
        return true;
    }

    private static boolean isDouble(final String s) {
        try {
            Double.parseDouble(s);
            return true;

        } catch(final Exception e) {
            return false;
        }
    }

}
