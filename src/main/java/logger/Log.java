package logger;

import java.time.Instant;

public class Log {
    private final Instant timestamp;
    private final String message;
    private final LogLevel level;

    public Log(LogLevel logLevel, String message){
        this.level = logLevel;
        this.message = message;
        this.timestamp = Instant.now();
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public LogLevel getLevel() {
        return level;
    }

    public String getMessage() {
        return message;
    }
}
