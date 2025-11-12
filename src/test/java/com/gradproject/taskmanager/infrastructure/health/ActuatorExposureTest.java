package com.gradproject.taskmanager.infrastructure.health;

import com.gradproject.taskmanager.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@AutoConfigureMockMvc
class ActuatorExposureTests extends AbstractIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    void health_isPublic_andMetricsNotExposed() throws Exception {
        mockMvc.perform(get("/actuator/health").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").exists());

        mockMvc.perform(get("/actuator/metrics").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }
}
