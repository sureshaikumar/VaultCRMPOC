package com.veeva.vault.custom.pages;

import com.veeva.vault.sdk.api.core.*;
import com.veeva.vault.sdk.api.data.Record;
import com.veeva.vault.sdk.api.data.RecordService;
import com.veeva.vault.sdk.api.executeas.ExecuteAs;
import com.veeva.vault.sdk.api.executeas.ExecuteAsUser;
import com.veeva.vault.sdk.api.json.JsonObject;
import com.veeva.vault.sdk.api.json.JsonService;
import com.veeva.vault.sdk.api.page.PageController;
import com.veeva.vault.sdk.api.page.PageControllerInfo;
import com.veeva.vault.sdk.api.page.PageLoadContext;
import com.veeva.vault.sdk.api.page.PageLoadResponse;
import com.veeva.vault.sdk.api.query.Query;
import com.veeva.vault.sdk.api.query.QueryExecutionRequest;
import com.veeva.vault.sdk.api.query.QueryService;

import java.util.List;

@PageControllerInfo
@ExecuteAs(ExecuteAsUser.REQUEST_OWNER)
public class HelloWorldController implements PageController {

    @Override
    public PageLoadResponse onLoad(PageLoadContext context) {
        JsonService jsonService = ServiceLocator.locate(JsonService.class);
        String initiatorUserID = RequestContext.get().getInitiatingUserId();
        String initiatorUserName = UserNameQuery(initiatorUserID);
        // Build a JsonObject containing the initiating user ID
        JsonObject initiatingUserJsonObject = jsonService.newJsonObjectBuilder()
            .setValue("userId", initiatorUserName)
            .build();

        // Return a JsonObject containing the initiating user ID to the client code
        return context.newLoadResponseBuilder()
            .withData(initiatingUserJsonObject)
            .build();
    }
    public String UserNameQuery(String userId) {
        LogService logService = ServiceLocator.locate(LogService.class);
        QueryService queryService = ServiceLocator.locate(QueryService.class);
        List<String> users = VaultCollections.newList();

        Query myQuery= queryService.newQueryBuilder()
                .withSelect(VaultCollections.asList( "id", "name__v"))
                .withFrom("user__sys")
                .withWhere("id = '"+userId+"'")
                .build();

        QueryExecutionRequest queryExecutionRequest = queryService.newQueryExecutionRequestBuilder()
                .withQuery(myQuery)
                .build();

        queryService.query(queryExecutionRequest).onSuccess(queryExecutionResponse -> {
                    queryExecutionResponse.streamResults().forEach(queryExecutionResult -> {
                        String UserName = queryExecutionResult.getValue("name__v", ValueType.STRING);
                        if (UserName != null && !UserName.isEmpty()) {
                            users.add(UserName);
                        }
                    });
                }).onError(queryOperationError -> {
                    logService.error("Failed to query Call Type records: " + queryOperationError.getMessage());
                })
                .execute();
        return users.isEmpty() ? "" : users.get(0);
    }
}