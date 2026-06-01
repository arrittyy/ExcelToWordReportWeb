package com.reportweb.controller;

import com.reportweb.dto.InstrumentDTOs;
import com.reportweb.entity.Instrument;
import com.reportweb.repository.InstrumentRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/instruments")
@RequiredArgsConstructor
@Slf4j
public class InstrumentsController {

    private final InstrumentRepository instrumentRepository;

    @GetMapping
    public ResponseEntity<List<InstrumentDTOs.InstrumentList>> getAllInstruments() {
        try {
            List<Instrument> instruments = instrumentRepository.findAll();
            List<InstrumentDTOs.InstrumentList> instrumentList = instruments.stream()
                    .map(this::convertToInstrumentListDTO)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(instrumentList);
        } catch (Exception ex) {
            log.error("Error getting instruments", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<InstrumentDTOs.InstrumentList> getInstrument(@PathVariable Integer id) {
        try {
            Instrument instrument = instrumentRepository.findById(id).orElse(null);
            if (instrument == null) {
                return ResponseEntity.notFound().build();
            }

            InstrumentDTOs.InstrumentList response = convertToInstrumentListDTO(instrument);
            return ResponseEntity.ok(response);
        } catch (Exception ex) {
            log.error("Error getting instrument with id: {}", id, ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping
    public ResponseEntity<?> createInstrument(
            @Valid @RequestBody InstrumentDTOs.CreateInstrument createInstrumentDTO) {
        try {
            // 检查仪器名称是否已存在
            if (instrumentRepository.existsByInstrumentName(createInstrumentDTO.getInstrumentName())) {
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("message", "仪器名称已存在");
                return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
            }

            // 检查仪器编号是否已存在（如果提供了编号）
            if (createInstrumentDTO.getInstrumentNumber() != null && !createInstrumentDTO.getInstrumentNumber().trim().isEmpty()) {
                if (instrumentRepository.existsByInstrumentNumber(createInstrumentDTO.getInstrumentNumber())) {
                    Map<String, String> errorResponse = new HashMap<>();
                    errorResponse.put("message", "仪器编号已存在");
                    return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
                }
            }

            Instrument instrument = new Instrument();
            instrument.setInstrumentName(createInstrumentDTO.getInstrumentName());
            instrument.setInstrumentModel(createInstrumentDTO.getInstrumentModel());
            instrument.setInstrumentNumber(createInstrumentDTO.getInstrumentNumber());
            instrument.setCreatedAt(LocalDateTime.now());
            instrument.setUpdatedAt(LocalDateTime.now());

            Instrument savedInstrument = instrumentRepository.save(instrument);

            InstrumentDTOs.InstrumentList response = convertToInstrumentListDTO(savedInstrument);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception ex) {
            log.error("Error creating instrument", ex);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "创建仪器失败，请稍后重试");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateInstrument(
            @PathVariable Integer id,
            @Valid @RequestBody InstrumentDTOs.UpdateInstrument updateInstrumentDTO) {
        try {
            Instrument instrument = instrumentRepository.findById(id).orElse(null);
            if (instrument == null) {
                return ResponseEntity.notFound().build();
            }

            // 检查仪器名称是否已被其他仪器使用
            if (!instrument.getInstrumentName().equals(updateInstrumentDTO.getInstrumentName()) &&
                    instrumentRepository.existsByInstrumentName(updateInstrumentDTO.getInstrumentName())) {
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("message", "仪器名称已被其他仪器使用");
                return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
            }

            // 检查仪器编号是否已被其他仪器使用（如果提供了编号）
            if (updateInstrumentDTO.getInstrumentNumber() != null && !updateInstrumentDTO.getInstrumentNumber().trim().isEmpty()) {
                if (!updateInstrumentDTO.getInstrumentNumber().equals(instrument.getInstrumentNumber()) &&
                        instrumentRepository.existsByInstrumentNumber(updateInstrumentDTO.getInstrumentNumber())) {
                    Map<String, String> errorResponse = new HashMap<>();
                    errorResponse.put("message", "仪器编号已被其他仪器使用");
                    return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
                }
            }

            instrument.setInstrumentName(updateInstrumentDTO.getInstrumentName());
            instrument.setInstrumentModel(updateInstrumentDTO.getInstrumentModel());
            instrument.setInstrumentNumber(updateInstrumentDTO.getInstrumentNumber());
            instrument.setUpdatedAt(LocalDateTime.now());

            instrumentRepository.save(instrument);

            return ResponseEntity.noContent().build();
        } catch (Exception ex) {
            log.error("Error updating instrument with id: {}", id, ex);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "更新仪器失败，请稍后重试");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteInstrument(@PathVariable Integer id) {
        try {
            if (!instrumentRepository.existsById(id)) {
                return ResponseEntity.notFound().build();
            }

            instrumentRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        } catch (Exception ex) {
            log.error("Error deleting instrument with id: {}", id, ex);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "删除仪器失败，请稍后重试");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    private InstrumentDTOs.InstrumentList convertToInstrumentListDTO(Instrument instrument) {
        InstrumentDTOs.InstrumentList dto = new InstrumentDTOs.InstrumentList();
        dto.setId(instrument.getId());
        dto.setInstrumentName(instrument.getInstrumentName());
        dto.setInstrumentModel(instrument.getInstrumentModel());
        dto.setInstrumentNumber(instrument.getInstrumentNumber());
        dto.setCreatedAt(instrument.getCreatedAt());
        dto.setUpdatedAt(instrument.getUpdatedAt());
        return dto;
    }
}
