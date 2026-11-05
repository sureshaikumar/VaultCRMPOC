package com.veeva.vault.custom.actions;

import com.veeva.vault.sdk.api.action.RecordAction;
import com.veeva.vault.sdk.api.action.RecordActionContext;
import com.veeva.vault.sdk.api.action.RecordActionInfo;
import com.veeva.vault.sdk.api.action.Usage;
import com.veeva.vault.sdk.api.core.LogService;
import com.veeva.vault.sdk.api.core.ServiceLocator;
import com.veeva.vault.sdk.api.core.ValueType;
import com.veeva.vault.sdk.api.core.VaultCollections;
import com.veeva.vault.sdk.api.data.Record;
import com.veeva.vault.sdk.api.data.RecordBatchSaveRequest;
import com.veeva.vault.sdk.api.data.RecordService;

import java.util.List;

@RecordActionInfo(
        label = "Create Test Object Clone",
        object = "test_object__c",
        usages = {Usage.LIFECYCLE_ENTRY_ACTION}
)
public class CreateTestObjectClone implements RecordAction {

    @Override
    public boolean isExecutable(RecordActionContext context) {
        return true;
    }

    @Override
    public void execute(RecordActionContext context) {
        LogService logService =ServiceLocator.locate(LogService.class); //instance of service package
        RecordService recordService =ServiceLocator.locate(RecordService.class);
        logService.info("Starting Create Test Object Clone action execution");
        for (Record sourceRecord : context.getRecords()) {
            String sourceRecordId =sourceRecord.getValue("id",ValueType.STRING);

            String sourceRecordName =sourceRecord.getValue("name__v",ValueType.STRING);
            
            String testvalidation = sourceRecord.getValue("test_validation__c", ValueType.STRING);

            logService.info("Source Test Object record ID: "+ sourceRecordId);

            logService.info("Source Test Object record name: "+ sourceRecordName);
            
            Record cloneRecord =recordService.newRecord("test_object__c");

            String cloneRecordName ="Clone of " + sourceRecordName;
            
            String cloneTestValidation = testvalidation;

            /*
             * Populate only the clone name and test_valdation.
             */
            cloneRecord.setValue("name__v",cloneRecordName);
            
            cloneRecord.setValue("test_validation__c",cloneTestValidation);

            logService.info("New Test Object record name: "+ cloneRecordName);

            logService.info("New Test Object record type: base__v");

            List<Record> recordsToSave =
                    VaultCollections.asList(cloneRecord);

            RecordBatchSaveRequest saveRequest =
                    recordService
                            .newRecordBatchSaveRequestBuilder()
                            .withRecords(recordsToSave)
                            .build();

            recordService.batchSaveRecords(saveRequest)

                    .onSuccesses(successes -> {

                        logService.info("Clone record created successfully for source record: " + sourceRecordId);

                        logService.info("Created clone record name: "+ cloneRecordName);})

                    .onErrors(errors -> {

                        logService.error("Failed to create clone for source record: "+ sourceRecordId);

                        errors.stream().forEach(error -> {

                            logService.error("Clone creation error: "+ error.getError().getMessage());
                        });
                    })

                    .execute();
        }

        logService.info("Completed Create Test Object Clone action execution");
    }
}