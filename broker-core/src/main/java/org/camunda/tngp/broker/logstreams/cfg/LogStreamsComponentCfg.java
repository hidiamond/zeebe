package org.camunda.tngp.broker.logstreams.cfg;

public class LogStreamsComponentCfg
{
    public String[] logDirectories = new String[0];

    public int logWriteBufferSize = 16;

    public int defaultLogSegmentSize = 512;

}