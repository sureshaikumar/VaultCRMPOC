package com.veeva.vault.custom.processors;

import com.veeva.vault.sdk.api.core.*;
import com.veeva.vault.sdk.api.data.*;
import com.veeva.vault.sdk.api.data.Record;
import com.veeva.vault.sdk.api.job.*;
import com.veeva.vault.sdk.api.notification.*;
import com.veeva.vault.sdk.api.query.Query;
import com.veeva.vault.sdk.api.query.QueryExecutionRequest;
import com.veeva.vault.sdk.api.query.QueryService;

import java.time.LocalDate;
import java.util.List;

@JobInfo(adminConfigurable = true, idempotent = true)
public class OverdueTaskJob implements Job {

    @Override
    public JobInputSupplier init(JobInitContext jobInitContext) {
        LogService logService = ServiceLocator.locate(LogService.class);
        String accountId = jobInitContext.getJobParameter("accountId", JobParamValueType.STRING);
        JobItem UseractionjobItem = jobInitContext.newJobItem();
        UseractionjobItem.setValue("id",accountId);
        List <JobItem> singleAccountId = VaultCollections.asList();
        singleAccountId.add(UseractionjobItem);

        if (accountId != null) {
            return jobInitContext.newJobInput(singleAccountId);
        }
        QueryService queryService = ServiceLocator.locate(QueryService.class);
        List<JobItem> jobItems = VaultCollections.newList();
        Query accountQuery = queryService.newQueryBuilder()
                .withSelect(VaultCollections.asList("account__v"))
                .withFrom("call_followup_task__v")
                .withWhere("completed__v = false")
                .build();

        QueryExecutionRequest request = queryService.newQueryExecutionRequestBuilder()
                .withQuery(accountQuery)
                .build();


        queryService.query(request)
                .onSuccess(resp -> {
                    resp.streamResults().forEach(row -> {
                        String account_Id = row.getValue("account__v", ValueType.STRING);
                        if (account_Id != null && !account_Id.isEmpty()) {
                            JobItem jobItem = jobInitContext.newJobItem();
                            jobItem.setValue("id", account_Id);
                            jobItems.add(jobItem);
                        }
                    });
                    logService.info("init(): Prepared " + jobItems.size() + " workflow(s) for cancellation.");
                })
                .onError(err -> logService.error("init(): Failed to query tasks: " + err.getMessage()))
                .execute();

        return jobInitContext.newJobInput(jobItems);

    }

