package plc.homework;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Contains JUnit tests for {@link Regex}. A framework of the test structure 
 * is provided, you will fill in the remaining pieces.
 *
 * To run tests, either click the run icon on the left margin, which can be used
 * to run all tests or only a specific test. You should make sure your tests are
 * run through IntelliJ (File > Settings > Build, Execution, Deployment > Build
 * Tools > Gradle > Run tests using <em>IntelliJ IDEA</em>). This ensures the
 * name and inputs for the tests are displayed correctly in the run window.
 */
public class RegexTests {

    /**
     * This is a parameterized test for the {@link Regex#EMAIL} regex. The
     * {@link ParameterizedTest} annotation defines this method as a
     * parameterized test, and {@link MethodSource} tells JUnit to look for the
     * static method {@link #testEmailRegex()}.
     *
     * For personal preference, I include a test name as the first parameter
     * which describes what that test should be testing - this is visible in
     * IntelliJ when running the tests (see above note if not working).
     */
    @ParameterizedTest
    @MethodSource
    public void testEmailRegex(String test, String input, boolean success) {
        test(input, Regex.EMAIL, success);
    }

    /**
     * This is the factory method providing test cases for the parameterized
     * test above - note that it is static, takes no arguments, and has the same
     * name as the test. The {@link Arguments} object contains the arguments for
     * each test to be passed to the function above.
     */
    public static Stream<Arguments> testEmailRegex() {
        return Stream.of(
                // Provided examples
                Arguments.of("Alphanumeric", "thelegend27@gmail.com", true),
                Arguments.of("UF Domain", "otherdomain@ufl.edu", true),
                Arguments.of("Missing Domain Dot", "missingdot@gmailcom", false),
                Arguments.of("Symbols", "symbols#$%@gmail.com", false),

                // My 5 matching
                Arguments.of("Local With Dot", "first.last@domain.com", true),
                Arguments.of("Local With Underscore", "first_last@domain.org", true),
                Arguments.of("Local With Hyphen", "first-last@domain.net", true),
                Arguments.of("Hyphen In Domain", "user@my-domain.com", true),
                Arguments.of("Numeric Domain Label", "user@123.ab", true),

                // My 5 non-matching
                Arguments.of("Plus In Local Not Allowed By Given Regex", "name+tag@domain.com", false),
                Arguments.of("Uppercase TLD", "user@site.EDU", false),
                Arguments.of("TLD Too Short", "user@site.c", false),
                Arguments.of("TLD Too Long", "user@site.info", false),
                Arguments.of("Subdomain Not Allowed", "user@mail.google.com", false)
        );
    }

    @ParameterizedTest
    @MethodSource
    public void testOddStringsRegex(String test, String input, boolean success) {
        test(input, Regex.ODD_STRINGS, success);
    }

    public static Stream<Arguments> testOddStringsRegex() {
        return Stream.of(
                // Provided
                // what have eleven letters and starts with gas?
                Arguments.of("11 Characters", "automobiles", true),
                Arguments.of("13 Characters", "i<3pancakes13", true),
                Arguments.of("5 Characters", "5five", false),
                Arguments.of("14 Characters", "i<3pancakes14!", false),

                // My 5 matching – odd lengths 11,13,15,17,19
                Arguments.of("11 Characters Letters", "abcdefghijk", true),
                Arguments.of("13 Characters Mixed", "abc123xyzABC!", true),
                Arguments.of("15 Characters Symbols", "!@#abcDEF123$%^", true),
                Arguments.of("17 Characters Letters", "abcdefghijklmnopq", true),
                Arguments.of("19 Characters Letters", "abcdefghijklmnopqrs", true),

                // My 5 non-matching – even or out of range
                Arguments.of("10 Characters Even", "abcdefghij", false),
                Arguments.of("16 Characters Even", "abcdefghijklmnop", false),
                Arguments.of("20 Characters Even", "abcdefghijklmnopqrst", false),
                Arguments.of("9 Characters Too Short", "abcdefghi", false),
                Arguments.of("21 Characters Too Long", "abcdefghijklmnopqrstu", false)
        );
    }

