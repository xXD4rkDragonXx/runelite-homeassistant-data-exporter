package haexporterplugin.utils;

import com.google.common.hash.HashCode;
import haexporterplugin.enums.AccountType;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.gameval.VarbitID;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Stream;


@Slf4j
@UtilityClass
public class Utils {
    public AccountType getAccountType(Client client) {
        return AccountType.get(client.getVarbitValue(VarbitID.IRONMAN));
    }

    // DO NOT CHANGE: changing the salt changes every user's accountHash, so receivers would see every account as new.
    private final byte[] ACCOUNT_HASH_SALT = "HAExporter-AccountHash-v1".getBytes(StandardCharsets.UTF_8);

    /**
     * Same approach as Utils#dinkHash in Dink Plugin, but with our own salt.
     * source: <a href="https://github.com/pajlads/DinkPlugin/blob/master/src/main/java/dinkplugin/util/Utils.java"></a>
     *
     * @param accountHash the value of {@link Client#getAccountHash()}
     * @return a salted SHA-224 hex digest, or null when not logged in (-1) or SHA-224 is unavailable
     */
    @Nullable
    public String accountHash(long accountHash) {
        if (accountHash == -1) return null;

        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-224");
        } catch (NoSuchAlgorithmException e) {
            log.warn("Account hash could not be computed", e);
            return null;
        }

        byte[] input = ByteBuffer.allocate(8 + ACCOUNT_HASH_SALT.length)
                .putLong(accountHash)
                .put(ACCOUNT_HASH_SALT)
                .array();
        return HashCode.fromBytes(digest.digest(input)).toString();
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
