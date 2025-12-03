package com.RutaDelSabor.ruta.utils;

import java.util.Arrays;

public class LevenshteinUtil {

    // Calcula la distancia entre dos textos (0 = idénticos)
    public static int calculateDistance(String x, String y) {
        if (x == null && y == null) return 0;
        if (x == null) return y.length();
        if (y == null) return x.length();

        int[][] dp = new int[x.length() + 1][y.length() + 1];

        for (int i = 0; i <= x.length(); i++) {
            for (int j = 0; j <= y.length(); j++) {
                if (i == 0) {
                    dp[i][j] = j;
                } else if (j == 0) {
                    dp[i][j] = i;
                } else {
                    dp[i][j] = min(dp[i - 1][j - 1] 
                                   + costOfSubstitution(x.charAt(i - 1), y.charAt(j - 1)), 
                                   dp[i - 1][j] + 1, 
                                   dp[i][j - 1] + 1);
                }
            }
        }
        return dp[x.length()][y.length()];
    }

    // Calcula el porcentaje de similitud (0.0 a 1.0)
    public static double calculateSimilarity(String x, String y) {
        if (x == null || y == null) return 0.0;
        String s1 = x.toLowerCase();
        String s2 = y.toLowerCase();
        int maxLength = Math.max(s1.length(), s2.length());
        if (maxLength == 0) return 1.0;
        return 1.0 - (double) calculateDistance(s1, s2) / maxLength;
    }

    private static int costOfSubstitution(char a, char b) {
        return a == b ? 0 : 1;
    }

    private static int min(int... numbers) {
        return Arrays.stream(numbers).min().orElse(Integer.MAX_VALUE);
    }
}