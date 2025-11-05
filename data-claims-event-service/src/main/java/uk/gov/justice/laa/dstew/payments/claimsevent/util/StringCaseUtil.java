package uk.gov.justice.laa.dstew.payments.claimsevent.util;

import org.springframework.stereotype.Component;

@Component
public final class StringCaseUtil {

    public static String camelToTitleCase(String input) {
        String[] words = input.split("(?=[A-Z])"); // Split before each uppercase letter
        return toTitleCase(words);
    }

    public static String snakeToTitleCase(String input) {
        String[] words = input.split("_");
        return toTitleCase(words);
    }

    private static String toTitleCase(String[] words) {
        StringBuilder result = new StringBuilder();

        for (String word : words) {
            if (!word.isEmpty()) {
                result.append(Character.toUpperCase(word.charAt(0)))
                        .append(word.substring(1).toLowerCase())
                        .append(" ");
            }
        }

        return result.toString().trim();
    }


}
