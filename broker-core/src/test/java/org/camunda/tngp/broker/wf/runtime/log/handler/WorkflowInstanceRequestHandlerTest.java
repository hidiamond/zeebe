package org.camunda.tngp.broker.wf.runtime.log.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.camunda.tngp.bpmn.graph.ProcessGraph;
import org.camunda.tngp.broker.util.mocks.StubLogWriter;
import org.camunda.tngp.broker.util.mocks.StubResponseControl;
import org.camunda.tngp.broker.wf.WfErrors;
import org.camunda.tngp.broker.wf.repository.WfDefinitionCache;
import org.camunda.tngp.broker.wf.runtime.log.WorkflowInstanceRequestReader;
import org.camunda.tngp.broker.wf.runtime.log.bpmn.BpmnFlowElementEventReader;
import org.camunda.tngp.graph.bpmn.ExecutionEventType;
import org.camunda.tngp.log.idgenerator.IdGenerator;
import org.camunda.tngp.log.idgenerator.impl.PrivateIdGenerator;
import org.camunda.tngp.protocol.error.ErrorReader;
import org.camunda.tngp.protocol.wf.StartWorkflowInstanceResponseReader;
import org.camunda.tngp.protocol.wf.runtime.StartWorkflowInstanceDecoder;
import org.camunda.tngp.taskqueue.data.ProcessInstanceRequestType;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import uk.co.real_logic.agrona.concurrent.UnsafeBuffer;

public class WorkflowInstanceRequestHandlerTest
{

    @Mock
    protected ProcessGraph process;

    @Mock
    protected WfDefinitionCache wfDefinitionCache;

    protected StubLogWriter logWriter;
    protected StubResponseControl responseControl;

    protected IdGenerator idGenerator;

    @Before
    public void setUp()
    {
        MockitoAnnotations.initMocks(this);
        logWriter = new StubLogWriter();
        responseControl = new StubResponseControl();
        idGenerator = new PrivateIdGenerator(10L);

        when(wfDefinitionCache.getProcessGraphByTypeId(1234L)).thenReturn(process);

        when(process.intialFlowNodeId()).thenReturn(987);
        when(process.id()).thenReturn(1234L);
    }

    @Test
    public void shouldStartProcessById()
    {
        // given
        final WorkflowInstanceRequestHandler handler = new WorkflowInstanceRequestHandler(wfDefinitionCache, logWriter, idGenerator);

        final WorkflowInstanceRequestReader requestReader = mock(WorkflowInstanceRequestReader.class);
        when(requestReader.type()).thenReturn(ProcessInstanceRequestType.NEW);
        when(requestReader.wfDefinitionId()).thenReturn(1234L);

        // when
        handler.handle(requestReader, responseControl);

        // then
        assertThat(responseControl.size()).isEqualTo(1);
        assertThat(responseControl.isAcceptance(0));

        final StartWorkflowInstanceResponseReader responseReader = responseControl.getAcceptanceValueAs(0, StartWorkflowInstanceResponseReader.class);
        assertThat(responseReader.wfInstanceId()).isEqualTo(11L);

        assertThat(logWriter.size()).isEqualTo(1);
        final BpmnFlowElementEventReader flowElementEventReader = logWriter.getEntryAs(0, BpmnFlowElementEventReader.class);

        assertThat(flowElementEventReader.event()).isEqualTo(ExecutionEventType.EVT_OCCURRED);
        assertThat(flowElementEventReader.flowElementId()).isEqualTo(987);
        assertThat(flowElementEventReader.wfDefinitionId()).isEqualTo(1234L);
        assertThat(flowElementEventReader.wfInstanceId()).isEqualTo(11L);
    }

