package com.example.backend.controller.admin;

import com.example.backend.dto.AdminStudentViewDto;
import com.example.backend.dto.DashboardStatsDto;
import com.example.backend.service.StudentManagementService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/manager")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
public class AdminStudentController {

    private final StudentManagementService studentManagementService;

    @GetMapping("/dashboard-stats")
    public ResponseEntity<DashboardStatsDto> getDashboardStats() {
        return ResponseEntity.ok(studentManagementService.getDashboardStats());
    }

    @GetMapping("/students")
    public ResponseEntity<Page<AdminStudentViewDto>> getStudents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, sortBy));
        return ResponseEntity.ok(studentManagementService.getStudentDirectory(pageable));
    }

    @GetMapping("/students/{id}/contact-info")
    public ResponseEntity<Map<String, String>> getContactInfo(@PathVariable UUID id) {
        String mobile = studentManagementService.getStudentContactInfo(id);
        return ResponseEntity.ok(Map.of("mobileNumber", mobile));
    }
}
