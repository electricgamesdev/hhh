package com.hydrides.processor;

import java.util.List;

import org.apache.avro.generic.GenericData.Record;
import org.jbpm.process.audit.JPAWorkingMemoryDbLogger;
import org.kie.api.task.TaskService;
import org.kie.api.task.model.Status;
import org.kie.api.task.model.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JBPMTaskProcessor extends AbstractJBPMProcessor {

	Logger log = LoggerFactory.getLogger(getClass());

	public JBPMTaskProcessor(String processName, String operation) {
		super(processName, operation);
	}

	@Override
	public Object process(List<Record> list) {
		init(list);
		JPAWorkingMemoryDbLogger jbpmLogger = new JPAWorkingMemoryDbLogger(getKsession());

		Record record = list.get(0);
		Long wid = new Long(record.get("work_item_id").toString());
		Long tid = new Long(record.get("task_id").toString());
		TaskService taskService = getTaskService();

		Task task = null;
		if (wid != 0) {
			task = taskService.getTaskByWorkItemId(wid);
		} else if (tid != null) {
			task = taskService.getTaskById(tid);
		} else {
			return "Work Item Id or Task Id missing";
		}

		if (task != null) {
			Long taskId = tid == 0 ? task.getId() : tid;
			String userId = record.get("user_id").toString();
			String targetUserId = record.get("target_user_id").toString();
			String targetEntityId = record.get("target_entity_id").toString();

			Status status = Status.Error;

			if ("start".equalsIgnoreCase(getOpration())) {
				taskService.start(taskId, userId);
				status = Status.InProgress;
			} else if ("complete".equalsIgnoreCase(getOpration())) {
				taskService.complete(taskId, userId, null);
				status = Status.Completed;
			} else if ("activate".equalsIgnoreCase(getOpration())) {
				taskService.activate(taskId, userId);
				status = Status.Ready;
			} else if ("claim".equalsIgnoreCase(getOpration())) {
				taskService.claim(taskId, userId);
				status = Status.InProgress;
			} else if ("claimNextAvailable".equalsIgnoreCase(getOpration())) {
				taskService.claimNextAvailable(userId, "us-en");
				status = Status.InProgress;
			} else if ("delegate".equalsIgnoreCase(getOpration())) {
				taskService.delegate(taskId, userId, targetUserId);
				status = Status.InProgress;
			} else if ("exit".equalsIgnoreCase(getOpration())) {
				taskService.exit(taskId, userId);
				status = Status.Exited;
			} else if ("fail".equalsIgnoreCase(getOpration())) {
				taskService.fail(taskId, userId, null);
				status = Status.Failed;
			} else if ("forward".equalsIgnoreCase(getOpration())) {
				taskService.forward(taskId, userId, targetEntityId);
				status = Status.Ready;
			} else if ("release".equalsIgnoreCase(getOpration())) {
				taskService.release(taskId, userId);
				status = Status.Ready;
			} else if ("resume".equalsIgnoreCase(getOpration())) {
				taskService.resume(taskId, userId);
				status = Status.InProgress;
			} else if ("suspend".equalsIgnoreCase(getOpration())) {
				taskService.suspend(taskId, userId);
				status = Status.Suspended;
			}

			return getTask(taskId, status);
		}
		return null;

	}

}