    @ParameterizedTest
    @MethodSource
    public void testIntegerListRegex(String test, String input, boolean success) {
        test(input, Regex.INTEGER_LIST, success);
    }

    public static Stream<Arguments> testIntegerListRegex() {
        return Stream.of(
                // Provided
                Arguments.of("Single Element", "[1]", true),
                Arguments.of("Multiple Elements", "[1,20,3]", true),
                Arguments.of("Missing Brackets", "1,2,3", false),
                Arguments.of("Missing Commas", "[1 2 3]", false),

                // My 5 matching
                Arguments.of("Empty List", "[]", true),
                Arguments.of("Space After Comma", "[1, 2, 3]", true),
                Arguments.of("Mixed Spacing Zero Or One Space", "[1,2, 3,4, 5]", true),
                Arguments.of("Two Elements", "[7,8]", true),
                Arguments.of("Larger Integers", "[123,4567,890123]", true),

                // My 5 non-matching
                Arguments.of("Zero Not Positive", "[0]", false),
                Arguments.of("Leading Zero Not Allowed", "[01,2]", false),
                Arguments.of("All Leading Zeros Not Allowed", "[007,008,009]", false),
                Arguments.of("Two Spaces After Comma Not Allowed", "[1,  2]", false),
                Arguments.of("Trailing Comma Not Allowed", "[1,2,3,]", false)
        );
    }

    @ParameterizedTest
    @MethodSource
    public void testDecimalRegex(String test, String input, boolean success) {
        test(input, Regex.DECIMAL, success);
    }

    public static Stream<Arguments> testDecimalRegex() {
        return Stream.of(
                // My 5 matching
                Arguments.of("0 POINT 5", "0.5", true),
                Arguments.of("1 POINT 0", "1.0", true),
                Arguments.of("Negative 0 POINT 123", "-0.123", true),
                Arguments.of("10 POINT 000", "10.000", true),
                Arguments.of("Large Integer Part With One Decimal", "123456.7", true),

                // My 5 non-matching
                Arguments.of("Integer Only", "1", false),
                Arguments.of("Missing Integer Part", ".5", false),
                Arguments.of("Missing Fraction Part", "1.", false),
                Arguments.of("Leading Zero On Nonzero Integer Part", "01.2", false),
                Arguments.of("Plus Sign Not Allowed", "+1.0", false)
        );
    }

    @ParameterizedTest
    @MethodSource
    public void testStringRegex(String test, String input, boolean success) {
        test(input, Regex.STRING, success);
    }

    public static Stream<Arguments> testStringRegex() {
        return Stream.of(
                // My 5 matching
                Arguments.of("Empty String Literal", "\"\"", true),
                Arguments.of("Simple Hello World", "\"Hello, World!\"", true),
                Arguments.of("Escaped Tab", "\"1\\t2\"", true),
                Arguments.of("Escaped Quote Inside", "\"He said \\\"hi\\\".\"", true),
                Arguments.of("Escaped Backslash Path", "\"path\\\\to\\\\file\"", true),

                // My 5 non-matching
                Arguments.of("Unterminated Missing Quote", "\"unterminated", false),
                Arguments.of("Invalid Escape Backslash q", "\"bad\\q\"", false),
                Arguments.of("Unescaped Double Quote Inside", "\"He said \"hi\"\"", false),
                Arguments.of("Raw Newline Not Allowed", "\"line1\nline2\"", false),
                Arguments.of("Invalid Unicode Escape Not In Allowed Set", "\"\\u0041\"", false)
        );
    }

    /**
     * Asserts that the input matches the given pattern. This method doesn't do
     * much now, but you will see this concept in future assignments.
     */
    private static void test(String input, Pattern pattern, boolean success) {
        Assertions.assertEquals(success, pattern.matcher(input).matches());
    }

}
