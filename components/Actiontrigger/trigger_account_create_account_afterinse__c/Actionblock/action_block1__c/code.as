IF
TRUE
THEN
UpdateRecords(GetRecords($integration_event__c, "status__v = 'Active'"), {$processed_flag__c: "YES"});