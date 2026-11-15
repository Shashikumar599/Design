package logger.writer.Formatter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import logger.Log;

public class JsonFormatter implements Formatter{
    private static final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    @Override
    public String format(Log log)  {
        try {
            return objectMapper.writeValueAsString(log);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
