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

import java.util.List;

@RecordActionInfo(label= "Field Update UserAction", object = "test_object__c", usages = {Usage.USER_ACTION, Usage.LIFECYCLE_ENTRY_ACTION})
public class FieldUpdateRecordUserAction implements RecordAction {

    public boolean isExecutable(RecordActionContext context) {
        return true;
    }

    public void execute(RecordActionContext recordActionContext) {
        List<Record> records = recordActionContext.getRecords();

        Record record = records.get(0);
        String externalId = record.getValue("external_id__c", ValueType.STRING);

        if (externalId != null) {
            List <String> medcommsSubTaskName = httpRequest("vault_medcomms_connection", externalId);
            if (!medcommsSubTaskName.isEmpty()) {
                record.setValue("medcoms_subtask__c", medcommsSubTaskName.get(0));
            }
        } else {
            record.setValue("medcoms_subtask__c", null);
        }
        RecordService recordService = ServiceLocator.locate(RecordService.class);

        RecordBatchSaveRequest saveRequest = recordService.newRecordBatchSaveRequestBuilder().withRecords(records).build();

        recordService.batchSaveRecords(saveRequest)
                .onErrors(batchOperationErrors -> {
                    batchOperationErrors.stream().findFirst().ifPresent(error -> {
                        String errMsg = error.getError().getMessage();
                        int errPosition = error.getInputPosition();
                        String name = records.get(errPosition).getValue("name__v", ValueType.STRING);
                        throw new RollbackException("OPERATION_NOT_ALLOWED", "Unable to create: " + name +
                                "because of " + errMsg);
                    });
                })
                .execute();
    }

    public List <String> httpRequest(String connections, String externalId) {

        HttpService httpService = ServiceLocator.locate(HttpService.class);
        LogService logService = ServiceLocator.locate(LogService.class);
        FormHttpRequest request = httpService.newHttpRequestBuilder()
                .withConnectionName(connections)
                .withMethod(HttpMethod.POST)
                .withPath("api/v25.1/query")
                .withHeader("Content-Type", "application/x-www-form-urlencoded")
                .withHeader("Accept", "application/json")
                .withQuerystringParam("q", "SELECT name__v FROM test_crm_update__c WHERE external_id__c = '" + externalId + "'")
                .withHeader("X-VaultAPI-ClientID", "veeva-vault-promomats_medical__v")
                .build();

        List<String> SubTaskName = VaultCollections.newList();
        httpService.sendRequest(request, HttpResponseBodyValueType.JSONDATA)
                .onSuccess(httpResponse -> {

                    JsonData response = httpResponse.getResponseBody();

                    if (response.isValidJson()) {
                        String responseStatus = response.getJsonObject().getValue("responseStatus", JsonValueType.STRING);

                        if (responseStatus.equals("SUCCESS")) {

                            JsonArray data = response.getJsonObject().getValue("data", JsonValueType.ARRAY);
                            logService.info("HTTP Query Request: SUCCESS");

                            //Retrieve each record returned from the VQL query.
                            //Each element of the returned `data` JsonArray is a record with it's queried fields.
                            for (int i = 0; i < data.getSize(); i++) {
                                JsonObject objectRecord = data.getValue(i, JsonValueType.OBJECT);
                                String sourceName = objectRecord.getValue("name__v", JsonValueType.STRING);
                                SubTaskName.add(sourceName);
                            }
                        }
                    } else {
                        logService.info("getIntTransIds error: Received a non-JSON response.");
                    }
                })
                .onError(httpOperationError -> {
                    logService.info("getIntTransIds error: httpOperationError.");
                }).execute();

        return SubTaskName;
    }
}