    public void process(JobProcessContext jobProcessContext) {

        JobLogger logService = jobProcessContext.getJobLogger();
        List<JobItem> items = jobProcessContext.getCurrentTask().getItems();
        // 1. Locate Services
        QueryService queryService = ServiceLocator.locate(QueryService.class);
        RecordService recordService = ServiceLocator.locate(RecordService.class);
        NotificationService notificationService = ServiceLocator.locate(NotificationService.class);

        for (JobItem jobItem : items) {
            // 2. Retrieve Job Parameters
            String accountId = jobItem.getValue("id", JobValueType.STRING);
            if (accountId == null || accountId.isEmpty()) {
                logService.log("process(): Skipping item with missing account ID.");
                continue;
            }
            LocalDate today = LocalDate.now();

            // 3. BUILD THE QUERY (Using Binding Parameters)
            Query taskQuery = queryService.newQueryBuilder()
                    .withSelect(VaultCollections.asList("id", "assigned_to__v", "due_date__v"))
                    .withFrom("call_followup_task__v")
                    .withWhere("account__v = '" + accountId + "' AND complete__v = false")
                    .build();

            // 4. BUILD THE REQUEST
            QueryExecutionRequest queryRequest = queryService.newQueryExecutionRequestBuilder()
                    .withQuery(taskQuery)
                    .build();

            // Collections to hold updates
            List<Record> recordsToUpdate = VaultCollections.newList();
            List<String> usersToNotify = VaultCollections.newList();

            // 5. EXECUTE & STREAM
            queryService.query(queryRequest)
                    .onSuccess(queryResponse -> {
                        queryResponse.streamResults().forEach(result -> {

                            String taskId = result.getValue("id", ValueType.STRING);
                            String userId = result.getValue("assigned_to__v", ValueType.STRING);
                            LocalDate dueDate = result.getValue("due_date__v", ValueType.DATE);

                            boolean sendEmail = false;

                            // Logic A: Overdue
                            if (dueDate != null && dueDate.isBefore(today)) {
                                sendEmail = true;
                            }
                            // Logic B: Missing Date (Auto-update)
                            else if (dueDate == null) {
                                Record record = recordService.newRecordWithId("call_followup_task__v", taskId);
                                record.setValue("due_date__v", today.plusDays(3));
                                recordsToUpdate.add(record);

                                sendEmail = true;
                            }

                            if (sendEmail && userId != null) {
                                usersToNotify.add(userId);
                            }
                        });
                    })
                    .onError(queryError -> {
                        logService.log("Failed to query Account records: " + queryError.getMessage());
                    })
                    .execute();

            // 6. Batch Save Updates
            if (!recordsToUpdate.isEmpty()) {

                RecordBatchSaveRequest saveRequest = recordService.newRecordBatchSaveRequestBuilder().withRecords(recordsToUpdate).build();

                recordService.batchSaveRecords(saveRequest)
                        .onErrors(batchOperationErrors -> {
                            batchOperationErrors.stream().findFirst().ifPresent(error -> {
                                String errMsg = error.getError().getMessage();
                                int errPosition = error.getInputPosition();
                                String name = recordsToUpdate.get(errPosition).getValue("name__v", ValueType.STRING);
                                throw new RollbackException("OPERATION_NOT_ALLOWED", "Unable to create: " + name +
                                        "because of " + errMsg);
                            });
                        })
                        .execute();
            }

            // 7. Send Notifications
            if (!usersToNotify.isEmpty()) {
                NotificationQueueRequest request = notificationService.newNotificationQueueRequestBuilder()
                        .withRecipientsByUsers(usersToNotify)
                        .withNotificationQueueMessage(notificationService.newNotificationQueueMessageBuilder()
                                .withSubject("Task Attention Required")
                                .withBody("You have overdue tasks for Account: " + accountId)
                                .withNotificationText("Task Attention Required, You have overdue tasks for Account: " + accountId)
                                        .build()).build();

                // ----- 2. Queue the Notification -----
                notificationService.queueNotification(request)
                        .onSuccess((response) -> {
                            // Handle success
                            logService.log("[NOTIFICATION] failure notification queued. AccountId=" + accountId );
                        })
                        .onError((error) -> {
                            // Handle error
                            logService.log("[NOTIFICATION] Unable to queue delivery failure notification. AccountId=" + accountId + ", Error=" + error.getErrorMessage());
                        })
                        .execute();
            }
        }
    }

    @Override
    public void completeWithSuccess(JobCompletionContext jobCompletionContext) {
        JobLogger logger = jobCompletionContext.getJobLogger();
        List<JobTask> tasks = jobCompletionContext.getSuccessTasks();

        for (JobTask task : tasks) {
            logger.log(task.getTaskId() + " completed successfully, items: " + task.getItems().size());
        }
    }

    @Override
    public void completeWithError(JobCompletionContext jobCompletionContext) {
        JobResult result = jobCompletionContext.getJobResult();
        JobLogger logger = jobCompletionContext.getJobLogger();

        logger.log("completeWithError: " + result.getNumberFailedTasks() + " tasks failed out of " + result.getNumberTasks());

        List<JobTask> tasks = jobCompletionContext.getErrorTasks();
        for (JobTask task : tasks) {
            TaskOutput taskOutput = task.getTaskOutput();
            if (TaskState.ERRORS_ENCOUNTERED.equals(taskOutput.getState())) {
                logger.log(task.getTaskId() + " failed with error message " + taskOutput.getValue("firstError", JobValueType.STRING));
            } else if (TaskState.FAILED_TO_RUN.equals(taskOutput.getState())) {
                logger.log(task.getTaskId() + " failed to run " + taskOutput.getValue("firstError", JobValueType.STRING));
            }
        }
    }
}