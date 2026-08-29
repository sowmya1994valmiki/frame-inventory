package com.global.ct.frameinventory.dto;

import java.math.BigDecimal;

public record CommercialDto(
    BigDecimal impactWeight,
    String productionRateCard,
    String legacyProductionRateCard,
    String pricingGrade,
    String priceEntityId,
    Boolean premium
) {
}
