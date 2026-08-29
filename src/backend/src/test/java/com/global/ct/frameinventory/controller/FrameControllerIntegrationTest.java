package com.global.ct.frameinventory.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.global.ct.frameinventory.repository.FrameRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class FrameControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private FrameRepository repository;

    @BeforeEach
    void clearFrames() {
        repository.deleteAll();
    }

    @Test
    void createsFrameWithTrimmedIdAndReturnsCompleteRepresentation() throws Exception {
        mockMvc.perform(post("/api/frames")
                .contentType(MediaType.APPLICATION_JSON)
                .content(createRequest("  frame 100  ", "LIVE", "DIGITAL", "UNDERGROUND", "D6",
                    "London", "Green Park", "Green Park Station")))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", endsWith("/api/frames/frame%20100")))
            .andExpect(jsonPath("$.frameId").value("frame 100"))
            .andExpect(jsonPath("$.createdDate", endsWith("Z")))
            .andExpect(jsonPath("$.modifiedDate", endsWith("Z")))
            .andExpect(jsonPath("$.location.region").value("London"))
            .andExpect(jsonPath("$.site.station").value("Green Park"))
            .andExpect(jsonPath("$.technical.numberOfSlots").value(6))
            .andExpect(jsonPath("$.commercial.premium").value(false))
            .andExpect(jsonPath("$.integrations.broadsignFrameId").value("BS-frame 100"));

        mockMvc.perform(get("/api/frames/{frameId}", "frame 100"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.frameId").value("frame 100"))
            .andExpect(jsonPath("$.location.address").value("Green Park Station"))
            .andExpect(jsonPath("$.location.latitude").value(51.50604991))
            .andExpect(jsonPath("$.site.siteNumber").value("SITE-1"));
    }

    @Test
    void rejectsDuplicateFrameId() throws Exception {
        String request = createRequest(
            "duplicate", "LIVE", "DIGITAL", "UNDERGROUND", "D6", "London", "Station", "Address"
        );
        create(request);

        mockMvc.perform(post("/api/frames")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.title").value("Frame already exists"))
            .andExpect(jsonPath("$.detail").value("Frame 'duplicate' already exists"));
    }

    @Test
    void validatesRequiredApiFields() throws Exception {
        mockMvc.perform(post("/api/frames")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "frameId": "   ",
                      "mediaType": "DIGITAL",
                      "format": "D6",
                      "status": "LIVE",
                      "location": {
                        "postcode": "W1J 9DZ",
                        "region": "London",
                        "town": "London West End"
                      },
                      "site": {
                        "siteNumber": "SITE-1"
                      }
                    }
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.title").value("Validation failed"))
            .andExpect(jsonPath("$.errors[0].field").value("frameId"));
    }

    @Test
    void allowsEnvironmentAndNonRequiredDetailsToBeOmitted() throws Exception {
        mockMvc.perform(post("/api/frames")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "frameId": "minimal",
                      "mediaType": "DIGITAL",
                      "format": "D6",
                      "status": "LIVE",
                      "location": {
                        "postcode": "W1J 9DZ",
                        "region": "London",
                        "town": "London West End"
                      },
                      "site": {
                        "siteNumber": "SITE-1"
                      }
                    }
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.environment").doesNotExist())
            .andExpect(jsonPath("$.location.address").doesNotExist())
            .andExpect(jsonPath("$.site.station").doesNotExist());
    }

    @Test
    void reportsNonDuplicatePersistenceFailuresWithoutCallingThemDuplicates() throws Exception {
        mockMvc.perform(post("/api/frames")
                .contentType(MediaType.APPLICATION_JSON)
                .content(createRequest(
                    "too-wide", "LIVE", "D".repeat(33), "UNDERGROUND", "D6",
                    "London", "Station", "Address"
                )))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.title").value("Frame data could not be persisted"))
            .andExpect(jsonPath("$.detail").value("Frame data violates a persistence constraint"));
    }

    @Test
    void returnsDecimalValuesAsActuallyPersistedWithoutApiPrecisionValidation() throws Exception {
        String request = createRequest(
            "decimal-scale", "LIVE", "DIGITAL", "UNDERGROUND", "D6",
            "London", "Station", "Address"
        )
            .replace("-0.14174505", "-0.141745059")
            .replace("51.50604991", "51.506049919")
            .replace("0.6027", "0.60271");

        MvcResult created = create(request);
        JsonNode createdBody = objectMapper.readTree(created.getResponse().getContentAsString());
        MvcResult loaded = mockMvc.perform(get("/api/frames/{frameId}", "decimal-scale"))
            .andExpect(status().isOk())
            .andReturn();
        JsonNode loadedBody = objectMapper.readTree(loaded.getResponse().getContentAsString());

        assertThat(createdBody.get("location").get("longitude"))
            .isEqualTo(loadedBody.get("location").get("longitude"));
        assertThat(createdBody.get("location").get("latitude"))
            .isEqualTo(loadedBody.get("location").get("latitude"));
        assertThat(createdBody.get("commercial").get("impactWeight"))
            .isEqualTo(loadedBody.get("commercial").get("impactWeight"));
    }

    @Test
    void putFullyReplacesWritableFieldsAndPreservesIdentityAndCreationDate() throws Exception {
        MvcResult created = create(createRequest(
            "replace-me", "LIVE", "DIGITAL", "UNDERGROUND", "D6", "London", "Old Station", "Old Address"
        ));
        JsonNode createdBody = objectMapper.readTree(created.getResponse().getContentAsString());
        String createdDate = createdBody.get("createdDate").asText();

        mockMvc.perform(put("/api/frames/{frameId}", "replace-me")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "mediaType": "CLASSIC",
                      "format": "6_SHEET",
                      "status": "INACTIVE",
                      "statusReason": "Maintenance",
                      "location": {
                        "postcode": "M1 1AA",
                        "address": "New Address",
                        "region": "North West",
                        "town": "Manchester"
                      },
                      "site": {
                        "siteNumber": "SITE-2"
                      }
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.frameId").value("replace-me"))
            .andExpect(jsonPath("$.createdDate").value(createdDate))
            .andExpect(jsonPath("$.status").value("INACTIVE"))
            .andExpect(jsonPath("$.environment").doesNotExist())
            .andExpect(jsonPath("$.location.town").value("Manchester"))
            .andExpect(jsonPath("$.site.siteNumber").value("SITE-2"))
            .andExpect(jsonPath("$.site.station").doesNotExist())
            .andExpect(jsonPath("$.technical").doesNotExist())
            .andExpect(jsonPath("$.commercial").doesNotExist())
            .andExpect(jsonPath("$.integrations").doesNotExist());

        mockMvc.perform(get("/api/frames/{frameId}", "replace-me"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.statusReason").value("Maintenance"))
            .andExpect(jsonPath("$.site.siteNumber").value("SITE-2"));
    }

    @Test
    void updateRejectsFrameIdInRequestBody() throws Exception {
        create(createRequest(
            "immutable", "LIVE", "DIGITAL", "UNDERGROUND", "D6", "London", "Station", "Address"
        ));

        mockMvc.perform(put("/api/frames/{frameId}", "immutable")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "frameId": "different",
                      "mediaType": "DIGITAL",
                      "format": "D6",
                      "status": "LIVE",
                      "location": {
                        "postcode": "W1J 9DZ",
                        "region": "London",
                        "town": "London West End"
                      },
                      "site": {
                        "siteNumber": "SITE-1"
                      }
                    }
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.title").value("Invalid request body"));
    }

    @Test
    void returnsNotFoundForMissingFrameReadsAndUpdates() throws Exception {
        mockMvc.perform(get("/api/frames/{frameId}", "missing"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.title").value("Frame not found"));

        mockMvc.perform(put("/api/frames/{frameId}", "missing")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "mediaType": "DIGITAL",
                      "format": "D6",
                      "status": "LIVE",
                      "location": {
                        "postcode": "W1J 9DZ",
                        "region": "London",
                        "town": "London West End"
                      },
                      "site": {
                        "siteNumber": "SITE-1"
                      }
                    }
                    """))
            .andExpect(status().isNotFound());
    }

    @Test
    void searchesWithApprovedFiltersAndReturnsOnlySummaryFields() throws Exception {
        create(createRequest(
            "target", "LIVE", "DIGITAL", "UNDERGROUND", "D6", "London", "Green Park", "Platform"
        ));
        create(createRequest(
            "other", "LIVE", "DIGITAL", "UNDERGROUND", "D6", "Scotland", "Green Park", "Platform"
        ));
        create(createRequest(
            "inactive", "INACTIVE", "DIGITAL", "UNDERGROUND", "D6", "London", "Green Park", "Platform"
        ));

        mockMvc.perform(get("/api/frames")
                .param("q", "green")
                .param("status", "live")
                .param("mediaType", "digital")
                .param("environment", "underground")
                .param("format", "d6")
                .param("region", "london"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items", hasSize(1)))
            .andExpect(jsonPath("$.items[0].frameId").value("target"))
            .andExpect(jsonPath("$.items[0].address").value("Platform"))
            .andExpect(jsonPath("$.items[0].station").value("Green Park"))
            .andExpect(jsonPath("$.items[0].location").doesNotExist())
            .andExpect(jsonPath("$.items[0].createdDate").doesNotExist())
            .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void paginatesWithStableSortingAndRejectsInvalidPageOptions() throws Exception {
        create(createRequest(
            "frame-b", "LIVE", "DIGITAL", "UNDERGROUND", "D6", "London", "Station", "Address"
        ));
        create(createRequest(
            "frame-a", "LIVE", "DIGITAL", "UNDERGROUND", "D6", "London", "Station", "Address"
        ));

        mockMvc.perform(get("/api/frames")
                .param("page", "1")
                .param("size", "1")
                .param("sort", "frameId,asc"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items[0].frameId").value("frame-b"))
            .andExpect(jsonPath("$.page").value(1))
            .andExpect(jsonPath("$.size").value(1))
            .andExpect(jsonPath("$.totalElements").value(2))
            .andExpect(jsonPath("$.totalPages").value(2));

        mockMvc.perform(get("/api/frames").param("size", "101"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detail").value("size must be between 1 and 100"));

        mockMvc.perform(get("/api/frames").param("sort", "town,asc"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.title").value("Invalid request"));
    }

    private MvcResult create(String request) throws Exception {
        return mockMvc.perform(post("/api/frames")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
            .andExpect(status().isCreated())
            .andReturn();
    }

    private String createRequest(
        String frameId,
        String status,
        String mediaType,
        String environment,
        String format,
        String region,
        String station,
        String address
    ) {
        return """
            {
              "frameId": "%s",
              "mediaType": "%s",
              "format": "%s",
              "environment": "%s",
              "status": "%s",
              "location": {
                "postcode": "W1J 9DZ",
                "address": "%s",
                "region": "%s",
                "countryCode": "UK",
                "town": "London West End",
                "longitude": -0.14174505,
                "latitude": 51.50604991,
                "distanceToClosestSchool": 637
              },
              "site": {
                "siteNumber": "SITE-1",
                "panelNumber": "01",
                "station": "%s"
              },
              "technical": {
                "numberOfSlots": 6,
                "sizeCode": "D6",
                "pixelHeight": 1200,
                "pixelWidth": 1920
              },
              "commercial": {
                "impactWeight": 0.6027,
                "premium": false
              },
              "integrations": {
                "broadsignFrameId": "BS-%s"
              }
            }
            """.formatted(
                frameId, mediaType, format, environment, status,
                address, region, station, frameId.trim()
            );
    }
}
