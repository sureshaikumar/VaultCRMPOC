package com.veeva.vault.custom.udc;

import com.veeva.vault.sdk.api.core.*;
import com.veeva.vault.sdk.api.query.Query;
import com.veeva.vault.sdk.api.query.QueryExecutionRequest;
import com.veeva.vault.sdk.api.query.QueryService;

import java.util.List;

@UserDefinedClassInfo
public class udcAccountQuery {

    public String getAccountName(String AccountId) {
        LogService logService = ServiceLocator.locate(LogService.class);
        QueryService queryService = ServiceLocator.locate(QueryService.class);
        List<String> accountNames = VaultCollections.newList();

        Query myQuery= queryService.newQueryBuilder()
                .withSelect(VaultCollections.asList("name__v", "id"))
                .withFrom("account__v")
                .withWhere("id = '"+ AccountId +"'")
                .build();

        QueryExecutionRequest queryExecutionRequest = queryService.newQueryExecutionRequestBuilder()
                .withQuery(myQuery)
                .build();

        queryService.query(queryExecutionRequest).onSuccess(queryExecutionResponse -> {
            queryExecutionResponse.streamResults().forEach(queryExecutionResult -> {
                String accountName = queryExecutionResult.getValue("name__v", ValueType.STRING);
                if (accountName != null && !accountName.isEmpty()) {
                    accountNames.add(accountName);
                }
            });
        }).onError(queryOperationError -> {
                    logService.error("Failed to query Account records: " + queryOperationError.getMessage());
                })
                .execute();
        return accountNames.isEmpty() ? "" : accountNames.get(0);
    }

}