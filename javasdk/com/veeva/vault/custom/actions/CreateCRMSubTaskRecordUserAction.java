package com.veeva.vault.custom.actions;

import com.veeva.vault.sdk.api.action.*;
import com.veeva.vault.sdk.api.core.*;
import com.veeva.vault.sdk.api.data.Record;
import com.veeva.vault.sdk.api.data.RecordBatchSaveRequest;
import com.veeva.vault.sdk.api.data.RecordService;
import com.veeva.vault.sdk.api.http.FormHttpRequest;
import com.veeva.vault.sdk.api.http.HttpMethod;
import com.veeva.vault.sdk.api.http.HttpResponseBodyValueType;
import com.veeva.vault.sdk.api.http.HttpService;
import com.veeva.vault.sdk.api.json.JsonArray;
import com.veeva.vault.sdk.api.json.JsonData;
import com.veeva.vault.sdk.api.json.JsonObject;
import com.veeva.vault.sdk.api.json.JsonValueType;
import com.veeva.vault.sdk.api.queue.Message;
import com.veeva.vault.sdk.api.queue.PutMessageResponse;
import com.veeva.vault.sdk.api.queue.QueueService;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RecordActionInfo(label= "Create New Sub Task in MedComms", object = "test_object__c", usages = {Usage.USER_ACTION, Usage.LIFECYCLE_ENTRY_ACTION})
public class CreateCRMSubTaskRecordUserAction implements RecordAction {

    public static final String QUEUE_NAME = "crm_to_medcomms__c";
    public boolean isExecutable(RecordActionContext context) {
        return true;
    }

        public void execute(RecordActionContext recordActionContext) {
            LogService logService = ServiceLocator.locate(LogService.class);
            List<Record> records = recordActionContext.getRecords();

            Record record = records.get(0);
            String externalId = record.getValue("external_id__c", ValueType.STRING);
            String TestObjectName = record.getValue("name__v", ValueType.STRING);

            QueueService qs = ServiceLocator.locate(QueueService.class);
            Message ms = qs.newMessage(QUEUE_NAME)
                    .setAttribute("External ID", externalId)
                    .setAttribute("Test Object Name", TestObjectName);
            PutMessageResponse response = qs.putMessage(ms);

            logService.info("job created in queue {}", QUEUE_NAME);
            logService.info("Connection Name: " + response.getPutMessageResults().get(0).getConnectionName());
            logService.info("Message Name: " + response.getPutMessageResults().get(0).getMessageId());
            logService.info("Message Details: " + response.getPutMessageResults().get(0));

            if (response.getError() != null) {
                logService.info("Error Queuing Failed: " + response.getError().getMessage());
            }

        }

}