package com.hydrides.processor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.avro.generic.GenericData.Record;
import org.kie.api.event.process.ProcessCompletedEvent;
import org.kie.api.event.process.ProcessEventListener;
import org.kie.api.event.process.ProcessNodeLeftEvent;
import org.kie.api.event.process.ProcessNodeTriggeredEvent;
import org.kie.api.event.process.ProcessStartedEvent;
import org.kie.api.event.process.ProcessVariableChangedEvent;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.manager.RuntimeEngine;
import org.kie.api.runtime.manager.RuntimeManager;
import org.kie.api.runtime.process.NodeInstance;
import org.kie.api.runtime.process.ProcessInstance;
import org.kie.api.task.TaskEvent;
import org.kie.api.task.TaskLifeCycleEventListener;
import org.kie.api.task.TaskService;
import org.kie.api.task.model.Status;
import org.kie.internal.runtime.manager.context.ProcessInstanceIdContext;
import org.kie.internal.task.api.EventService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hydrides.core.Processor;
import com.hydrides.engine.JBPMEngine;

public abstract class AbstractJBPMProcessor extends Processor
		implements ProcessEventListener, TaskLifeCycleEventListener {
	Logger log = LoggerFactory.getLogger(getClass());

	private JBPMEngine engine = null;
	private Map<Integer, String> stateMap = new HashMap<Integer, String>();
	private String processName = null;
	private String opration = null;

	public AbstractJBPMProcessor(String processName, String opration) {
		this.processName = processName;
		this.opration = opration;
	}

	public String getProcessName() {
		return processName;
	}

	public String getOpration() {
		return opration;
	}

	public void setEngine(JBPMEngine engine) {
		this.engine = engine;
	}

	protected String getState(int state) {
		return stateMap.get(state);
	}

	private String jbpmProcessName = null;

	protected Map<String, String> nodeActionMap;

	public String getJbpmProcessName() {
		return jbpmProcessName;
	}

	public void setJbpmProcessName(String jbpmProcessName) {
		this.jbpmProcessName = jbpmProcessName;
	}

	private List<String> logs = new ArrayList<String>();

	private KieSession ksession = null;
	private TaskService taskService = null;
	private Record data = null;

	protected KieSession getKsession() {
		return ksession;
	}

	protected TaskService getTaskService() {
		return taskService;
	}

	public void init(List<Record> rec) {
		this.data = rec.get(0);
		RuntimeEngine engine = null;
		Long instanceid = 0L;
		Object pid = data.get("process_instance_id");

		if (data.get("process_instance_id")!=null) {
			engine = manager.getRuntimeEngine(
					ProcessInstanceIdContext.get(new Long(data.get("process_instance_id").toString())));
		} else {
			engine = manager.getRuntimeEngine(ProcessInstanceIdContext.get());
		}

		ksession = engine.getKieSession();
		taskService = engine.getTaskService();
		((EventService<TaskLifeCycleEventListener>) taskService).registerTaskEventListener(this);
		ksession.addEventListener(this);
		// ksession.getWorkItemManager().registerWorkItemHandler("Human Task",
		// new CustomTask(taskService, (Action) getModel(), data));
		// ksession.getWorkItemManager().registerWorkItemHandler("Service Task",
		// new CustomTask(taskService, (Action) getModel(), data));

	}

	@Override
	public void beforeProcessStarted(ProcessStartedEvent event) {
		nodeEvent(event.getProcessInstance(), "beforeProcessStarted");

	}

	@Override
	public void afterProcessStarted(ProcessStartedEvent event) {
		nodeEvent(event.getProcessInstance(), "afterProcessStarted");
	}

	@Override
	public void beforeProcessCompleted(ProcessCompletedEvent event) {
		nodeEvent(event.getProcessInstance(), "beforeProcessCompleted");

	}

	@Override
	public void afterProcessCompleted(ProcessCompletedEvent event) {
		nodeEvent(event.getProcessInstance(), "afterProcessCompleted");

	}

	@Override
	public void afterNodeTriggered(ProcessNodeTriggeredEvent event) {

		nodeEvent(event.getNodeInstance(), "afterNodeTriggered");
	}

	private void nodeEvent(ProcessInstance instance, String to) {
		log.info("-------->" + to);

		data.put("process_instance_id", instance.getId() + "");
		data.put("process_name", instance.getProcessName() + "");
		data.put("process_status", stateMap.get(instance.getState()));

		trigger(processName + "\\" + to, data);

	}

	private void nodeEvent(NodeInstance instance, String to) {
		log.info("-------->" + to);

		data.put("process_instance_id", instance.getProcessInstance().getId() + "");
		data.put("task_id", instance.getId() + "");
		data.put("task_name", instance.getNodeName() + "");
		data.put("task_status", stateMap.get(0));
		trigger(processName + "\\" + to, data);

	}

	private void trigger(String string, Record data2) {
		try {
			engine.stream(string, data2);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

	@Override
	public void beforeNodeTriggered(ProcessNodeTriggeredEvent event) {
		// TODO Auto-generated method stub

	}

	@Override
	public void beforeNodeLeft(ProcessNodeLeftEvent event) {
		// TODO Auto-generated method stub

	}

	@Override
	public void afterNodeLeft(ProcessNodeLeftEvent event) {
		// TODO Auto-generated method stub

	}

	@Override
	public void beforeVariableChanged(ProcessVariableChangedEvent event) {
		// TODO Auto-generated method stub

	}

	@Override
	public void afterVariableChanged(ProcessVariableChangedEvent event) {
		// TODO Auto-generated method stub

	}

	private RuntimeManager manager = null;

	public void setRuntimeManager(RuntimeManager manager) {
		this.manager = manager;

	}

	protected Record getTask(Long long1, Status status) {
		org.kie.api.task.model.Task task = taskService.getTaskById(long1);

		data.put("name", task.getName());
		data.put("task_id", task.getId());
		data.put("description", task.getDescription());
		data.put("priority", task.getPriority());
		data.put("subject", task.getSubject());
		data.put("task_type", task.getTaskType());
		if (task.getTaskData().getActualOwner() != null)
			data.put("owner", task.getTaskData().getActualOwner().getId());
		else
			data.put("owner", "");

		data.put("created_by", task.getTaskData().getCreatedBy().getId());
		data.put("created_on", task.getTaskData().getCreatedOn().getTime());
		if (task.getTaskData().getExpirationTime() != null)
			data.put("expiration_time", task.getTaskData().getExpirationTime().getTime());
		else
			data.put("expiration_time", "");
		data.put("task_status", status.name());
		data.put("workitem_id", task.getTaskData().getWorkItemId());
		data.put("session_id", task.getTaskData().getProcessSessionId());
		data.put("process_instance_id", task.getTaskData().getProcessInstanceId());
		data.put("process_id", task.getTaskData().getProcessId());
		log.info("--------->" + data);

		return data;

	}

	@Override
	public void beforeTaskActivatedEvent(TaskEvent event) {
		// TODO Auto-generated method stub

	}

	@Override
	public void beforeTaskClaimedEvent(TaskEvent event) {
		// TODO Auto-generated method stub

	}

	@Override
	public void beforeTaskSkippedEvent(TaskEvent event) {
		// TODO Auto-generated method stub

	}

	@Override
	public void beforeTaskStartedEvent(TaskEvent event) {
		// TODO Auto-generated method stub

	}

	@Override
	public void beforeTaskStoppedEvent(TaskEvent event) {
		// TODO Auto-generated method stub

	}

	@Override
	public void beforeTaskCompletedEvent(TaskEvent event) {
		// TODO Auto-generated method stub

	}

	@Override
	public void beforeTaskFailedEvent(TaskEvent event) {
		// TODO Auto-generated method stub

	}

	@Override
	public void beforeTaskAddedEvent(TaskEvent event) {
		// TODO Auto-generated method stub

	}

	@Override
	public void beforeTaskExitedEvent(TaskEvent event) {
		// TODO Auto-generated method stub

	}

	@Override
	public void beforeTaskReleasedEvent(TaskEvent event) {
		// TODO Auto-generated method stub

	}

	@Override
	public void beforeTaskResumedEvent(TaskEvent event) {
		// TODO Auto-generated method stub

	}

	@Override
	public void beforeTaskSuspendedEvent(TaskEvent event) {
		// TODO Auto-generated method stub

	}

	@Override
	public void beforeTaskForwardedEvent(TaskEvent event) {
		// TODO Auto-generated method stub

	}

	@Override
	public void beforeTaskDelegatedEvent(TaskEvent event) {
		taskEvent(event, "beforeTaskDelegatedEvent");

	}

	@Override
	public void beforeTaskNominatedEvent(TaskEvent event) {
		taskEvent(event, "beforeTaskNominatedEvent");

	}

	@Override
	public void afterTaskActivatedEvent(TaskEvent event) {
		taskEvent(event, "afterTaskActivatedEvent");

	}

	@Override
	public void afterTaskClaimedEvent(TaskEvent event) {
		taskEvent(event, "afterTaskClaimedEvent");
	}

	@Override
	public void afterTaskSkippedEvent(TaskEvent event) {
		taskEvent(event, "afterTaskSkippedEvent");

	}

	@Override
	public void afterTaskStartedEvent(TaskEvent event) {
		taskEvent(event, "afterTaskStartedEvent");

	}

	@Override
	public void afterTaskStoppedEvent(TaskEvent event) {
		taskEvent(event, "afterTaskStoppedEvent");

	}

	@Override
	public void afterTaskCompletedEvent(TaskEvent event) {
		taskEvent(event, "afterTaskCompletedEvent");
	}

	// String t = null;

	private void taskEvent(TaskEvent event, String to) {

		data.put("process_instance_id", event.getTask().getTaskData().getProcessInstanceId() + "");
		data.put("work_item_id", event.getTask().getTaskData().getWorkItemId() + "");
		data.put("task_id", event.getTask().getId() + "");
		data.put("task_name", event.getTask().getName() + "");
		data.put("task_status", event.getTask().getTaskData().getStatus().name());

		trigger(processName + "/" + to, data);

	}

	@Override
	public void afterTaskFailedEvent(TaskEvent event) {
		// TODO Auto-generated method stub

	}

	@Override
	public void afterTaskAddedEvent(TaskEvent event) {
		taskEvent(event, "afterTaskAddedEvent");

	}

	@Override
	public void afterTaskExitedEvent(TaskEvent event) {
		// TODO Auto-generated method stub

	}

	@Override
	public void afterTaskReleasedEvent(TaskEvent event) {
		// TODO Auto-generated method stub

	}

	@Override
	public void afterTaskResumedEvent(TaskEvent event) {
		// TODO Auto-generated method stub

	}

	@Override
	public void afterTaskSuspendedEvent(TaskEvent event) {
		// TODO Auto-generated method stub

	}

	@Override
	public void afterTaskForwardedEvent(TaskEvent event) {
		// TODO Auto-generated method stub

	}

	@Override
	public void afterTaskDelegatedEvent(TaskEvent event) {
		// TODO Auto-generated method stub

	}

	@Override
	public void afterTaskNominatedEvent(TaskEvent event) {
		// TODO Auto-generated method stub

	}

}
