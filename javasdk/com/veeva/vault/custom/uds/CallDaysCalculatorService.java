package com.veeva.vault.custom.uds;

import com.veeva.vault.sdk.api.core.UserDefinedService;
import com.veeva.vault.sdk.api.core.UserDefinedServiceInfo;
import java.time.LocalDate;

/**
 * Interface definition for the Call Days Calculator Service.
 * This extends UserDefinedService so Vault knows it is a custom service.
 */
@UserDefinedServiceInfo
public interface CallDaysCalculatorService extends UserDefinedService {

    // Calculates business days between two dates (excluding weekends)
    long calculateBusinessDays(LocalDate startDate, LocalDate endDate);
}