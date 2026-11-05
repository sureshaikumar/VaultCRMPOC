package com.veeva.vault.custom.triggers;

import java.util.List;

import com.veeva.vault.sdk.api.core.LogService;
import com.veeva.vault.sdk.api.core.RollbackException;
import com.veeva.vault.sdk.api.core.ServiceLocator;
import com.veeva.vault.sdk.api.core.ValueType;
import com.veeva.vault.sdk.api.core.VaultCollectors;

import com.veeva.vault.sdk.api.data.Record;
import com.veeva.vault.sdk.api.data.RecordEvent;
import com.veeva.vault.sdk.api.data.RecordTrigger;
import com.veeva.vault.sdk.api.data.RecordTriggerContext;
import com.veeva.vault.sdk.api.data.RecordTriggerInfo;


@RecordTriggerInfo(
    object = "product_approval_request__c",
    events = {
        RecordEvent.BEFORE_INSERT,
        RecordEvent.BEFORE_UPDATE
    }
)
public class ProductApprovalRequestTrigger
        implements RecordTrigger {


    @Override
    public void execute(RecordTriggerContext context) {

        LogService logService =
            ServiceLocator.locate(LogService.class);
                
        logService.info("Entering in execute() of ProductApprovalRequestTrigger");

        List<Record> records =
            context.getRecordChanges()
                .stream()
                .map(recordChange -> recordChange.getNew())
                .collect(VaultCollectors.toList());


        for (Record record : records) {

            String productName =
                record.getValue(
                    "product_name__c",
                    ValueType.STRING
                );

            String productCode =
                record.getValue(
                    "product_code__c",
                    ValueType.STRING
                );

            String comments =
                record.getValue(
                    "comments__c",
                    ValueType.STRING
                );


            List<String> requestTypes =
                record.getValue(
                    "request_type__c",
                    ValueType.PICKLIST_VALUES
                );


            String requestType = null;

            if (requestTypes != null
                    && !requestTypes.isEmpty()) {

                requestType = requestTypes.get(0);
            }


            // Logging

            logService.info(
                "Request Type: {}",
                requestType
            );

            logService.info(
                "Product Name: {}",
                productName
            );

            logService.info(
                "Product Code: {}",
                productCode
            );

            logService.info(
                "Comments: {}",
                comments
            );


            // ==========================================
            // NEW PRODUCT
            // ==========================================

            if ("new_product__c".equals(requestType)) {

                logService.info(
                    "Validating New Product request"
                );

                validateRequired(
                    productName,
                    "MISSING_PRODUCT_NAME",
                    "Product Name is required for New Product request."
                );

                validateProductCode(
                    productCode,
                    logService
                );
            }


            // ==========================================
            // UPDATE PRODUCT
            // ==========================================

            else if ("update_product__c".equals(requestType)) {

                logService.info(
                    "Validating Update Product request"
                );

                validateProductCode(
                    productCode,
                    logService
                );

                validateRequired(
                    comments,
                    "MISSING_COMMENTS",
                    "Comments are required for Update Product request."
                );
            }


            // ==========================================
            // DEACTIVATE PRODUCT
            // ==========================================

            else if ("deactivate_product__c".equals(requestType)) {

                logService.info(
                    "Validating Deactivate Product request"
                );

                validateProductCode(
                    productCode,
                    logService
                );
            }
        }
    }


    // ==============================================
    // COMMON REQUIRED FIELD VALIDATION
    // ==============================================

    private void validateRequired(
            String value,
            String errorCode,
            String errorMessage) {

        if (value == null || value.trim().isEmpty()) {

            throw new RollbackException(
                errorCode,
                errorMessage
            );
        }
    }


    // ==============================================
    // PRODUCT CODE VALIDATION
    // ==============================================

    private void validateProductCode(
            String productCode,
            LogService logService) {


        // Check mandatory field

        if (productCode == null
                || productCode.trim().isEmpty()) {

            logService.error(
                "Product Code is null/empty"
            );

            throw new RollbackException(
                "MISSING_PRODUCT_CODE",
                "Product Code is required."
            );
        }


        // Check format

        if (!productCode.startsWith("PRD-")
                || productCode.length() <= 4) {

            logService.error(
                "Invalid Product Code: {}",
                productCode
            );

            throw new RollbackException(
                "INVALID_PRODUCT_CODE",
                "Product Code must start with PRD- and contain a value after it."
            );
        }
    }
}