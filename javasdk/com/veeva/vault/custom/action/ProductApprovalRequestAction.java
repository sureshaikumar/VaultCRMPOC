package com.veeva.vault.custom.action;

import java.util.List;
import com.veeva.vault.sdk.api.action.RecordAction;
import com.veeva.vault.sdk.api.action.RecordActionContext;
import com.veeva.vault.sdk.api.action.RecordActionInfo;
import com.veeva.vault.sdk.api.action.Usage;
import com.veeva.vault.sdk.api.core.LogService;
import com.veeva.vault.sdk.api.core.ServiceLocator;
import com.veeva.vault.sdk.api.core.ValueType;
import com.veeva.vault.sdk.api.data.Record;
import com.veeva.vault.sdk.api.core.VaultCollections;
import com.veeva.vault.sdk.api.query.Query;
import com.veeva.vault.sdk.api.query.QueryExecutionRequest;
import com.veeva.vault.sdk.api.query.QueryService;

@RecordActionInfo(label = "Product Approval Request", object = "product_approval_request__c", usages = {
                Usage.USER_ACTION,
                Usage.LIFECYCLE_ENTRY_ACTION
})
public class ProductApprovalRequestAction implements RecordAction {

        @Override
        public boolean isExecutable(RecordActionContext context) {
                // TODO Auto-generated method stub
                return true;
        }

        @Override
        public void execute(RecordActionContext context) {
                // TODO Auto-generated method stub

                LogService logService = ServiceLocator.locate(LogService.class);

                List<Record> records = context.getRecords();

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

                        logService.info( "STEP 6 QUERY CODE STARTED");
                        
                        QueryService queryService = ServiceLocator.locate(QueryService.class);

                        String safeProductCode = queryService.escapeValue(productCode);

                        Query productQuery = queryService.newQueryBuilder()
                                        .withSelect(
                                                        VaultCollections.asList(
                                                                        "id",
                                                                        "product_name__c",
                                                                        "product_code__c"))
                                        .withFrom("product__c")
                                        .withWhere(
                                                        "product_code__c = '"
                                                                        + safeProductCode
                                                                        + "'")
                                        .build();

                        QueryExecutionRequest queryRequest = queryService
                                        .newQueryExecutionRequestBuilder()
                                        .withQuery(productQuery)
                                        .build();

                        queryService
                                        .query(queryRequest)

                                        .onSuccess(response -> {

                                                boolean productFound = response.streamResults()
                                                                .findFirst()
                                                                .isPresent();

                                                logService.info(
                                                                "Product Found: {}",
                                                                productFound);
                                        })

                                        .onError(error -> {

                                                logService.error(
                                                                "Product Query Failed: {}",
                                                                error.getMessage());
                                        })

                                        .execute();

                }
        }

}