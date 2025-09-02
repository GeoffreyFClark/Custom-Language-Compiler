package plc.homework;

import java.util.regex.Pattern;

/**
 * Contains {@link Pattern} constants, which are compiled regular expressions.
 * See the assignment page for resources on regexes as needed.
 */
public class Regex {

    public static final Pattern

            // Provided Email Regex
            EMAIL = Pattern.compile("[A-Za-z0-9._-]+@[A-Za-z0-9-]*\\.[a-z]{2,3}"),

            // ^ and $ mean start and end of the string.
            // . matches any character (except newline).
            // {11}, {13}, etc. mean "exactly this many characters".
            // (a|b|c) means "a OR b OR c". (?: … ) = non-capturing group (just groups, doesn’t save).
            // Altogether: match strings that are exactly 11, 13, 15, 17, or 19 chars long.
            ODD_STRINGS = Pattern.compile("^(?:.{11}|.{13}|.{15}|.{17}|.{19})$"),

            // ^\[ and \]$ match literal square brackets at start and end.
            // Inside: ( … )? means the contents are optional (so [] is valid).
            // [1-9]\d* = a positive integer (no leading zeros).
            //
            // (?:, ?[1-9]\d*)*
            //   - , is just a literal comma (no escape needed).
            //   -  ? = optional space after the comma.
            //   - [1-9]\d* once again = another positive integer.
            //   - ( … )* = repeat this whole "comma + optional space + integer" zero or more times.
            //   - (?: … ) non-capturing group.
            INTEGER_LIST = Pattern.compile("^\\[(?:[1-9]\\d*(?:, ?[1-9]\\d*)*)?\\]$"),

            // ^ and $ again to anchor the string start/end.
            // -? optional minus sign.
            // (0|[1-9]\d*) = either just 0 OR a nonzero number (no leading zeros).
            // \. = literal dot.
            // \d+ = one or more digits.
            DECIMAL = Pattern.compile("^-?(?:0|[1-9]\\d*)\\.\\d+$"),

            // ^" and "$ force the string to start and end with quotes.
            // Inside the quotes, two different options, separated by |:
            //
            //   [^"\\\r\n]
            //   - [^ … ] = "anything except".
            //   - So this matches any char except: " (quote), \ (backslash), \r (carriage return), \n (newline).
            //
            //   \\\\[bnrt'\"\\\\.]
            //   - \\\\ = literal backslash.
            //   - [bnrt'\"\\\\.] = one of the allowed escape chars:
            //       b, n, r, t, ', ", \, or .
            //   - So together this matches escapes like \n, \t, \", \\.
            //
            // (?: … )* = repeat zero or more times, non-capturing (groups the two options without saving).
            STRING = Pattern.compile("^\"(?:[^\"\\\\\\r\\n]|\\\\[bnrt'\"\\\\.])*\"$");
}