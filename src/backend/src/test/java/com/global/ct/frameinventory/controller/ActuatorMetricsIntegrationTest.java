package com.global.ct.frameinventory.controller;

import static org.hamcrest.Matchers.hasItems;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ActuatorMetricsIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void exposesRepresentativeBuiltInMetrics() throws Exception {
        mockMvc.perform(get("/actuator/health"))
            .andExpect(status().isOk());

        mockMvc.perform(get("/actuator/metrics"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.names", hasItems(
                "jvm.memory.used",
                "http.server.requests",
                "hikaricp.connections.active"
            )));
    }
}
