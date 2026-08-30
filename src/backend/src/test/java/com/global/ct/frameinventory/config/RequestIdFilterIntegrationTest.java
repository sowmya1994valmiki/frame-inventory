package com.global.ct.frameinventory.config;

import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class RequestIdFilterIntegrationTest {

    private static final String UUID_PATTERN =
        "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-"
            + "[0-9a-f]{4}-[0-9a-f]{12}";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void returnsValidSuppliedRequestId() throws Exception {
        mockMvc.perform(get("/actuator/health")
                .header("X-Request-ID", "client-request-123"))
            .andExpect(status().isOk())
            .andExpect(header().string(
                "X-Request-ID",
                "client-request-123"
            ));
    }

    @Test
    void generatesRequestIdWhenOneIsNotSupplied() throws Exception {
        mockMvc.perform(get("/actuator/health"))
            .andExpect(status().isOk())
            .andExpect(header().string(
                "X-Request-ID",
                matchesPattern(UUID_PATTERN)
            ));
    }

    @Test
    void replacesUnsafeSuppliedRequestId() throws Exception {
        mockMvc.perform(get("/actuator/health")
                .header("X-Request-ID", "unsafe request id"))
            .andExpect(status().isOk())
            .andExpect(header().string(
                "X-Request-ID",
                matchesPattern(UUID_PATTERN)
            ));
    }
}
