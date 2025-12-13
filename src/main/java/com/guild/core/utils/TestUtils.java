package com.guild.core.utils;

/**
 * Klasa narzędziowa do testowania
 */
public class TestUtils {

    /**
     * Test kompatybilności serwera
     */
    public static void testCompatibility(java.util.logging.Logger logger) {
        if (logger == null) {
            return;
        }
        logger.info("=== Test kompatybilności serwera ===");
        logger.info("Typ serwera: " + ServerUtils.getServerType());
        logger.info("Wersja serwera: " + ServerUtils.getServerVersion());
        logger.info("Czy obsługuje 1.21: " + ServerUtils.supportsApiVersion("1.21"));
        logger.info("Czy obsługuje 1.21.8: " + ServerUtils.supportsApiVersion("1.21.8"));
        logger.info("Czy to Folia: " + ServerUtils.isFolia());
        logger.info("Czy to Spigot: " + ServerUtils.isSpigot());
        logger.info("=========================");
    }

    /**
     * Test kompatybilności schedulera
     */
    public static void testSchedulerCompatibility(java.util.logging.Logger logger) {
        if (logger == null) {
            return;
        }
        logger.info("=== Test kompatybilności schedulera ===");
        logger.info("Czy w głównym wątku: " + CompatibleScheduler.isPrimaryThread());
        logger.info("Typ serwera: " + ServerUtils.getServerType());
        logger.info("=========================");
    }
}
