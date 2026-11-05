package com.veeva.vault.custom.udc;

import com.veeva.vault.sdk.api.core.LogService;
import com.veeva.vault.sdk.api.core.ServiceLocator;
import com.veeva.vault.sdk.api.core.UserDefinedClassInfo;
import com.veeva.vault.sdk.api.core.VaultCollections;
import com.veeva.vault.sdk.api.http.FormHttpRequest;
import com.veeva.vault.sdk.api.http.HttpMethod;
import com.veeva.vault.sdk.api.http.HttpResponseBodyValueType;
import com.veeva.vault.sdk.api.http.HttpService;
import com.veeva.vault.sdk.api.json.JsonArray;
import com.veeva.vault.sdk.api.json.JsonData;
import com.veeva.vault.sdk.api.json.JsonObject;
import com.veeva.vault.sdk.api.json.JsonValueType;

import java.util.Map;

@UserDefinedClassInfo
public class PicklistHttpRequests {

    public Map<String, String> NewPicklistRequest (String connectionName, String productName){

        HttpService httpService = ServiceLocator.locate(HttpService.class);
        LogService logService = ServiceLocator.locate(LogService.class);
        FormHttpRequest request = httpService.newHttpRequestBuilder()
                .withConnectionName(connectionName)
                .withMethod(HttpMethod.POST)
                .withPath("api/v25.1/objects/picklists/crm_product__c")
                .withHeader("Content-Type", "application/x-www-form-urlencoded")
                .withHeader("Accept", "application/json")
                .withBody("value_1="+productName)
                .build();

        Map <String,String> SuccessMessage = VaultCollections.newMap();
        Map <String,String> errorMessage = VaultCollections.newMap();
        httpService.sendRequest(request, HttpResponseBodyValueType.JSONDATA)
                .onSuccess(httpResponse -> {

                    JsonData response = httpResponse.getResponseBody();
                    logService.info("response: " + response.toString());
                    if (response.isValidJson()) {
                        String responseStatus = response.getJsonObject().getValue("responseStatus", JsonValueType.STRING);
                        String responseMessage = response.getJsonObject().getValue("responseMessage", JsonValueType.STRING);
                        logService.info("responseStatus: " + responseStatus);
                        logService.info("responseMessage: " + responseMessage);

                        if (responseStatus.equals("SUCCESS")) {

                            JsonArray data = response.getJsonObject().getValue("picklistValues", JsonValueType.ARRAY);
                            logService.info("HTTP Query Request: SUCCESS");

                            //Retrieve each record returned from the VQL query.
                            //Each element of the returned `data` JsonArray is a record with it's queried fields.
                            for (int i = 0; i < data.getSize();i++) {
                                JsonObject objectRecord = data.getValue(i, JsonValueType.OBJECT);
                                String picklistValue = objectRecord.getValue("name", JsonValueType.STRING);
                                logService.info("picklistValue: " + picklistValue);
                                SuccessMessage.put(responseStatus, picklistValue);
                            }

                        }

                    }
                    else {
                        logService.info("getIntTransIds error: Received a non-JSON response.");
                    }
                })
                .onError(httpOperationError -> {
                    errorMessage.put("Error",httpOperationError.getHttpResponse().getResponseBody());
                    logService.info("getIntTransIds error: httpOperationError.");
                }).execute();


        if(!SuccessMessage.isEmpty()){ return SuccessMessage;}else {return errorMessage;}

    }

    public Map<String, String> ViewPicklistValueRequest (String connectionName, String productName){

        HttpService httpService = ServiceLocator.locate(HttpService.class);
        LogService logService = ServiceLocator.locate(LogService.class);
        FormHttpRequest request = httpService.newHttpRequestBuilder()
                .withConnectionName(connectionName)
                .withMethod(HttpMethod.GET)
                .withPath("api/v25.1/objects/picklists/crm_product__c")
                .withHeader("Content-Type", "application/x-www-form-urlencoded")
                .withHeader("Accept", "application/json")
                .build();

        Map <String,String> SuccessMessage = VaultCollections.newMap();
        Map <String,String> errorMessage = VaultCollections.newMap();
        httpService.sendRequest(request, HttpResponseBodyValueType.JSONDATA)
                .onSuccess(httpResponse -> {

                    JsonData response = httpResponse.getResponseBody();
                    logService.info("response: " + response.toString());
                    if (response.isValidJson()) {
                        String responseStatus = response.getJsonObject().getValue("responseStatus", JsonValueType.STRING);
                        String responseMessage = response.getJsonObject().getValue("responseMessage", JsonValueType.STRING);
                        logService.info("responseStatus: " + responseStatus);
                        logService.info("responseMessage: " + responseMessage);

                        if (responseStatus.equals("SUCCESS")) {

                            JsonArray data = response.getJsonObject().getValue("picklistValues", JsonValueType.ARRAY);
                            logService.info("HTTP Query Request: SUCCESS");

                            //Retrieve each record returned from the VQL query.
                            //Each element of the returned `data` JsonArray is a record with it's queried fields.
                            for (int i = 0; i < data.getSize();i++) {
                                JsonObject objectRecord = data.getValue(i, JsonValueType.OBJECT);
                                String picklistValue = objectRecord.getValue("name", JsonValueType.STRING);
                                String picklistLabel = objectRecord.getValue("label", JsonValueType.STRING);
                                logService.info("picklistValue: " + picklistLabel);
                                SuccessMessage.put(picklistLabel,picklistValue);
                            }

                        }

                    }
                    else {
                        logService.info("getIntTransIds error: Received a non-JSON response.");
                    }
                })
                .onError(httpOperationError -> {
                    errorMessage.put("Error",httpOperationError.getHttpResponse().getResponseBody());
                    logService.info("getIntTransIds error: httpOperationError.");
                }).execute();


        if(!SuccessMessage.isEmpty()){ return SuccessMessage;}else {return errorMessage;}

    }
}