package com.pm.patientservice.controller;

import com.pm.patientservice.model.Patient;
import com.pm.patientservice.repository.PatientRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@Transactional
class PatientControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PatientRepository patientRepository;

    private Patient savedPatient(String name, String email) {
        Patient p = new Patient();
        p.setName(name);
        p.setEmail(email);
        p.setAddress("123 Main St, Springfield");
        p.setDateOfBirth(LocalDate.of(1990, 5, 15));
        p.setRegisterDate(LocalDate.of(2024, 1, 10));
        return patientRepository.save(p);
    }

    // ── GET /api/v1/patients ─────────────────────────────────────────────────

    @Test
    void getAllPatients_shouldReturn200WithList() throws Exception {
        savedPatient("John Doe", "john.doe@example.com");

        mockMvc.perform(get("/api/v1/patients"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1));
    }

    // ── GET /api/v1/patients/{id} ────────────────────────────────────────────

    @Test
    void getPatientById_shouldReturn200_whenPatientExists() throws Exception {
        Patient saved = savedPatient("John Doe", "john.doe@example.com");

        mockMvc.perform(get("/api/v1/patients/{id}", saved.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("john.doe@example.com"));
    }

    @Test
    void getPatientById_shouldReturn404_whenPatientDoesNotExist() throws Exception {
        mockMvc.perform(get("/api/v1/patients/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    // ── POST /api/v1/patients ────────────────────────────────────────────────

    @Test
    void createPatient_shouldReturn201_whenValidRequest() throws Exception {
        String body = """
                {
                    "name": "Jane Smith",
                    "email": "jane.smith@example.com",
                    "address": "456 Oak Ave, Shelbyville",
                    "dateOfBirth": "1985-03-22",
                    "registerDate": "2024-02-05"
                }
                """;

        mockMvc.perform(post("/api/v1/patients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("jane.smith@example.com"));
    }

    @Test
    void createPatient_shouldReturn400_whenEmailIsInvalid() throws Exception {
        String body = """
                {
                    "name": "Jane Smith",
                    "email": "not-a-valid-email",
                    "address": "456 Oak Ave, Shelbyville",
                    "dateOfBirth": "1985-03-22",
                    "registerDate": "2024-02-05"
                }
                """;

        mockMvc.perform(post("/api/v1/patients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createPatient_shouldReturn400_whenEmailAlreadyExists() throws Exception {
        savedPatient("John Doe", "john.doe@example.com");

        String body = """
                {
                    "name": "Another John",
                    "email": "john.doe@example.com",
                    "address": "789 Pine Rd, Capital City",
                    "dateOfBirth": "1992-07-11",
                    "registerDate": "2024-03-01"
                }
                """;

        mockMvc.perform(post("/api/v1/patients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    // ── PUT /api/v1/patients/{id} ────────────────────────────────────────────

    @Test
    void updatePatient_shouldReturn200_whenValidRequest() throws Exception {
        Patient saved = savedPatient("John Doe", "john.doe@example.com");

        String body = """
                {
                    "name": "John Updated",
                    "email": "john.doe@example.com",
                    "address": "999 New Blvd, Capital City",
                    "dateOfBirth": "1990-05-15"
                }
                """;

        mockMvc.perform(put("/api/v1/patients/{id}", saved.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("John Updated"));
    }

    @Test
    void updatePatient_shouldReturn404_whenPatientDoesNotExist() throws Exception {
        String body = """
                {
                    "name": "Ghost",
                    "email": "ghost@example.com",
                    "address": "Nowhere Lane",
                    "dateOfBirth": "1990-01-01"
                }
                """;

        mockMvc.perform(put("/api/v1/patients/{id}", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound());
    }

    // ── DELETE /api/v1/patients/{id} ─────────────────────────────────────────

    @Test
    void deletePatient_shouldReturn204_whenPatientExists() throws Exception {
        Patient saved = savedPatient("John Doe", "john.doe@example.com");

        mockMvc.perform(delete("/api/v1/patients/{id}", saved.getId()))
                .andExpect(status().isNoContent());
    }

    @Test
    void deletePatient_shouldReturn404_whenPatientDoesNotExist() throws Exception {
        mockMvc.perform(delete("/api/v1/patients/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }
}