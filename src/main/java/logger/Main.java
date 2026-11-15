package logger;

import logger.writer.ConsoleWriter;
import logger.writer.FileWriter;
import logger.writer.Formatter.JsonFormatter;
import logger.writer.Formatter.StringFormatter;
import logger.writer.LogWriter;

import java.util.ArrayList;
import java.util.List;

public class Main {
    public static void main(String[] args){
        System.out.println("main");

        LogWriter fileWrtier = new FileWriter(LogLevel.ERROR, new JsonFormatter());
        LogWriter consoleWriter = new ConsoleWriter(LogLevel.DEBUG, new StringFormatter());

        List<LogWriter> writers = new ArrayList<>();
        writers.add(fileWrtier);
        writers.add(consoleWriter);


        Logger logger = new Logger(writers);

        logger.log(LogLevel.FATAL, "Service is crashing");
        logger.log(LogLevel.DEBUG, "current value is 5");

    }
}
