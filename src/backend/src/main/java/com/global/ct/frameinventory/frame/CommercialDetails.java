package com.global.ct.frameinventory.frame;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class CommercialDetails {

    @Column(name = "impact_weight", precision = 12, scale = 4)
    private BigDecimal impactWeight;

    @Column(name = "production_rate_card", length = 100)
    private String productionRateCard;

    @Column(name = "production_rate_card_legacy", length = 100)
    private String legacyProductionRateCard;

    @Column(name = "pricing_grade", length = 50)
    private String pricingGrade;

    @Column(name = "price_entity_id", length = 100)
    private String priceEntityId;

    @Column(name = "premium")
    private Boolean premium;

    protected CommercialDetails() {
    }

    public CommercialDetails(
        BigDecimal impactWeight,
        String productionRateCard,
        String legacyProductionRateCard,
        String pricingGrade,
        String priceEntityId,
        Boolean premium
    ) {
        this.impactWeight = impactWeight;
        this.productionRateCard = productionRateCard;
        this.legacyProductionRateCard = legacyProductionRateCard;
        this.pricingGrade = pricingGrade;
        this.priceEntityId = priceEntityId;
        this.premium = premium;
    }

    public BigDecimal getImpactWeight() {
        return impactWeight;
    }

    public String getProductionRateCard() {
        return productionRateCard;
    }

    public String getLegacyProductionRateCard() {
        return legacyProductionRateCard;
    }

    public String getPricingGrade() {
        return pricingGrade;
    }

    public String getPriceEntityId() {
        return priceEntityId;
    }

    public Boolean getPremium() {
        return premium;
    }
}
