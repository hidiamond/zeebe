package org.camunda.tngp.broker.log;

import org.camunda.tngp.log.LogReader;
import org.camunda.tngp.util.buffer.BufferReader;

public class LogEntryProcessor<T extends BufferReader>
{
    protected LogReader logReader;
    protected T bufferReader;
    protected LogEntryHandler<T> entryHandler;

    public LogEntryProcessor(LogReader logReader, T bufferReader, LogEntryHandler<T> entryHandler)
    {
        this.bufferReader = bufferReader;
        this.logReader = logReader;
        this.entryHandler = entryHandler;
    }

    public int doWorkSingle()
    {
        return doWork(1);
    }

    public int doWork(final int cycles)
    {
        int workCount = 0;

        boolean hasProcessedEntry;

        do
        {
            final long position = logReader.position();
            hasProcessedEntry = logReader.read(bufferReader);
            if (hasProcessedEntry)
            {
                final int handlerResult = entryHandler.handle(position, bufferReader);
                if (handlerResult == LogEntryHandler.CONSUME_ENTRY_RESULT)
                {
                    workCount++;
                }
                else
                {
                    hasProcessedEntry = false;
                    logReader.setPosition(position); // reset position to handle log entry a second time
                }
            }
        } while (hasProcessedEntry && workCount < cycles);

        return workCount;
    }

    public void setLogReader(LogReader logReader)
    {
        this.logReader = logReader;
    }
}