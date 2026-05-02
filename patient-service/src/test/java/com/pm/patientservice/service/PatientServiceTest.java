package com.pm.patientservice.service;

import com.pm.patientservice.dto.PatientRequestDTO;
import com.pm.patientservice.dto.PatientResponseDTO;
import com.pm.patientservice.exception.EmailAlreadyExistException;
import com.pm.patientservice.exception.PatientNotFoundException;
import com.pm.patientservice.model.Patient;
import com.pm.patientservice.repository.PatientRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PatientServiceTest {

    @Mock
    private PatientRepository patientRepository;

    @InjectMocks
    private PatientService patientService;

    private UUID patientId;
    private Patient patient;
    private PatientRequestDTO requestDTO;

    @BeforeEach
    void setUp() {
        patientId = UUID.fromString("a1b2c3d4-e5f6-7890-abcd-ef1234567890");

        patient = new Patient();
        patient.setId(patientId);
        patient.setName("John Doe");
        patient.setEmail("john.doe@example.com");
        patient.setAddress("123 Main St, Springfield");
        patient.setDateOfBirth(LocalDate.of(1990, 5, 15));
        patient.setRegisterDate(LocalDate.of(2024, 1, 10));

        requestDTO = new PatientRequestDTO();
        requestDTO.setName("John Doe");
        requestDTO.setEmail("john.doe@example.com");
        requestDTO.setAddress("123 Main St, Springfield");
        requestDTO.setDateOfBirth("1990-05-15");
        requestDTO.setRegisterDate("2024-01-10");
    }

    // ── getPatients ──────────────────────────────────────────────────────────

    @Test
    void getPatients_shouldReturnListOfPatientResponseDTOs() {
        Patient second = new Patient();
        second.setId(UUID.randomUUID());
        second.setName("Jane Smith");
        second.setEmail("jane.smith@example.com");
        second.setAddress("456 Oak Ave, Shelbyville");
        second.setDateOfBirth(LocalDate.of(1985, 3, 22));
        second.setRegisterDate(LocalDate.of(2024, 2, 5));

        when(patientRepository.findAll()).thenReturn(List.of(patient, second));

        List<PatientResponseDTO> result = patientService.getPatients();

        assertThat(result).hasSize(2);
    }

    // ── getPatient ───────────────────────────────────────────────────────────

    @Test
    void getPatient_shouldReturnPatientResponseDTO_whenPatientExists() {
        when(patientRepository.findById(patientId)).thenReturn(Optional.of(patient));

        PatientResponseDTO result = patientService.getPatient(patientId);

        assertThat(result.getId()).isEqualTo(patientId.toString());
        assertThat(result.getName()).isEqualTo("John Doe");
        assertThat(result.getEmail()).isEqualTo("john.doe@example.com");
        assertThat(result.getAddress()).isEqualTo("123 Main St, Springfield");
        assertThat(result.getDateOfBirth()).isEqualTo("1990-05-15");
    }

    @Test
    void getPatient_shouldThrowPatientNotFoundException_whenPatientDoesNotExist() {
        when(patientRepository.findById(patientId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> patientService.getPatient(patientId))
                .isInstanceOf(PatientNotFoundException.class);
    }

    // ── createPatient ────────────────────────────────────────────────────────

    @Test
    void createPatient_shouldSaveAndReturnPatientResponseDTO() {
        when(patientRepository.existsByEmail("john.doe@example.com")).thenReturn(false);
        when(patientRepository.save(any(Patient.class))).thenReturn(patient);

        PatientResponseDTO result = patientService.createPatient(requestDTO);

        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo("john.doe@example.com");
        verify(patientRepository).save(any(Patient.class));
    }

    @Test
    void createPatient_shouldThrowEmailAlreadyExistException_whenEmailIsDuplicate() {
        when(patientRepository.existsByEmail("john.doe@example.com")).thenReturn(true);

        assertThatThrownBy(() -> patientService.createPatient(requestDTO))
                .isInstanceOf(EmailAlreadyExistException.class);

        verify(patientRepository, never()).save(any());
    }

    // ── updatePatient ────────────────────────────────────────────────────────

    @Test
    void updatePatient_shouldUpdateAndReturnPatientResponseDTO() {
        requestDTO.setName("John Updated");
        requestDTO.setAddress("789 Pine Rd, Capital City");

        Patient updatedPatient = new Patient();
        updatedPatient.setId(patientId);
        updatedPatient.setName("John Updated");
        updatedPatient.setEmail("john.doe@example.com");
        updatedPatient.setAddress("789 Pine Rd, Capital City");
        updatedPatient.setDateOfBirth(LocalDate.of(1990, 5, 15));
        updatedPatient.setRegisterDate(LocalDate.of(2024, 1, 10));

        when(patientRepository.findById(patientId)).thenReturn(Optional.of(patient));
        when(patientRepository.existsByEmailAndIdNot("john.doe@example.com", patientId)).thenReturn(false);
        when(patientRepository.save(any(Patient.class))).thenReturn(updatedPatient);

        PatientResponseDTO result = patientService.updatePatient(patientId, requestDTO);

        assertThat(result.getName()).isEqualTo("John Updated");
        assertThat(result.getAddress()).isEqualTo("789 Pine Rd, Capital City");
        assertThat(result.getEmail()).isEqualTo("john.doe@example.com");
    }

    @Test
    void updatePatient_shouldThrowPatientNotFoundException_whenPatientDoesNotExist() {
        when(patientRepository.findById(patientId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> patientService.updatePatient(patientId, requestDTO))
                .isInstanceOf(PatientNotFoundException.class);

        verify(patientRepository, never()).save(any());
    }

    @Test
    void updatePatient_shouldThrowEmailAlreadyExistException_whenEmailTakenByAnotherPatient() {
        requestDTO.setEmail("taken@example.com");

        when(patientRepository.findById(patientId)).thenReturn(Optional.of(patient));
        when(patientRepository.existsByEmailAndIdNot("taken@example.com", patientId)).thenReturn(true);

        assertThatThrownBy(() -> patientService.updatePatient(patientId, requestDTO))
                .isInstanceOf(EmailAlreadyExistException.class);

        verify(patientRepository, never()).save(any());
    }

    // ── deletePatient ────────────────────────────────────────────────────────

    @Test
    void deletePatient_shouldDeletePatient_whenPatientExists() {
        when(patientRepository.existsById(patientId)).thenReturn(true);

        patientService.deletePatient(patientId);

        verify(patientRepository, times(1)).deleteById(patientId);
    }

    @Test
    void deletePatient_shouldThrowPatientNotFoundException_whenPatientDoesNotExist() {
        when(patientRepository.existsById(patientId)).thenReturn(false);

        assertThatThrownBy(() -> patientService.deletePatient(patientId))
                .isInstanceOf(PatientNotFoundException.class);

        verify(patientRepository, never()).deleteById(any());
    }
}