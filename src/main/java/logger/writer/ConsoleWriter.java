package logger.writer;

import logger.LogLevel;
import logger.writer.Formatter.Formatter;

public class ConsoleWriter extends LogWriter {

    public ConsoleWriter(LogLevel level, Formatter formatter) {
        super(level, formatter);
    }

    @Override
    protected void writeop(String input) {

        System.out.println("CONSOLE : " + input );

    }
}
