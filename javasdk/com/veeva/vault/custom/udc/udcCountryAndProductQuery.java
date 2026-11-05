package com.veeva.vault.custom.udc;

import com.veeva.vault.sdk.api.core.*;
import com.veeva.vault.sdk.api.query.Query;
import com.veeva.vault.sdk.api.query.QueryExecutionRequest;
import com.veeva.vault.sdk.api.query.QueryService;

import java.util.List;

@UserDefinedClassInfo
public class udcCountryAndProductQuery {

    public String getCountryName(String CountryId) {
        LogService logService = ServiceLocator.locate(LogService.class);
        QueryService queryService = ServiceLocator.locate(QueryService.class);
        List<String> countryNames = VaultCollections.newList();

        Query myQuery= queryService.newQueryBuilder()
                .withSelect(VaultCollections.asList("name__v", "id"))
                .withFrom("country__v")
                .withWhere("id = '"+CountryId+"'")
                .build();

        QueryExecutionRequest queryExecutionRequest = queryService.newQueryExecutionRequestBuilder()
                .withQuery(myQuery)
                .build();

        queryService.query(queryExecutionRequest).onSuccess(queryExecutionResponse -> {
            queryExecutionResponse.streamResults().forEach(queryExecutionResult -> {
                String countryName = queryExecutionResult.getValue("name__v", ValueType.STRING);
                if (countryName != null && !countryName.isEmpty()) {
                    countryNames.add(countryName);
                }
            });
        }).onError(queryOperationError -> {
                    logService.error("Failed to query country records: " + queryOperationError.getMessage());
                })
                .execute();
        return countryNames.isEmpty() ? "" : countryNames.get(0);
    }

    public String getProductName(String ProductId) {
        LogService logService = ServiceLocator.locate(LogService.class);
        QueryService queryService = ServiceLocator.locate(QueryService.class);
        List<String> countryNames = VaultCollections.newList();

        Query myQuery= queryService.newQueryBuilder()
                .withSelect(VaultCollections.asList("name__v", "id"))
                .withFrom("product__v")
                .withWhere("id = '"+ProductId+"'")
                .build();

        QueryExecutionRequest queryExecutionRequest = queryService.newQueryExecutionRequestBuilder()
                .withQuery(myQuery)
                .build();

        queryService.query(queryExecutionRequest).onSuccess(queryExecutionResponse -> {
                    queryExecutionResponse.streamResults().forEach(queryExecutionResult -> {
                        String countryName = queryExecutionResult.getValue("name__v", ValueType.STRING);
                        if (countryName != null && !countryName.isEmpty()) {
                            countryNames.add(countryName);
                        }
                    });
                }).onError(queryOperationError -> {
                    logService.error("Failed to query country records: " + queryOperationError.getMessage());
                })
                .execute();
        return countryNames.isEmpty() ? "" : countryNames.get(0);
    }

}