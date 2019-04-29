/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.cognitree.kronos;

import com.cognitree.kronos.scheduler.NamespaceService;
import com.cognitree.kronos.scheduler.WorkflowService;
import com.cognitree.kronos.scheduler.WorkflowTriggerService;
import com.cognitree.kronos.scheduler.model.events.ConfigUpdate;
import com.cognitree.kronos.scheduler.model.Namespace;
import com.cognitree.kronos.scheduler.model.SimpleSchedule;
import com.cognitree.kronos.scheduler.model.Workflow;
import com.cognitree.kronos.scheduler.model.WorkflowTrigger;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.junit.Assert;
import org.quartz.Scheduler;
import org.quartz.TriggerKey;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.UUID;

public class TestUtil {

    private static final ObjectMapper YAML_MAPPER = new ObjectMapper(new YAMLFactory());

    private static final SimpleSchedule SCHEDULE = new SimpleSchedule();

    public static ConfigUpdate createConfigUpdate(ConfigUpdate.Action action, Object model) {
        ConfigUpdate configUpdate = new ConfigUpdate();
        configUpdate.setAction(action);
        configUpdate.setModel(model);
        return configUpdate;
    }

    public static Namespace createNamespace(String name) {
        Namespace namespace = new Namespace();
        namespace.setName(name);
        return namespace;
    }

    public static Workflow createWorkflow(String workflowTemplate, String workflowName,
                                          String namespace) throws IOException {
        return createWorkflow(workflowTemplate, workflowName, namespace, null);
    }

    public static Workflow createWorkflow(String workflowTemplate, String workflowName,
                                          String namespace, Map<String, Object> properties) throws IOException {
        final InputStream resourceAsStream =
                TestUtil.class.getClassLoader().getResourceAsStream(workflowTemplate);
        final Workflow workflow = YAML_MAPPER.readValue(resourceAsStream, Workflow.class);
        workflow.setName(workflowName);
        workflow.setNamespace(namespace);
        workflow.setProperties(properties);
        return workflow;
    }

    public static WorkflowTrigger createWorkflowTrigger(String triggerName, String workflow, String namespace) {
        return createWorkflowTrigger(triggerName, workflow, namespace, null);
    }

    public static WorkflowTrigger createWorkflowTrigger(String triggerName, String workflow, String namespace,
                                                        Map<String, Object> properties) {
        final WorkflowTrigger workflowTrigger = new WorkflowTrigger();
        workflowTrigger.setName(triggerName);
        workflowTrigger.setWorkflow(workflow);
        workflowTrigger.setNamespace(namespace);
        workflowTrigger.setSchedule(SCHEDULE);
        workflowTrigger.setProperties(properties);
        return workflowTrigger;
    }

    public static WorkflowTrigger scheduleWorkflow(String workflowTemplate) throws Exception {
        return scheduleWorkflow(workflowTemplate, null, null);
    }

    public static WorkflowTrigger scheduleWorkflow(String workflowTemplate, Map<String, Object> workflowProps,
                                                   Map<String, Object> triggerProps) throws Exception {
        Namespace namespace = createNamespace(UUID.randomUUID().toString());
        NamespaceService.getService().add(namespace);
        final Workflow workflow = createWorkflow(workflowTemplate,
                UUID.randomUUID().toString(), namespace.getName(), workflowProps);
        WorkflowService.getService().add(workflow);
        final WorkflowTrigger workflowTrigger = createWorkflowTrigger(UUID.randomUUID().toString(),
                workflow.getName(), workflow.getNamespace(), triggerProps);
        WorkflowTriggerService.getService().add(workflowTrigger);
        return workflowTrigger;
    }

    public static void waitForTriggerToComplete(WorkflowTrigger workflowTriggerOne, Scheduler scheduler) throws Exception {
        // wait for both the job to be triggered
        TriggerKey workflowOneTriggerKey = new TriggerKey(workflowTriggerOne.getName(),
                workflowTriggerOne.getWorkflow() + ":" + workflowTriggerOne.getNamespace());
        int maxCount = 50;
        while (maxCount > 0 && scheduler.checkExists(workflowOneTriggerKey)) {
            Thread.sleep(100);
            maxCount--;
        }
        if (maxCount < 0) {
            Assert.fail("failed while waiting for trigger to complete");
        }
    }
}
