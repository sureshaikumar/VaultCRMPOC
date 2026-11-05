package com.veeva.vault.custom.triggers;

import com.veeva.vault.sdk.api.core.LogService;
import com.veeva.vault.sdk.api.core.ServiceLocator;
import com.veeva.vault.sdk.api.data.Record;
import com.veeva.vault.sdk.api.data.RecordChange;
import com.veeva.vault.sdk.api.data.RecordEvent;
import com.veeva.vault.sdk.api.data.RecordTrigger;
import com.veeva.vault.sdk.api.data.RecordTriggerContext;
import com.veeva.vault.sdk.api.data.RecordTriggerInfo;

import java.time.LocalDate;

@RecordTriggerInfo(
        object = "test_object__c",
        events = {RecordEvent.BEFORE_INSERT}
)
public class DemoTrigger implements RecordTrigger {

	@Override
	public void execute(RecordTriggerContext context) {

	    LogService log = ServiceLocator.locate(LogService.class);

	    log.info("=== DemoTrigger Started ===");

	    for (RecordChange change : context.getRecordChanges()) {

	        log.info("Processing record");

	        Record newRecord = change.getNew();

	        LocalDate todayDate = LocalDate.now();

	        log.info("Today's date: " + todayDate);

	        newRecord.setValue("expected_date__c", todayDate);

	        log.info("expected_date__c updated successfully");
	    }

	    log.info("=== DemoTrigger Finished ===");
	}
}