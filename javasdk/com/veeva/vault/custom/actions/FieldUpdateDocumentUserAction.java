package com.veeva.vault.custom.actions;

import com.veeva.vault.custom.udc.udcCountryAndProductQuery;
import com.veeva.vault.sdk.api.action.*;
import com.veeva.vault.sdk.api.core.*;
import com.veeva.vault.sdk.api.document.*;

import java.util.List;

@DocumentActionInfo(label = "Field Update UserAction", usages = {Usage.USER_ACTION, Usage.LIFECYCLE_ENTRY_ACTION})
public class FieldUpdateDocumentUserAction implements DocumentAction {

    public void execute(DocumentActionContext documentActionContext) {

        LogService logService = ServiceLocator.locate(LogService.class);
        DocumentService docService = ServiceLocator.locate(DocumentService.class);

        DocumentVersion docVersion = documentActionContext.getDocumentVersions().get(0);
        String documentName = docVersion.getValue("name__v", ValueType.STRING);
        Number majorVersion = docVersion.getValue("major_version_number__v", ValueType.NUMBER);
        Number minorVersion = docVersion.getValue("minor_version_number__v", ValueType.NUMBER);
        String productId = docVersion.getValue("product__v",ValueType.REFERENCES).get(0);
        udcCountryAndProductQuery productQuery = new udcCountryAndProductQuery();

        String ProductName = productQuery.getProductName(productId);

        if (documentName != null && documentName.contains("CRM")) {
            // Format link__sys field
            String formattedLink = documentName + " v(" + majorVersion.toString() + "." + minorVersion.toString() + ")";
            docVersion.setValue("link__sys", formattedLink+" And Product Name: "+ ProductName);

            List<DocumentVersion> docVersionList = VaultCollections.newList();
            docVersionList.add(docVersion);

            SaveDocumentVersionsResponse saveResponse = docService.saveDocumentVersions(docVersionList);

            if (!saveResponse.getUnprocessed().isEmpty()) {
                PositionalDocumentVersionId unprocessedId = saveResponse.getUnprocessed().get(0);
                logService.info("Unprocessed DocumentVersion: InputPosition=" + unprocessedId.getInputPosition() +
                        ", DocumentVersionId=" + unprocessedId.getDocumentVersionId());
            } else if (!saveResponse.getSuccesses().isEmpty()) {
                PositionalDocumentVersionId successId = saveResponse.getSuccesses().get(0);
                logService.info("Document successfully updated: InputPosition=" + successId.getInputPosition() +
                        ", DocumentVersionId=" + successId.getDocumentVersionId());
            } else {
                logService.warn("No results returned from saveDocumentVersions().");
            }
        } else {
            logService.info("Skipped: Document name does not contain 'CRM'.");
        }
    }

    public void onPreExecute(PreExecutionDocumentActionUIContext context) {
        UserActionUIService userActionUIService = ServiceLocator.locate(UserActionUIService.class);

        String dialogMessage = "Document link will be updated with the document name and current version.";
        String dialogTitle = "Document Update Notice";

        UserActionExecutionDialog dialog = userActionUIService.newUserActionExecutionDialogBuilder()
                .withMessage(dialogMessage)
                .withTitle(dialogTitle)
                .build();

        PreExecutionUserActionUIBehavior preExecBehavior = userActionUIService.newPreExecutionUserActionUIBehaviorBuilder()
                .withDialog(dialog)
                .build();

        context.setUserActionUIBehavior(preExecBehavior);
    }

    public boolean isExecutable(DocumentActionContext context) {
        return true;
    }
}