package com.veeva.vault.custom.action;

import java.util.List;
import java.util.Map;

import com.veeva.vault.custom.udc.PickListHttpRequest;
import com.veeva.vault.sdk.api.action.RecordAction;
import com.veeva.vault.sdk.api.action.RecordActionContext;
import com.veeva.vault.sdk.api.action.RecordActionInfo;
import com.veeva.vault.sdk.api.action.Usage;
import com.veeva.vault.sdk.api.core.LogService;
import com.veeva.vault.sdk.api.core.ServiceLocator;
import com.veeva.vault.sdk.api.core.ValueType;
import com.veeva.vault.sdk.api.data.Record;
import com.veeva.vault.sdk.api.data.RecordBatchSaveRequest;
import com.veeva.vault.sdk.api.data.RecordService;
import com.veeva.vault.sdk.api.core.VaultCollections;

@RecordActionInfo(label = "CRM_Object_Test", object = "crm_objecttest__c", usages = {
                Usage.USER_ACTION,
                Usage.LIFECYCLE_ENTRY_ACTION
})
public class RecordActionTest3 implements RecordAction {

        @Override
        public boolean isExecutable(RecordActionContext context) {
                return true;
        }

        @Override
        public void execute(RecordActionContext context) {

                List<Record> records = context.getRecords();

                Record record = records.get(0);

                LogService logService = ServiceLocator.locate(LogService.class);

                // ----------------------------
                // Request Type
                // ----------------------------

                List<String> requestTypes = record.getValue(
                                "request_type__c",
                                ValueType.PICKLIST_VALUES);

                String requestType = null;

                if (requestTypes != null &&
                                !requestTypes.isEmpty()) {

                        requestType = requestTypes.get(0);
                }

                // ----------------------------
                // Other fields
                // ----------------------------

                String newProductName = record.getValue(
                                "new_product_request_name__c",
                                ValueType.STRING);

                String existingProductName = record.getValue(
                                "existing_product_name__c",
                                ValueType.STRING);

                logService.info(
                                "Request Type: {}",
                                requestType);

                logService.info(
                                "New Product Name: {}",
                                newProductName);

                logService.info(
                                "Existing Product Name: {}",
                                existingProductName);

                PickListHttpRequest pickListHttpRequest = new PickListHttpRequest();

                Map<String, String> result = null;

                // ==================================================
                // CASE 1: NEW PRODUCT
                // ==================================================

                if ("new__c".equals(requestType)) {

                        logService.info(
                                        "Processing NEW Product Request");

                        result = pickListHttpRequest.newPicklistRequest(
                                        "crm_training_connection",
                                        newProductName);
                }

                // ==================================================
                // CASE 2 & 3:
                // UPDATE LABEL OR UPDATE NAME
                // ==================================================

                else if ("update_picklist_label__c".equals(requestType)
                                ||
                                "update_picklist_name__c".equals(requestType)
                        || "inactive_picklist_value__c".equals(requestType)) {

                        logService.info(
                                        "Processing Picklist Update: {}",
                                        requestType);

                        result = pickListHttpRequest.updatePicklistRequest(
                                        "crm_training_connection",
                                        existingProductName,
                                        newProductName,
                                        requestType);
                }

                // ==================================================
                // UPDATE SUCCESS / FAILURE
                // ==================================================

                if (result != null) {

                        String status = result.get("STATUS");

                        logService.info(
                                        "Final HTTP Result Status: {}",
                                        status);

                        List<String> statusValues;

                        if ("SUCCESS".equalsIgnoreCase(status)) {

                                statusValues = VaultCollections.asList(
                                                "success__c");

                        } else {

                                statusValues = VaultCollections.asList(
                                                "failure__c");
                        }

                        record.setValue(
                                        "product_create_status__c",
                                        statusValues);
                }

                // ==================================================
                // SAVE REQUEST RECORD
                // ==================================================

                RecordService recordService = ServiceLocator.locate(
                                RecordService.class);

                RecordBatchSaveRequest saveRequest = recordService
                                .newRecordBatchSaveRequestBuilder()
                                .withRecords(records)
                                .build();

                recordService
                                .batchSaveRecords(saveRequest)
                                .onErrors(batchOperationErrors -> {

                                        batchOperationErrors
                                                        .stream()
                                                        .forEach(error -> {

                                                                logService.error(
                                                                                "Error saving CRM Product Request: {}",
                                                                                error
                                                                                                .getError()
                                                                                                .getMessage());
                                                        });
                                })
                                .execute();
        }
}