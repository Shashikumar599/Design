package logger.writer;

import logger.LogLevel;
import logger.writer.Formatter.Formatter;

public class FileWriter extends LogWriter {
    public FileWriter(LogLevel level, Formatter formatter) {
        super(level, formatter);
    }

    @Override
    protected void writeop(String input) {

        System.out.println("FILE : " + input);

    }
}
