package lab;


/**
 * Assertions
 */
public class TestingHelper {
    public static void assertEqual(String left, String right) {
        System.out.printf("CLAIM: [%s] == [%s]\n", left, right);
        assert left.equals(right);
    }

}
