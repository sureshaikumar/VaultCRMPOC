package com.veeva.vault.custom.triggers;

import com.veeva.vault.sdk.api.core.LogService;
import com.veeva.vault.sdk.api.core.ServiceLocator;
import com.veeva.vault.sdk.api.core.ValueType;
import com.veeva.vault.sdk.api.core.VaultCollections;
import com.veeva.vault.sdk.api.data.Record;
import com.veeva.vault.sdk.api.data.RecordBatchSaveRequest;
import com.veeva.vault.sdk.api.data.RecordChange;
import com.veeva.vault.sdk.api.data.RecordEvent;
import com.veeva.vault.sdk.api.data.RecordService;
import com.veeva.vault.sdk.api.data.RecordTrigger;
import com.veeva.vault.sdk.api.data.RecordTriggerContext;
import com.veeva.vault.sdk.api.data.RecordTriggerInfo;
import com.veeva.vault.sdk.api.query.Query;
import com.veeva.vault.sdk.api.query.QueryExecutionRequest;
import com.veeva.vault.sdk.api.query.QueryService;

import java.util.List;

@RecordTriggerInfo(
        object = "test_object__c",
        events = {RecordEvent.AFTER_INSERT}
)
public class TestObjectRecordCountTrigger implements RecordTrigger {

    @Override
    public void execute(RecordTriggerContext context) {

        LogService logService =
                ServiceLocator.locate(LogService.class);

        QueryService queryService =
                ServiceLocator.locate(QueryService.class);

        RecordService recordService =
                ServiceLocator.locate(RecordService.class);

        logService.info(
                "===== TestObjectRecordCountTrigger Started ====="
        );

        for (RecordChange recordChange : context.getRecordChanges()) {
        	
            Record insertedRecord = recordChange.getNew();
            String insertedRecordId =insertedRecord.getValue("id",ValueType.STRING);
            String insertedRecordName =insertedRecord.getValue("name__v",ValueType.STRING);
            
            logService.info("New Test Object record ID: "+ insertedRecordId);
            logService.info("New Test Object record name: "+ insertedRecordName);
            
            Query recordCountQuery =queryService.newQueryBuilder()
            		.withSelect(VaultCollections.asList("id"))
            		.withFrom("test_object__c").build();
            
            QueryExecutionRequest queryExecutionRequest =queryService
                            .newQueryExecutionRequestBuilder()
                            .withQuery(recordCountQuery)
                            .build();
            queryService.query(queryExecutionRequest)
                    .onSuccess(queryExecutionResponse -> {
                        long totalRecordCount =queryExecutionResponse
                        		.streamResults()
                        		.count();
                        
                        logService.info("Total test_object__c record count: "+ totalRecordCount);
                        
                        String description ="You are record number "+ totalRecordCount + " in the system.";
                        Record recordToUpdate =recordService.newRecordWithId("test_object__c",insertedRecordId);
                        recordToUpdate.setValue("description__c",description
                        );

                        logService.info("Description to be populated: "+ description);
                        List<Record> recordsToSave =VaultCollections.asList(recordToUpdate);

                        RecordBatchSaveRequest saveRequest =recordService
                                        .newRecordBatchSaveRequestBuilder()
                                        .withRecords(recordsToSave)
                                        .build();

                        recordService.batchSaveRecords(saveRequest)
                                .onSuccesses(successes -> {
                                    logService.info(
                                            "Description updated successfully for record: "
                                                    + insertedRecordId
                                    );
                                })
                                .onErrors(errors -> {
                                    logService.error(
                                            "Failed to update description for record: "
                                                    + insertedRecordId
                                    );
                                    errors.stream().forEach(error -> {

                                        logService.error("Record update error: "+ error.getError().getMessage());
                                    });
                                })

                                .execute();
                    })
                    .onError(queryOperationError -> {
                        logService.error("Failed to count test_object__c records: "+ queryOperationError.getMessage());
                    })

                    .execute();
        }

        logService.info(
                "===== TestObjectRecordCountTrigger Finished ====="
        );
    }
}