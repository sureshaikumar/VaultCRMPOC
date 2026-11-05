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


@RecordTriggerInfo(object = "meena_test_product_approval__c", events = {
        RecordEvent.BEFORE_INSERT,
        RecordEvent.BEFORE_UPDATE
})
public class MeenaTestProductApprovalRequest implements RecordTrigger{

    @Override
    public void execute(RecordTriggerContext context) {
        // TODO Auto-generated method stub

        LogService logService = ServiceLocator.locate(LogService.class);

        List<Record> records = context.getRecordChanges()
                .stream()
                .map(recordChange -> recordChange.getNew())
                .collect(VaultCollectors.toList());

            for (Record record : records) {

            String productName = record.getValue(
                    "product_name__c",
                    ValueType.STRING);

            String productCode = record.getValue(
                    "product_code__c",
                    ValueType.STRING);

            List<String> requestTypes = record.getValue(
                    "request_type__c",
                    ValueType.PICKLIST_VALUES);

            String requestType = null;

            if (requestTypes != null &&
                    !requestTypes.isEmpty()) {

                requestType = requestTypes.get(0);
            }

            String comments = record.getValue(
                    "comments__c",
                    ValueType.STRING);

            logService.info(
                    "Request Type: {}",
                    requestType);

            logService.info(
                    "Product Name: {}",
                    productName);

            logService.info(
                    "Product Code: {}",
                    productCode);

            logService.info(
                    "Comments: {}",
                    comments);

             if ("new__c".equals(requestType)) {

                 logService.info("Validating New Product request");

                if (productName == null || productName.trim().isEmpty()) {

                    // validation error here
                     logService.error("product name is null/empty");
                     throw new RollbackException(
                            "MISSING_PRODUCT_NAME",
                            "Product Name is required for New Product request.");
                }

            if (productCode == null || productCode.trim().isEmpty()) {

                     // validation error here
                    logService.error("product code is null/empty");
                     throw new RollbackException(
                             "MISSING_PRODUCT_CODE",
                             "Product Code is required for New Product request.");
                 }
            }

            // else if ("update_product__c".equals(requestType)) {

            //     logService.info("Validating Update Product request");

            //     if (productCode == null || productCode.trim().isEmpty()) {

            //         throw new RollbackException(
            //                 "MISSING_PRODUCT_CODE",
            //                 "Product Code is required for Update Product request.");
            //     }

            //     if (comments == null || comments.trim().isEmpty()) {

            //         throw new RollbackException(
            //                 "MISSING_COMMENTS",
            //                 "Comments are required for Update Product request.");
            //     }
            // } else if ("deactivate_product__c".equals(requestType)) {

            //     logService.info("Validating Deactivate Product request");

            //     if (productCode == null || productCode.trim().isEmpty()) {

            //         throw new RollbackException(
            //                 "MISSING_PRODUCT_CODE",
            //                 "Product Code is required for Deactivate Product request.");
            //     }
            // }
            // // Common Product Code format validation
            // if (productCode != null && !productCode.trim().isEmpty()) {

            //     if (!productCode.matches("^PRD-[A-Z0-9]+(?:-[A-Z0-9]+)*$")) {

            //         throw new RollbackException(
            //                 "INVALID_PRODUCT_CODE",
            //                 "Product Code must start with PRD-.");
            //     }
            // }
        }

    }
    
}