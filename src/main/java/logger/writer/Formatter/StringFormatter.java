package logger.writer.Formatter;

import logger.Log;

public class StringFormatter implements Formatter{
    @Override
    public String format(Log log) {
        return "[ " + log.getLevel() + " " + log.getTimestamp() + " " + log.getMessage() + " ]";
    }
}
