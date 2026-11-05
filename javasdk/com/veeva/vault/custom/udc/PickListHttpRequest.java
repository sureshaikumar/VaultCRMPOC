package com.veeva.vault.custom.udc;

import java.util.Map;

import com.veeva.vault.sdk.api.core.LogService;
import com.veeva.vault.sdk.api.core.ServiceLocator;
import com.veeva.vault.sdk.api.core.UserDefinedClassInfo;
import com.veeva.vault.sdk.api.core.VaultCollections;
import com.veeva.vault.sdk.api.http.FormHttpRequest;
import com.veeva.vault.sdk.api.http.HttpMethod;
import com.veeva.vault.sdk.api.http.HttpResponseBodyValueType;
import com.veeva.vault.sdk.api.http.HttpService;
import com.veeva.vault.sdk.api.json.JsonData;
import com.veeva.vault.sdk.api.json.JsonValueType;

@UserDefinedClassInfo
public class PickListHttpRequest {

        // =====================================================
        // CASE 1: CREATE NEW PICKLIST VALUE
        // =====================================================

        public Map<String, String> newPicklistRequest(
                        String connectionName,
                        String productName) {

                HttpService httpService = ServiceLocator.locate(
                                HttpService.class);

                LogService logService = ServiceLocator.locate(
                                LogService.class);

                FormHttpRequest request = httpService
                                .newHttpRequestBuilder()

                                .withConnectionName(
                                                connectionName)

                                .withMethod(
                                                HttpMethod.POST)

                                .withPath(
                                                "api/v25.1/objects/picklists/crm_product__c")

                                .withHeader(
                                                "Content-Type",
                                                "application/x-www-form-urlencoded")

                                .withHeader(
                                                "Accept",
                                                "application/json")

                                .withBody(
                                                "value_1=" + productName)

                                .build();

                Map<String, String> result = VaultCollections.newMap();

                httpService
                                .sendRequest(
                                                request,
                                                HttpResponseBodyValueType.JSONDATA)

                                .onSuccess(httpResponse -> {

                                        JsonData response = httpResponse
                                                        .getResponseBody();

                                        if (response.isValidJson()) {

                                                String status = response
                                                                .getJsonObject()
                                                                .getValue(
                                                                                "responseStatus",
                                                                                JsonValueType.STRING);

                                                logService.info(
                                                                "New Product Response Status: {}",
                                                                status);

                                                result.put(
                                                                "STATUS",
                                                                status);
                                        }
                                })

                                .onError(error -> {

                                        logService.error(
                                                        "New Product Request Failed: {}",
                                                        error);

                                        result.put(
                                                        "STATUS",
                                                        "FAILED");
                                })

                                .execute();

                return result;
        }

        // =====================================================
        // CASE 2 + CASE 3
        //
        // UPDATE PICKLIST LABEL
        // UPDATE PICKLIST NAME
        // =====================================================

        public Map<String, String> updatePicklistRequest(
                        String connectionName,
                        String existingProductName,
                        String newValue,
                        String requestType) {

                HttpService httpService = ServiceLocator.locate(
                                HttpService.class);

                LogService logService = ServiceLocator.locate(
                                LogService.class);

                String path;
                String body;

                // =================================================
                // UPDATE LABEL
                // =================================================

                if ("update_picklist_label__c"
                                .equals(requestType)) {

                        path = "api/v25.1/objects/picklists/"
                                        + "crm_product__c";

                        body = existingProductName
                                        + "="
                                        + newValue;

                        logService.info(
                                        "Updating Picklist Label");

                        logService.info(
                                        "Existing Name: {}",
                                        existingProductName);

                        logService.info(
                                        "New Label: {}",
                                        newValue);
                }

                // =================================================
                // UPDATE NAME
                // =================================================

                else if ("update_picklist_name__c"
                                .equals(requestType)) {

                        path = "api/v25.1/objects/picklists/"
                                        + "crm_product__c/"
                                        + existingProductName;

                        body = "name="
                                        + newValue;

                        logService.info(
                                        "Updating Picklist Name");

                        logService.info(
                                        "Existing Name: {}",
                                        existingProductName);

                        logService.info(
                                        "New Name: {}",
                                        newValue);
                } else if ("inactive_picklist_value__c".equals(requestType)) {

                        path = "api/v25.1/objects/picklists/"
                                        + "crm_product__c/"
                                        + existingProductName;

                        body = "status=inactive";
                        logService.info("Inactivating Picklist Value");

                        logService.info("Existing Name: {}",
                                        existingProductName);
                }

                // =================================================
                // INVALID REQUEST TYPE
                // =================================================

                else {

                        Map<String, String> result = VaultCollections.newMap();

                        result.put(
                                        "STATUS",
                                        "FAILED");

                        return result;
                }

                // =================================================
                // CREATE HTTP REQUEST
                // =================================================

                FormHttpRequest request = httpService
                                .newHttpRequestBuilder()
                                .withConnectionName(connectionName)
                                .withMethod(HttpMethod.PUT)
                                .withPath(path)
                                .withHeader( "Content-Type","application/x-www-form-urlencoded")
                                .withHeader("Accept", "application/json")
                                .withBody(body)
                                .build();

                Map<String, String> result = VaultCollections.newMap();

                // =================================================
                // SEND REQUEST
                // =================================================

                httpService.sendRequest(request,HttpResponseBodyValueType.JSONDATA).onSuccess(httpResponse -> {JsonData response = httpResponse.getResponseBody();
                                if (response.isValidJson()) {
                                String status = response.getJsonObject().getValue(
                                                                                "responseStatus",
                                                                                JsonValueType.STRING);

                logService.info("Update Picklist Response Status: {}", status);

                result.put("STATUS", status);
                                        }
                                })

                                .onError(error -> {

                                        logService.error(
                                                        "Update Picklist Request Failed: {}",
                                                        error);

                                        result.put(
                                                        "STATUS",
                                                        "FAILED");
                                })

                                .execute();

                return result;
        }
}