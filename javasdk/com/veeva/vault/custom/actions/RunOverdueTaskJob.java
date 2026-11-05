package com.veeva.vault.custom.actions;

import com.veeva.vault.sdk.api.action.RecordAction;
import com.veeva.vault.sdk.api.action.RecordActionContext;
import com.veeva.vault.sdk.api.action.RecordActionInfo;
import com.veeva.vault.sdk.api.core.ServiceLocator;
import com.veeva.vault.sdk.api.data.Record;
import com.veeva.vault.sdk.api.job.JobParameters;
import com.veeva.vault.sdk.api.job.JobService;

@RecordActionInfo(object = "account__v", label = "Send Overdue Reminders")
public class RunOverdueTaskJob implements RecordAction {

    public boolean isExecutable(RecordActionContext context) {
        return true;
    }

    public void execute(RecordActionContext context) {
        JobService jobService = ServiceLocator.locate(JobService.class);

        // 1. Get the Current Account
        // (We assume single selection for this action)
        Record account = context.getRecords().get(0);
        String accountId = account.getValue("id", com.veeva.vault.sdk.api.core.ValueType.STRING);

        // 2. Prepare Job Parameters
        // We pass the Account ID to the job so it knows which tasks to check.
        JobParameters params = jobService.newJobParameters("overdue_task_job__c");
        params.setValue("accountId", accountId);

        // 3. Run the Job
        //  runs in the background. The user can continue working immediately.
        jobService.run(params);
    }
}