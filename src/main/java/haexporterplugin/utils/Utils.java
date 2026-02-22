package haexporterplugin.utils;

import haexporterplugin.enums.AccountType;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.gameval.VarbitID;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Stream;


@Slf4j
@UtilityClass
public class Utils {
    public AccountType getAccountType(Client client) {
        return AccountType.get(client.getVarbitValue(VarbitID.IRONMAN));
    }

    private final Pattern DELIM = Pattern.compile("[,;\\n]");

    public Stream<String> readDelimited(String value) {
        if (value == null) return Stream.empty();
        return DELIM.splitAsStream(value)
                .map(String::trim)
                .filter(StringUtils::isNotEmpty);
    }

    /**
     * Converts simple patterns (asterisk is the only special character) into regexps.
     *
     * @param pattern a simple pattern (asterisks are wildcards, and the rest is a string literal)
     * @return a compiled regular expression associated with the simple pattern
     */
    @Nullable
    public Pattern regexify(@NotNull String pattern) {
        final int len = pattern.length();
        final StringBuilder sb = new StringBuilder(len + 2 + 4);
        int startIndex = 0;

        if (!pattern.startsWith("*")) {
            sb.append('^');
        } else {
            startIndex++;
        }

        int i;
        while ((i = pattern.indexOf('*', startIndex)) >= 0) {
            String section = pattern.substring(startIndex, i);
            sb.append(Pattern.quote(section));
            sb.append(".*");
            startIndex = i + 1;
        }

        if (startIndex < len) {
            sb.append(Pattern.quote(pattern.substring(startIndex)));
            sb.append('$');
        }

        try {
            return Pattern.compile(sb.toString(), Pattern.CASE_INSENSITIVE);
        } catch (PatternSyntaxException e) {
            log.warn("Failed to parse pattern: {}", pattern, e);
            return null;
        }
    }
}
