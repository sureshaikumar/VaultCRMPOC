package com.veeva.vault.custom.actions;

import com.veeva.vault.custom.udc.PicklistHttpRequests;
import com.veeva.vault.sdk.api.action.*;
import com.veeva.vault.sdk.api.core.*;
import com.veeva.vault.sdk.api.data.Record;
import com.veeva.vault.sdk.api.data.RecordBatchSaveRequest;
import com.veeva.vault.sdk.api.data.RecordService;

import java.util.List;
import java.util.Map;

@RecordActionInfo(label= "New CRM Product Creation", object = "test_object__c", usages = {Usage.USER_ACTION, Usage.LIFECYCLE_ENTRY_ACTION})
public class NewProductCreationRecordAction implements RecordAction {

    public boolean isExecutable(RecordActionContext context) {
        return true;
    }

        public void execute(RecordActionContext recordActionContext) {
            List<Record> records = recordActionContext.getRecords();

            Record record = records.get(0);
            String productName = record.getValue("product_name__c", ValueType.STRING);

            PicklistHttpRequests picklistHttpRequests = new PicklistHttpRequests();

            if (productName != null) {

                Map<String, String> viewProductPicklist = picklistHttpRequests.ViewPicklistValueRequest("crm_training_connection",productName);
                if(!viewProductPicklist.containsKey(productName))
                {
                    String currentUserId = RequestContext.get().getInitiatingUserId();
                    record.setValue("product_requestor__c", currentUserId);

                    Map<String, String> picklistAndStatus = picklistHttpRequests.NewPicklistRequest("crm_training_connection", productName);
                    String ProductName = picklistAndStatus.get("SUCCESS");
                    String ProductErrorStatus = picklistAndStatus.get("Error");
                    if (picklistAndStatus.containsKey("SUCCESS"))
                        {
                            record.setValue("product_create_status__c", "SUCCESS");
                            record.setValue("product_name__c", ProductName);
                        } else {
                            record.setValue("product_create_status__c", ProductErrorStatus);
                        }
                }else {record.setValue("product_create_status__c", "Picklist Label Already Exists");}
            } else {
                record.setValue("product_create_status__c", "Error: Picklist Value Missing");
            }
                    RecordService recordService = ServiceLocator.locate(RecordService.class);

                    RecordBatchSaveRequest saveRequest = recordService.newRecordBatchSaveRequestBuilder().withRecords(records).build();

                    recordService.batchSaveRecords(saveRequest)
                            .onErrors(batchOperationErrors -> {
                                batchOperationErrors.stream().findFirst().ifPresent(error -> {
                                    String errMsg = error.getError().getMessage();
                                    int errPosition = error.getInputPosition();
                                    String name = records.get(errPosition).getValue("name__v", ValueType.STRING);
                                    throw new RollbackException("OPERATION_NOT_ALLOWED", "Unable to create: " + name +
                                            "because of " + errMsg);
                                });
                            })
                            .execute();
        }

    public void onPostExecute(PostExecutionRecordActionUIContext context) {
        // Get the UserActionUIService
        UserActionUIService userActionUIService = ServiceLocator.locate(UserActionUIService.class);

        // Build a UserActionExecutionBanner with a message
        String bannerMessage = "Record has been updated... Please refresh the page";
        UserActionExecutionBanner banner = userActionUIService.newUserActionExecutionBannerBuilder()
                .withMessage(bannerMessage)
                .build();

        // Build a PostExecutionUserActionUIBehavior with the banner
        PostExecutionUserActionUIBehavior postExecBehavior = userActionUIService.newPostExecutionUserActionUIBehaviorBuilder()
                .withBanner(banner)
                .build();

        // Set the post-execution user action UI behavior on the context
        context.setUserActionUIBehavior(postExecBehavior);
    }
}