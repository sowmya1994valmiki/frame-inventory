package com.global.ct.frameinventory.frame;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import jakarta.persistence.EntityManager;

@DataJpaTest
class FrameRepositoryPersistenceTest {

    @Autowired
    private FrameRepository frameRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void persistsAndLoadsFrameAndAllEmbeddedGroups() {
        Frame frame = completeFrame("1234604983");

        frameRepository.saveAndFlush(frame);
        entityManager.clear();

        Frame loaded = frameRepository.findById("1234604983").orElseThrow();

        assertThat(loaded.getFrameId()).isEqualTo("1234604983");
        assertThat(loaded)
            .extracting(
                Frame::getMediaType,
                Frame::getFormat,
                Frame::getEnvironment,
                Frame::getStatus,
                Frame::getStatusReason,
                Frame::getCreatedDate,
                Frame::getModifiedDate
            )
            .containsExactly(
                "DIGITAL",
                "DIGITAL_CROSSTRACK_48_SHEETS",
                "UNDERGROUND",
                "LIVE",
                null,
                LocalDateTime.of(2022, 10, 1, 16, 26, 27),
                LocalDateTime.of(2026, 5, 11, 22, 18, 5)
            );
        assertThat(loaded.getLocation())
            .usingRecursiveComparison()
            .isEqualTo(frame.getLocation());
        assertThat(loaded.getSite())
            .usingRecursiveComparison()
            .isEqualTo(frame.getSite());
        assertThat(loaded.getTechnical())
            .usingRecursiveComparison()
            .isEqualTo(frame.getTechnical());
        assertThat(loaded.getCommercial())
            .usingRecursiveComparison()
            .isEqualTo(frame.getCommercial());
        assertThat(loaded.getIntegrations())
            .usingRecursiveComparison()
            .isEqualTo(frame.getIntegrations());
    }

    @Test
    void usesFrameIdAsTheJpaIdentifier() {
        frameRepository.saveAndFlush(completeFrame("2000100023"));

        assertThat(frameRepository.existsById("2000100023")).isTrue();
        assertThat(frameRepository.findById("2000100023"))
            .get()
            .extracting(Frame::getFrameId)
            .isEqualTo("2000100023");
    }

    static Frame completeFrame(String frameId) {
        LocationDetails location = new LocationDetails(
            "W1J 9DZ",
            "W",
            "1J",
            "9",
            "DZ",
            "VICTORIA LINE PLATFORM 4 SOUTHBOUND",
            "London",
            "UK",
            "London West End",
            new BigDecimal("-0.14174505"),
            new BigDecimal("51.50604991"),
            637,
            "POINT (51.50604991 -0.14174505)",
            "w1j9dz"
        );
        SiteDetails site = new SiteDetails(
            "GRPX04033",
            "3776",
            "01",
            "Green Park",
            null
        );
        TechnicalDetails technical = new TechnicalDetails(
            "1",
            6,
            "DX48",
            "DX48",
            "DX48",
            "LARGE",
            1200,
            1920
        );
        CommercialDetails commercial = new CommercialDetails(
            new BigDecimal("0.6027"),
            null,
            null,
            "PREMIUM",
            "LUDX48",
            false
        );
        IntegrationDetails integrations = new IntegrationDetails(
            "62570424",
            "57805710",
            "5123640",
            "2000100023,2000100024"
        );

        return new Frame(
            frameId,
            "DIGITAL",
            "DIGITAL_CROSSTRACK_48_SHEETS",
            "UNDERGROUND",
            "LIVE",
            null,
            LocalDateTime.of(2022, 10, 1, 16, 26, 27),
            LocalDateTime.of(2026, 5, 11, 22, 18, 5),
            location,
            site,
            technical,
            commercial,
            integrations
        );
    }
}
