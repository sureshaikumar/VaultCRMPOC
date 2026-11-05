package com.veeva.vault.custom.udc;

import com.veeva.vault.sdk.api.core.*;
import com.veeva.vault.sdk.api.query.Query;
import com.veeva.vault.sdk.api.query.QueryExecutionRequest;
import com.veeva.vault.sdk.api.query.QueryService;

import java.util.List;

@UserDefinedClassInfo
public class udcProductQuery {

    public String verifyProduct(String ProductId, String account) {
        LogService logService = ServiceLocator.locate(LogService.class);
        QueryService queryService = ServiceLocator.locate(QueryService.class);
        List<String> product = VaultCollections.newList();

        Query myQuery= queryService.newQueryBuilder()
                .withSelect(VaultCollections.asList("id"))
                .withFrom("product_metrics__v")
                .withWhere("(products__v = '"+ProductId+"') AND (account__v = '"+account+"')")
                .build();

        QueryExecutionRequest queryExecutionRequest = queryService.newQueryExecutionRequestBuilder()
                .withQuery(myQuery)
                .build();

        queryService.query(queryExecutionRequest).onSuccess(queryExecutionResponse -> {
                    queryExecutionResponse.streamResults().forEach(queryExecutionResult -> {
                        String productId = queryExecutionResult.getValue("id", ValueType.STRING);

                            if (productId != null && !productId.isEmpty()) {
                                product.add(productId);
                            }
                    });
                }).onError(queryOperationError -> {
                    logService.error("Failed to query Product records: " + queryOperationError.getMessage());
                })
                .execute();
        return product.isEmpty() ? "" : product.get(0);
    }

}