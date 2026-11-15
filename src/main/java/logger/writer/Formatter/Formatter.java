package logger.writer.Formatter;

import com.fasterxml.jackson.core.JsonProcessingException;
import logger.Log;

public interface Formatter {
    String format(Log log);
}
