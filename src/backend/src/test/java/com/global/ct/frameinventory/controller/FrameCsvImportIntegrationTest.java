package com.global.ct.frameinventory.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.global.ct.frameinventory.repository.FrameHistoryRepository;
import com.global.ct.frameinventory.repository.FrameRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

@SpringBootTest
@AutoConfigureMockMvc
class FrameCsvImportIntegrationTest {

    private static final String REQUIRED_HEADERS =
        "frame_id,type_classic_digital,format,status,postcode,site_no,region,town";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private FrameRepository frameRepository;

    @MockitoSpyBean
    private FrameHistoryRepository historyRepository;

    @BeforeEach
    void clearDatabase() {
        historyRepository.deleteAll();
        frameRepository.deleteAll();
    }

    @Test
    void importsReorderedColumnsAndMapsRecognisedLegacyFields() throws Exception {
        String csv = """
            status,town,site_no,postcode,frame_id,format,region,type_classic_digital,environment,address,longitude,latitude,number_of_slots,impact_weight,premium,location,t_size,production_rate_card_legacy,broadsign_frame_id,created_date,modified_date
            LIVE,London West End,SITE-1,W1J 9DZ,csv-success,DX48,London,DIGITAL,UNDERGROUND,Platform 4,-0.14174505,51.50604991,6,0.6027,1,POINT (51.5 -0.1),LARGE,LEGACY-RATE,BS-123,2000-01-01 00:00:00,2001-01-01 00:00:00
            """;

        MvcResult result = importCsv(csv)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalRows").value(1))
            .andExpect(jsonPath("$.created").value(1))
            .andExpect(jsonPath("$.duplicates").value(0))
            .andExpect(jsonPath("$.failed").value(0))
            .andExpect(jsonPath("$.errors", hasSize(0)))
            .andReturn();

        JsonNode imported = objectMapper.readTree(
            mockMvc.perform(get("/api/frames/{frameId}", "csv-success"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.environment").value("UNDERGROUND"))
                .andExpect(jsonPath("$.location.address").value("Platform 4"))
                .andExpect(jsonPath("$.location.rawLocationPoint").value("POINT (51.5 -0.1)"))
                .andExpect(jsonPath("$.technical.numberOfSlots").value(6))
                .andExpect(jsonPath("$.technical.sizeCategory").value("LARGE"))
                .andExpect(jsonPath("$.commercial.impactWeight").value(0.6027))
                .andExpect(jsonPath("$.commercial.premium").value(true))
                .andExpect(jsonPath("$.commercial.legacyProductionRateCard").value("LEGACY-RATE"))
                .andExpect(jsonPath("$.integrations.broadsignFrameId").value("BS-123"))
                .andReturn()
                .getResponse()
                .getContentAsString()
        );
        assertThat(imported.get("createdDate").asText()).isEqualTo(imported.get("modifiedDate").asText());
        assertThat(imported.get("createdDate").asText()).doesNotStartWith("2000-");
        assertThat(result.getResponse().getContentType()).startsWith(MediaType.APPLICATION_JSON_VALUE);
    }

    @Test
    void importsValidRowsWhileReportingInvalidRows() throws Exception {
        String csv = REQUIRED_HEADERS + ",number_of_slots\n"
            + "mixed-valid,DIGITAL,D6,LIVE,W1J 9DZ,SITE-1,London,London,6\n"
            + "mixed-number,DIGITAL,D6,LIVE,W1J 9DZ,SITE-2,London,London,not-a-number\n"
            + "mixed-blank,DIGITAL,D6,LIVE,W1J 9DZ,SITE-3,,London,6\n";

        importCsv(csv)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalRows").value(3))
            .andExpect(jsonPath("$.created").value(1))
            .andExpect(jsonPath("$.duplicates").value(0))
            .andExpect(jsonPath("$.failed").value(2))
            .andExpect(jsonPath("$.errors", hasSize(2)))
            .andExpect(jsonPath("$.errors[0].rowNumber").value(3))
            .andExpect(jsonPath("$.errors[0].frameId").value("mixed-number"))
            .andExpect(jsonPath("$.errors[0].reason").value("number_of_slots must be a whole number"))
            .andExpect(jsonPath("$.errors[1].rowNumber").value(4))
            .andExpect(jsonPath("$.errors[1].frameId").value("mixed-blank"))
            .andExpect(jsonPath("$.errors[1].reason").value("location.region must not be blank"));

        assertThat(frameRepository.existsById("mixed-valid")).isTrue();
        assertThat(frameRepository.existsById("mixed-number")).isFalse();
        assertThat(frameRepository.existsById("mixed-blank")).isFalse();
    }

    @Test
    void reportsExistingFrameAsDuplicateWithoutUpdatingIt() throws Exception {
        createFrame("existing", "LIVE");
        String csv = REQUIRED_HEADERS + "\n"
            + "existing,DIGITAL,D6,INACTIVE,W1J 9DZ,SITE-1,London,London\n";

        importCsv(csv)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.created").value(0))
            .andExpect(jsonPath("$.duplicates").value(1))
            .andExpect(jsonPath("$.failed").value(0))
            .andExpect(jsonPath("$.errors[0].rowNumber").value(2))
            .andExpect(jsonPath("$.errors[0].frameId").value("existing"))
            .andExpect(jsonPath("$.errors[0].reason").value("Frame 'existing' already exists"));

        mockMvc.perform(get("/api/frames/{frameId}", "existing"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("LIVE"));
    }

    @Test
    void laterOccurrenceOfIdIsDuplicateEvenWhenFirstRowWasInvalid() throws Exception {
        String csv = REQUIRED_HEADERS + ",number_of_slots\n"
            + "repeated,DIGITAL,D6,LIVE,W1J 9DZ,SITE-1,London,London,bad\n"
            + "repeated,DIGITAL,D6,LIVE,W1J 9DZ,SITE-1,London,London,6\n";

        importCsv(csv)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalRows").value(2))
            .andExpect(jsonPath("$.created").value(0))
            .andExpect(jsonPath("$.duplicates").value(1))
            .andExpect(jsonPath("$.failed").value(1))
            .andExpect(jsonPath("$.errors[1].rowNumber").value(3))
            .andExpect(jsonPath("$.errors[1].frameId").value("repeated"))
            .andExpect(jsonPath("$.errors[1].reason").value("frameId appears more than once in the CSV"));

        assertThat(frameRepository.existsById("repeated")).isFalse();
    }

    @Test
    void laterOccurrenceOfSuccessfullyImportedIdDoesNotUpdateFirstRow() throws Exception {
        String csv = REQUIRED_HEADERS + ",address\n"
            + "repeated-valid,DIGITAL,D6,LIVE,W1J 9DZ,SITE-1,London,London,First address\n"
            + "repeated-valid,DIGITAL,D6,INACTIVE,W1J 9DZ,SITE-1,London,London,Changed address\n";

        importCsv(csv)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalRows").value(2))
            .andExpect(jsonPath("$.created").value(1))
            .andExpect(jsonPath("$.duplicates").value(1))
            .andExpect(jsonPath("$.failed").value(0))
            .andExpect(jsonPath("$.errors", hasSize(1)))
            .andExpect(jsonPath("$.errors[0].rowNumber").value(3))
            .andExpect(jsonPath("$.errors[0].frameId").value("repeated-valid"));

        mockMvc.perform(get("/api/frames/{frameId}", "repeated-valid"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("LIVE"))
            .andExpect(jsonPath("$.location.address").value("First address"));
    }

    @Test
    void historyFailureRollsBackOneRowWithoutAffectingTheNextRow() throws Exception {
        doThrow(new DataIntegrityViolationException("history insert failed"))
            .when(historyRepository)
            .saveAndFlush(argThat(history ->
                "history-fails".equals(history.getFrame().getFrameId())
            ));
        String csv = REQUIRED_HEADERS + "\n"
            + "history-fails,DIGITAL,D6,LIVE,W1J 9DZ,SITE-1,London,London\n"
            + "history-succeeds,DIGITAL,D6,LIVE,W1J 9DZ,SITE-2,London,London\n";

        importCsv(csv)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalRows").value(2))
            .andExpect(jsonPath("$.created").value(1))
            .andExpect(jsonPath("$.duplicates").value(0))
            .andExpect(jsonPath("$.failed").value(1))
            .andExpect(jsonPath("$.errors", hasSize(1)))
            .andExpect(jsonPath("$.errors[0].rowNumber").value(2))
            .andExpect(jsonPath("$.errors[0].frameId").value("history-fails"))
            .andExpect(jsonPath("$.errors[0].reason").value("Frame could not be persisted"));

        assertThat(frameRepository.existsById("history-fails")).isFalse();
        assertThat(frameRepository.existsById("history-succeeds")).isTrue();
        assertThat(historyRepository.findByFrameFrameIdOrderByOccurredAtDescIdDesc("history-fails"))
            .isEmpty();

        mockMvc.perform(get("/api/frames/{frameId}/history", "history-succeeds"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].eventType").value("IMPORTED"))
            .andExpect(jsonPath("$[0].source").value("CSV_UPLOAD"));
    }

    @Test
    void rejectsMissingRequiredHeaders() throws Exception {
        String csv = "frame_id,type_classic_digital,format,status,postcode,site_no,town\n"
            + "missing-region,DIGITAL,D6,LIVE,W1J 9DZ,SITE-1,London\n";

        importCsv(csv)
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.title").value("Invalid CSV file"))
            .andExpect(jsonPath("$.detail").value("CSV is missing required headers: region"));

        assertThat(frameRepository.count()).isZero();
    }

    @Test
    void rejectsMalformedCsvBeforePersistingEarlierValidRows() throws Exception {
        String csv = REQUIRED_HEADERS + "\n"
            + "would-have-been-valid,DIGITAL,D6,LIVE,W1J 9DZ,SITE-1,London,London\n"
            + "broken,DIGITAL,D6,LIVE,\"unterminated\n";

        importCsv(csv)
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.title").value("Invalid CSV file"))
            .andExpect(jsonPath("$.detail").value("CSV content is malformed"));

        assertThat(frameRepository.count()).isZero();
    }

    @Test
    void recordsImportedCsvHistory() throws Exception {
        String csv = REQUIRED_HEADERS + "\n"
            + "history-import,DIGITAL,D6,LIVE,W1J 9DZ,SITE-1,London,London\n";
        importCsv(csv).andExpect(status().isOk());

        mockMvc.perform(get("/api/frames/{frameId}/history", "history-import"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].eventType").value("IMPORTED"))
            .andExpect(jsonPath("$[0].source").value("CSV_UPLOAD"))
            .andExpect(jsonPath("$[0].changedFields").isEmpty());
    }

    @Test
    void rejectsEmptyAndUnreadableFiles() throws Exception {
        importFile(new byte[0])
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detail").value("CSV file is empty"));

        importFile(new byte[] {(byte) 0xC3, 0x28})
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detail").value("CSV content is not valid UTF-8"));
    }

    private org.springframework.test.web.servlet.ResultActions importCsv(String csv) throws Exception {
        return importFile(csv.getBytes(StandardCharsets.UTF_8));
    }

    private org.springframework.test.web.servlet.ResultActions importFile(byte[] content) throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "frames.csv", "text/csv", content);
        return mockMvc.perform(multipart("/api/frames/import").file(file));
    }

    private void createFrame(String frameId, String status) throws Exception {
        mockMvc.perform(post("/api/frames")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "frameId": "%s",
                      "mediaType": "DIGITAL",
                      "format": "D6",
                      "environment": "UNDERGROUND",
                      "status": "%s",
                      "location": {
                        "postcode": "W1J 9DZ",
                        "region": "London",
                        "town": "London"
                      },
                      "site": {
                        "siteNumber": "SITE-1"
                      }
                    }
                    """.formatted(frameId, status)))
            .andExpect(status().isCreated());
    }
}
