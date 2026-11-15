package logger.writer;

import com.fasterxml.jackson.core.JsonProcessingException;
import logger.Log;
import logger.LogLevel;
import logger.writer.Formatter.Formatter;

import java.util.concurrent.locks.ReentrantLock;

public abstract class LogWriter {

    protected final LogLevel level;
    protected final Formatter formatter;
    protected final ReentrantLock lock;

    public LogWriter(LogLevel level, Formatter formatter){
        this.level = level;
        this.formatter = formatter;
        lock = new ReentrantLock();
    }

    public final void write(Log log)   {
        if(log.getLevel().compareTo(level) < 0){
            return;
        }
        String input = formatter.format(log);
        lock.lock();
        try{
        writeop(input);
        }
        catch (Exception e){
            throw e;
        }
        finally {
            lock.unlock();
        }


    }
    protected abstract void writeop(String input);

}
