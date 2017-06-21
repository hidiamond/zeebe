package org.camunda.tngp.broker.logstreams;

import static org.camunda.tngp.util.EnsureUtil.ensureGreaterThanOrEqual;
import static org.camunda.tngp.util.EnsureUtil.ensureLessThanOrEqual;
import static org.camunda.tngp.util.EnsureUtil.ensureNotNullOrEmpty;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.function.Consumer;

import org.agrona.DirectBuffer;
import org.agrona.collections.Int2ObjectHashMap;
import org.camunda.tngp.broker.logstreams.cfg.LogStreamsCfg;
import org.camunda.tngp.logstreams.LogStreams;
import org.camunda.tngp.logstreams.fs.FsLogStreamBuilder;
import org.camunda.tngp.logstreams.log.LogStream;
import org.camunda.tngp.util.actor.ActorScheduler;


public class LogStreamsManager
{
    protected LogStreamsCfg logStreamsCfg;
    protected ActorScheduler actorScheduler;
    protected Map<DirectBuffer, Int2ObjectHashMap<LogStream>> logStreams;

    public LogStreamsManager(final LogStreamsCfg logStreamsCfg, final ActorScheduler actorScheduler)
    {
        this.logStreamsCfg = logStreamsCfg;
        this.actorScheduler = actorScheduler;
        this.logStreams = new HashMap<>();
    }

    public void forEachLogStream(Consumer<LogStream> consumer)
    {
        // TODO(menski): probably not garbage free
        logStreams.forEach((topicName, partitions) ->
            partitions.forEach((partitionId, logStream) ->
                consumer.accept(logStream))
        );
    }


    public LogStream getLogStream(final DirectBuffer topicName, final int partitionId)
    {
        final Int2ObjectHashMap<LogStream> logStreamPartitions = logStreams.get(topicName);

        if (logStreamPartitions != null)
        {
            return logStreamPartitions.get(partitionId);
        }

        return null;
    }

    public LogStream createLogStream(final DirectBuffer topicName, final int partitionId)
    {
        ensureNotNullOrEmpty("topic name", topicName);
        ensureGreaterThanOrEqual("partition id", partitionId, 0);
        ensureLessThanOrEqual("partition id", partitionId, Short.MAX_VALUE);

        final FsLogStreamBuilder logStreamBuilder = LogStreams.createFsLogStream(topicName, partitionId);
        final String logName = logStreamBuilder.getLogName();

        final String logDirectory;
        final boolean deleteOnExit = false;

        int assignedLogDirectory = 0;
        if (logStreamsCfg.directories.length == 0)
        {
            throw new RuntimeException(String.format("Cannot start log %s, no log directory provided.", logName));
        }
        else if (logStreamsCfg.directories.length > 1)
        {
            assignedLogDirectory = new Random().nextInt(logStreamsCfg.directories.length - 1);
        }
        logDirectory = logStreamsCfg.directories[assignedLogDirectory] + File.separator + logName;


        final int logSegmentSize = logStreamsCfg.defaultLogSegmentSize * 1024 * 1024;

        final LogStream logStream = logStreamBuilder
            .deleteOnClose(deleteOnExit)
            .logDirectory(logDirectory)
            .actorScheduler(actorScheduler)
            .logSegmentSize(logSegmentSize)
            .logStreamControllerDisabled(true)
            .build();

        addLogStream(logStream);

        logStream.open();

        return logStream;
    }

    public LogStream createLogStream(final DirectBuffer topicName, final int partitionId, final String logDirectory)
    {
        final LogStream logStream = LogStreams.createFsLogStream(topicName, partitionId)
                .deleteOnClose(false)
                .logDirectory(logDirectory)
                .actorScheduler(actorScheduler)
                .logSegmentSize(logStreamsCfg.defaultLogSegmentSize * 1024 * 1024)
                .logStreamControllerDisabled(true)
                .build();

        addLogStream(logStream);

        logStream.open();

        return logStream;
    }

    private void addLogStream(final LogStream logStream)
    {
        logStreams
            .computeIfAbsent(logStream.getTopicName(), k -> new Int2ObjectHashMap<>())
            .put(logStream.getPartitionId(), logStream);
    }
}