    @Test
    public void shouldStartProcessByKey()
    {
        // given
        final WorkflowInstanceRequestHandler handler = new WorkflowInstanceRequestHandler(wfDefinitionCache, logWriter, idGenerator);

        final byte[] key = new byte[]{ 1, 2, 3 };

        final WorkflowInstanceRequestReader requestReader = mock(WorkflowInstanceRequestReader.class);
        when(requestReader.type()).thenReturn(ProcessInstanceRequestType.NEW);
        when(requestReader.wfDefinitionId()).thenReturn(StartWorkflowInstanceDecoder.wfDefinitionIdNullValue());
        when(requestReader.wfDefinitionKey()).thenReturn(new UnsafeBuffer(key));

        when(wfDefinitionCache.getProcessGraphByTypeId(anyLong())).thenReturn(null);
        when(wfDefinitionCache.getLatestProcessGraphByTypeKey(any(), anyInt(), anyInt())).thenReturn(process); // TODO: make stronger key parameter assumption

        // when
        handler.handle(requestReader, responseControl);

        // then
        assertThat(responseControl.size()).isEqualTo(1);
        assertThat(responseControl.isAcceptance(0));

        final StartWorkflowInstanceResponseReader responseReader = responseControl.getAcceptanceValueAs(0, StartWorkflowInstanceResponseReader.class);
        assertThat(responseReader.wfInstanceId()).isEqualTo(11L);

        assertThat(logWriter.size()).isEqualTo(1);
        final BpmnFlowElementEventReader flowElementEventReader = logWriter.getEntryAs(0, BpmnFlowElementEventReader.class);

        assertThat(flowElementEventReader.event()).isEqualTo(ExecutionEventType.EVT_OCCURRED);
        assertThat(flowElementEventReader.flowElementId()).isEqualTo(987);
        assertThat(flowElementEventReader.wfDefinitionId()).isEqualTo(1234L);
        assertThat(flowElementEventReader.wfInstanceId()).isEqualTo(11L);
    }

    @Test
    public void shouldWriteErrorWhenProcessNotFoundById()
    {
        // given
        final WorkflowInstanceRequestHandler handler = new WorkflowInstanceRequestHandler(wfDefinitionCache, logWriter, idGenerator);

        final WorkflowInstanceRequestReader requestReader = mock(WorkflowInstanceRequestReader.class);
        when(requestReader.type()).thenReturn(ProcessInstanceRequestType.NEW);
        when(requestReader.wfDefinitionId()).thenReturn(1234L);

        when(wfDefinitionCache.getProcessGraphByTypeId(1234L)).thenReturn(null);

        // when
        handler.handle(requestReader, responseControl);

        // then
        assertThat(logWriter.size()).isEqualTo(0);
        assertThat(responseControl.size()).isEqualTo(1);
        assertThat(responseControl.isRejection(0)).isTrue();

        final ErrorReader errorReader = responseControl.getRejectionValueAs(0, ErrorReader.class);

        assertThat(errorReader.componentCode()).isEqualTo(WfErrors.COMPONENT_CODE);
        assertThat(errorReader.detailCode()).isEqualTo(WfErrors.PROCESS_NOT_FOUND_ERROR);
        assertThat(errorReader.errorMessage()).isEqualTo("Cannot find process with id");
    }

    @Test
    public void shouldWriteErrorWhenProcessNotFoundByKey()
    {
        // given
        final WorkflowInstanceRequestHandler handler = new WorkflowInstanceRequestHandler(wfDefinitionCache, logWriter, idGenerator);

        final byte[] key = new byte[]{ 1, 2, 3 };

        final WorkflowInstanceRequestReader requestReader = mock(WorkflowInstanceRequestReader.class);
        when(requestReader.type()).thenReturn(ProcessInstanceRequestType.NEW);
        when(requestReader.wfDefinitionId()).thenReturn(StartWorkflowInstanceDecoder.wfDefinitionIdNullValue());
        when(requestReader.wfDefinitionKey()).thenReturn(new UnsafeBuffer(key));

        when(wfDefinitionCache.getProcessGraphByTypeId(anyLong())).thenReturn(null);
        when(wfDefinitionCache.getLatestProcessGraphByTypeKey(any(), anyInt(), anyInt())).thenReturn(null);

        // when
        handler.handle(requestReader, responseControl);

        // then
        assertThat(logWriter.size()).isEqualTo(0);
        assertThat(responseControl.size()).isEqualTo(1);
        assertThat(responseControl.isRejection(0)).isTrue();

        final ErrorReader errorReader = responseControl.getRejectionValueAs(0, ErrorReader.class);

        assertThat(errorReader.componentCode()).isEqualTo(WfErrors.COMPONENT_CODE);
        assertThat(errorReader.detailCode()).isEqualTo(WfErrors.PROCESS_NOT_FOUND_ERROR);
        assertThat(errorReader.errorMessage()).isEqualTo("Cannot find process with key");
    }
}