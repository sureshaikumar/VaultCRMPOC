package com.veeva.vault.custom.triggers;

import com.veeva.vault.sdk.api.core.*;
import com.veeva.vault.sdk.api.data.*;

import java.util.List;

@RecordTriggerInfo(object = "account_merge_request__c", events = {RecordEvent.AFTER_INSERT})
public class MergeAccount implements RecordTrigger {

    public void execute(RecordTriggerContext context) {

        LogService logService = ServiceLocator.locate(LogService.class);
        RecordService recordService = ServiceLocator.locate(RecordService.class);

        for (RecordChange recordChange : context.getRecordChanges()) {
            com.veeva.vault.sdk.api.data.Record newRecord = recordChange.getNew();

            String winnerId = newRecord.getValue("winner_id__c", ValueType.STRING);
            String duplicateId = newRecord.getValue("duplicate_id__c", ValueType.STRING);

            if (winnerId == null || winnerId.isEmpty() || duplicateId == null || duplicateId.isEmpty()) {
                logService.info("Winner ID or Duplicate ID is missing. Skipping merge.");
                continue;
            }

            RecordMergeSetInput mergeInput = recordService.newRecordMergeSetInputBuilder()
                    .withMainRecordId(winnerId)
                    .withDuplicateRecordId(duplicateId)
                    .build();

            RecordMergeRequest mergeRequest = recordService.newRecordMergeRequestBuilder()
                    .withObjectName("account__v")
                    .withRecordMerges(VaultCollections.asList(mergeInput))
                    .build();

            recordService.mergeRecords(mergeRequest)
                    .onSuccess(success -> {
                        String jobId = success.getJobId();
                        logService.info("Successfully started merge job. Job ID: " + jobId);
                    })
                    .onError(error -> {
                        List<RecordMergeError> errorList = error.getErrors();
                        for (RecordMergeError mergeError : errorList) {
                            logService.info("Merge failed. Message: " + mergeError.getError().getMessage());
                        }
                    })
                    .execute();
        }
    }
}