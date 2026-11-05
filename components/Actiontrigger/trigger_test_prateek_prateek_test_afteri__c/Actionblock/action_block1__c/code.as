IF
TRUE
THEN
UpdateRecords(GetRecords($integration_event__c, "processed_flag__c = 'NO'"), {$processed_flag__c: 'YES'});