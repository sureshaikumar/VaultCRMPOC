package com.veeva.vault.custom.actions;

import com.veeva.vault.sdk.api.action.RecordActionInfo;
import com.veeva.vault.sdk.api.action.*;
import com.veeva.vault.sdk.api.core.*;
import com.veeva.vault.sdk.api.data.Record;
import com.veeva.vault.sdk.api.data.*;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

/**
 * Record Action: Calculate Days Remaining
 * This action is triggered manually by the user from the "Gear Menu" on a Call Objective record.
 * It calculates the number of business days from TODAY until the Due Date.
 */

@RecordActionInfo(label= "Business Days Calculator", object = "call_followup_task__v", usages = {Usage.USER_ACTION, Usage.LIFECYCLE_ENTRY_ACTION})
public class callDaysCalculator implements RecordAction {

    public boolean isExecutable(RecordActionContext context) {
        return true;
    }

    public void execute(RecordActionContext recordActionContext) {

        RecordService recordService = ServiceLocator.locate(RecordService.class);
        List<Record> recordsToUpdate = VaultCollections.newList();
        for (Record CallFollowUpRecord : recordActionContext.getRecords()) {
            String id = CallFollowUpRecord.getValue("id", ValueType.STRING);
            LocalDate dueDate = CallFollowUpRecord.getValue("due_date__v", ValueType.DATE);

            if (dueDate != null) {
                LocalDate currentDate = LocalDate.now();

                // If the due date is in the past, result is 0 or negative
                if (currentDate.isAfter(dueDate)) {
                    // specific logic for overdue (optional)
                    continue;
                }

                int businessDays = getBusinessDays(currentDate, dueDate);
                Record update = recordService.newRecordWithId("call_followup_task__v1", id);
                update.setValue("due_days_calculator__c", String.valueOf(businessDays));
                recordsToUpdate.add(update);

            }
        }
        if (!recordsToUpdate.isEmpty()) {
            RecordBatchSaveRequest saveRequest = recordService.newRecordBatchSaveRequestBuilder().withRecords(recordsToUpdate).build();
            recordService.batchSaveRecords(saveRequest)
                    .onErrors(batchOperationErrors -> {
                        batchOperationErrors.stream().findFirst().ifPresent(error -> {
                            String errMsg = error.getError().getMessage();
                            int errPosition = error.getInputPosition();
                            String id = recordsToUpdate.get(errPosition).getValue("id", ValueType.STRING);
                            throw new RollbackException("OPERATION_NOT_ALLOWED", "Unable to Update: " + id +
                                    "because of " + errMsg);
                        });
                    })
                    .execute();
        }
    }
    // --- THE "EXCLUDE WEEKENDS" LOGIC ---
    public static int getBusinessDays(LocalDate currentDate, LocalDate dueDate) {
        int businessDays = 0;
        LocalDate tempDate = currentDate;

        // Loop through every day from Now until Due Date
        while (tempDate.isBefore(dueDate)) {
            // Move to next day first (or check current depending on requirement)
            tempDate = tempDate.plusDays(1);

            DayOfWeek day = tempDate.getDayOfWeek();

            // Only count if it is NOT Saturday AND NOT Sunday
            if (day != DayOfWeek.SATURDAY && day != DayOfWeek.SUNDAY) {
                businessDays++;
            }
        }
        return businessDays;
    }
}