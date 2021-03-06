/*
 * Copyright © 2017 camunda services GmbH (info@camunda.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.zeebe.client.workflow.cmd;

import java.io.InputStream;

import io.zeebe.client.cmd.Request;
import io.zeebe.client.event.WorkflowInstanceEvent;

/**
 * Command to update the payload of a workflow instance.
 */
public interface UpdatePayloadCommand extends Request<WorkflowInstanceEvent>
{

    /**
     * Set the new payload as JSON stream. Note that the given payload replace
     * the current workflow instance payload.
     */
    UpdatePayloadCommand payload(InputStream payload);

    /**
     * Set the new payload as JSON string. Note that the given payload replace
     * the current workflow instance payload.
     */
    UpdatePayloadCommand payload(String payload);

}
