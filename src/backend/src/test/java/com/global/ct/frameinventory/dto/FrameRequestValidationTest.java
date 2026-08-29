package com.global.ct.frameinventory.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import jakarta.validation.Validation;
import jakarta.validation.Validator;

class FrameRequestValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void optionalLengthsAndDecimalPrecisionAreNotBeanValidationRules() {
        CreateFrameRequest request = new CreateFrameRequest(
            "frame-1",
            "DIGITAL",
            "D6",
            "UNDERGROUND",
            "LIVE",
            "x".repeat(501),
            new LocationDto(
                "W1J 9DZ", null, null, null, null, "x".repeat(1001), "London", null,
                "London West End",
                new BigDecimal("-0.123456789"), new BigDecimal("51.123456789"),
                null, null, null
            ),
            new SiteDto("SITE-1", null, null, null, null),
            null,
            new CommercialDto(new BigDecimal("1.12345"), null, null, null, null, null),
            null
        );

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void frameIdStillHasTheApprovedMaximumLength() {
        CreateFrameRequest request = new CreateFrameRequest(
            "x".repeat(65), "DIGITAL", "D6", "UNDERGROUND", "LIVE", null,
            requiredLocation(), requiredSite(), null, null, null
        );

        assertThat(validator.validate(request))
            .extracting(violation -> violation.getPropertyPath().toString())
            .containsExactly("frameId");
    }

    @Test
    void requiredFieldsMatchTheApprovedApiContract() {
        CreateFrameRequest request = new CreateFrameRequest(
            " ", " ", " ", null, " ", null,
            new LocationDto(" ", null, null, null, null, null, " ", null, " ",
                null, null, null, null, null),
            new SiteDto(" ", null, null, null, null),
            null, null, null
        );

        assertThat(validator.validate(request))
            .extracting(violation -> violation.getPropertyPath().toString())
            .containsExactlyInAnyOrder(
                "frameId", "mediaType", "format", "status",
                "location.postcode", "location.region", "location.town", "site.siteNumber"
            );
    }

    @Test
    void requiredNestedGroupsMustBePresent() {
        CreateFrameRequest request = new CreateFrameRequest(
            "frame-1", "DIGITAL", "D6", null, "LIVE", null,
            null, null, null, null, null
        );

        assertThat(validator.validate(request))
            .extracting(violation -> violation.getPropertyPath().toString())
            .containsExactlyInAnyOrder("location", "site");
    }

    private LocationDto requiredLocation() {
        return new LocationDto(
            "W1J 9DZ", null, null, null, null, null, "London", null, "London West End",
            null, null, null, null, null
        );
    }

    private SiteDto requiredSite() {
        return new SiteDto("SITE-1", null, null, null, null);
    }
}
