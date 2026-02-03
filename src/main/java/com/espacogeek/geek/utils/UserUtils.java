package com.espacogeek.geek.utils;

import java.util.regex.Pattern;
import org.springframework.security.core.Authentication;

public abstract class UserUtils {
    /**
     * Validate the password.
     *
     * @return <code>true</code> if the given password flow all rules and
     *         <code>false</code> if password doesn't flow any rule.
     */
    public static boolean isValidPassword(String password) {
        final String REG_EXPN_PASSWORD = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[!*@#$%^&+=])(?=\\S+$).{8,70}$";

        var pattern = Pattern.compile(REG_EXPN_PASSWORD, Pattern.CASE_INSENSITIVE);
        var matcher = pattern.matcher(password);

        return matcher.matches();
    }

    /**
     * Extracts the user ID from the given authentication object. The user ID is
     * assumed
     * to be stored in an authority with the prefix "ID_".
     *
     * @param authentication the authentication object
     * @return the user ID
     */
    public static Integer getUserID(Authentication authentication) {
        return Integer.valueOf(
                authentication.getAuthorities().stream().filter(
                        (authority) -> authority.getAuthority().startsWith("ID_"))
                        .toList()
                        .getFirst()
                        .getAuthority()
                        .replace("ID_", ""));
    }
}
