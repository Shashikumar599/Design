package logger;

import logger.writer.LogWriter;

import java.time.Instant;
import java.util.List;

public class Logger {
    private final List<LogWriter> writers;

    public Logger (List<LogWriter> writers){
        this.writers = List.copyOf(writers);
    }
    public void log(LogLevel logLevel, String message){
        Log log =  new Log(logLevel, message);
        publish(log);
    }
    private void publish(Log log){
        for(LogWriter writer : writers){
            try {
                writer.write(log);
            }
            catch(Exception e){
                System.out.println("Failed to write " + e.getMessage());
            }
        }
    }

}
