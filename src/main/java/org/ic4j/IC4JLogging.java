package org.ic4j;

public class IC4JLogging {
    private static final String SLF4J_LOGGER_FACTORY_CLASS = "org.slf4j.LoggerFactory";
    private static final String LOGGER_INTERFACE_CLASS = "org.slf4j.Logger";

    private final Object logger;

    private IC4JLogging(Object logger) {
        this.logger = logger;
    }

    public static IC4JLogging getIC4JLogger(Class<?> clazz) {
        Object logger = null;
        // Check if SLF4J is present in the classpath
        if (isSlf4jPresent()) {
            // If present, instantiate the SLF4J Logger using reflection
            try {
                Class<?> loggerFactoryClass = Class.forName(SLF4J_LOGGER_FACTORY_CLASS);
                Class<?> loggerInterface = Class.forName(LOGGER_INTERFACE_CLASS);
                logger = loggerFactoryClass.getDeclaredMethod("getLogger", Class.class).invoke(null, clazz);
            } catch (Exception e) {
                throw new RuntimeException("Failed to initialize SLF4J logger", e);
            }
        }
        return new IC4JLogging(logger);
    }

    private static boolean isSlf4jPresent() {
        try {
            Class.forName(SLF4J_LOGGER_FACTORY_CLASS);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    public void log(String message) {
        if (logger != null) {
            // Use reflection to call the logger method
            try {
                Class<?> loggerClass = logger.getClass();
                loggerClass.getMethod("info", String.class).invoke(logger, message);
            } catch (Exception e) {
                throw new RuntimeException("Logging failed", e);
            }
        }
    }
}
