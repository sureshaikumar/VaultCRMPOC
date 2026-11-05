package com.veeva.vault.custom.uds;

import com.veeva.vault.sdk.api.core.UserDefinedServiceInfo;
import java.time.DayOfWeek;
import java.time.LocalDate;

/**
 * Implementation of the Call Days Calculator Service.
 * Contains the logic to loop through dates and skip weekends.
 */
@UserDefinedServiceInfo
public class CallDaysCalculatorServiceImpl implements CallDaysCalculatorService {

    public long calculateBusinessDays(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null || endDate.isBefore(startDate)) {
            return 0;
        }

        long businessDays = 0;
        LocalDate current = startDate;

        // Loop through every day from Start to End
        while (!current.isAfter(endDate)) {
            DayOfWeek day = current.getDayOfWeek();

            // If it is NOT Saturday (6) and NOT Sunday (7), count it
            if (day != DayOfWeek.SATURDAY && day != DayOfWeek.SUNDAY) {
                businessDays++;
            }

            // Move to next day
            current = current.plusDays(1);
        }

        return businessDays;
    }
}