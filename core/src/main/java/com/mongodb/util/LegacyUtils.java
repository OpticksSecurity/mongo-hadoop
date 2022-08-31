package com.mongodb.util;

import java.util.regex.Pattern;

public class LegacyUtils {

    // --- regex flags

    private static final int FLAG_GLOBAL = 256;

    private static final int[] FLAG_LOOKUP = new int[Character.MAX_VALUE];

    static {
        FLAG_LOOKUP['g'] = FLAG_GLOBAL;
        FLAG_LOOKUP['i'] = Pattern.CASE_INSENSITIVE;
        FLAG_LOOKUP['m'] = Pattern.MULTILINE;
        FLAG_LOOKUP['s'] = Pattern.DOTALL;
        FLAG_LOOKUP['c'] = Pattern.CANON_EQ;
        FLAG_LOOKUP['x'] = Pattern.COMMENTS;
        FLAG_LOOKUP['d'] = Pattern.UNIX_LINES;
        FLAG_LOOKUP['t'] = Pattern.LITERAL;
        FLAG_LOOKUP['u'] = Pattern.UNICODE_CASE;
    }

    /**
     * Converts a sequence of regular expression modifiers from the database into Java regular expression flags.
     *
     * @param s regular expression modifiers
     * @return the Java flags
     * @throws IllegalArgumentException If sequence contains invalid flags.
     */
    static int regexFlags(final String s) {
        int flags = 0;

        if (s == null) {
            return flags;
        }

        for (final char f : s.toLowerCase().toCharArray()) {
            flags |= regexFlag(f);
        }

        return flags;
    }

    /**
     * Converts a regular expression modifier from the database into Java regular expression flags.
     *
     * @param c regular expression modifier
     * @return the Java flags
     * @throws IllegalArgumentException If sequence contains invalid flags.
     */
    private static int regexFlag(final char c) {

        int flag = FLAG_LOOKUP[c];

        if (flag == 0) {
            throw new IllegalArgumentException(String.format("Unrecognized flag [%c]", c));
        }

        return flag;
    }

    /**
     * Converts Java regular expression flags into regular expression modifiers from the database.
     *
     * @param flags the Java flags
     * @return the Java flags
     * @throws IllegalArgumentException if some flags couldn't be recognized.
     */
    static String regexFlags(final int flags) {
        int processedFlags = flags;
        StringBuilder buf = new StringBuilder();

        for (int i = 0; i < FLAG_LOOKUP.length; i++) {
            if ((processedFlags & FLAG_LOOKUP[i]) > 0) {
                buf.append((char) i);
                processedFlags -= FLAG_LOOKUP[i];
            }
        }

        if (processedFlags > 0) {
            throw new IllegalArgumentException("Some flags could not be recognized.");
        }

        return buf.toString();
    }
}
