package com.reportweb.controller;

import com.reportweb.dto.MaterialLibraryDTOs;
import com.reportweb.entity.User;
import com.reportweb.security.CustomUserPrincipal;
import com.reportweb.security.UserRoleUtils;
import com.reportweb.service.MaterialLibraryReviewerUtils;
import com.reportweb.service.MaterialLibraryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/material-library")
@RequiredArgsConstructor
@Slf4j
public class MaterialLibraryController {

    private final MaterialLibraryService materialLibraryService;

    @GetMapping("/capabilities")
    public ResponseEntity<?> capabilities(Authentication authentication) {
        try {
            if (isSubUser(authentication)) {
                return forbiddenSubUser();
            }
            User currentUser = currentUser(authentication);
            return ResponseEntity.ok(materialLibraryService.capabilities(currentUser));
        } catch (Exception ex) {
            log.error("Error getting material library capabilities", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping
    public ResponseEntity<?> listEntries(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword,
            Authentication authentication) {
        try {
            if (isSubUser(authentication)) {
                return forbiddenSubUser();
            }
            return ResponseEntity.ok(materialLibraryService.listEntries(category, keyword));
        } catch (Exception ex) {
            log.error("Error listing material library entries", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/keys")
    public ResponseEntity<?> listKeys(Authentication authentication) {
        try {
            if (isSubUser(authentication)) {
                return forbiddenSubUser();
            }
            MaterialLibraryDTOs.KeysResponse response = new MaterialLibraryDTOs.KeysResponse();
            response.setKeys(materialLibraryService.listAllKeys());
            return ResponseEntity.ok(response);
        } catch (Exception ex) {
            log.error("Error listing material keys", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/my-submissions")
    public ResponseEntity<?> mySubmissions(Authentication authentication) {
        try {
            User currentUser = currentUser(authentication);
            if (UserRoleUtils.isSubUser(currentUser)) {
                return forbiddenSubUser();
            }
            return ResponseEntity.ok(materialLibraryService.listMySubmissions(currentUser.getId()));
        } catch (Exception ex) {
            log.error("Error listing my material submissions", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/pending")
    public ResponseEntity<?> listPending(Authentication authentication) {
        try {
            User currentUser = currentUser(authentication);
            if (!MaterialLibraryReviewerUtils.canReview(currentUser)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            return ResponseEntity.ok(materialLibraryService.listPending());
        } catch (Exception ex) {
            log.error("Error listing pending material entries", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/pending/count")
    public ResponseEntity<?> countPending(Authentication authentication) {
        try {
            User currentUser = currentUser(authentication);
            if (!MaterialLibraryReviewerUtils.canReview(currentUser)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            Map<String, Long> body = new HashMap<>();
            body.put("count", materialLibraryService.countPending());
            return ResponseEntity.ok(body);
        } catch (Exception ex) {
            log.error("Error counting pending material entries", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{id}/logs")
    public ResponseEntity<?> listLogs(@PathVariable Long id, Authentication authentication) {
        try {
            if (isSubUser(authentication)) {
                return forbiddenSubUser();
            }
            return ResponseEntity.ok(materialLibraryService.listLogs(id));
        } catch (Exception ex) {
            log.error("Error listing material approval logs for id {}", id, ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping
    public ResponseEntity<?> submit(
            @Valid @RequestBody MaterialLibraryDTOs.CreateRequest request,
            Authentication authentication) {
        try {
            User currentUser = currentUser(authentication);
            if (UserRoleUtils.isSubUser(currentUser)) {
                return forbiddenSubUser();
            }
            MaterialLibraryDTOs.ListItem created = materialLibraryService.submit(currentUser, request);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (IllegalArgumentException ex) {
            return badRequest(ex.getMessage());
        } catch (Exception ex) {
            log.error("Error submitting material library entry", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(
            @PathVariable Long id,
            @Valid @RequestBody MaterialLibraryDTOs.UpdateRequest request,
            Authentication authentication) {
        try {
            User currentUser = currentUser(authentication);
            if (UserRoleUtils.isSubUser(currentUser)) {
                return forbiddenSubUser();
            }
            MaterialLibraryDTOs.ListItem updated = materialLibraryService.updateEntry(id, currentUser, request);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException ex) {
            return badRequest(ex.getMessage());
        } catch (Exception ex) {
            log.error("Error updating material library entry {}", id, ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/{id}/delete-request")
    public ResponseEntity<?> deleteRequest(@PathVariable Long id, Authentication authentication) {
        try {
            User currentUser = currentUser(authentication);
            if (UserRoleUtils.isSubUser(currentUser)) {
                return forbiddenSubUser();
            }
            MaterialLibraryDTOs.ListItem result = materialLibraryService.requestDelete(id, currentUser);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException ex) {
            return badRequest(ex.getMessage());
        } catch (Exception ex) {
            log.error("Error requesting delete for material {}", id, ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteDraft(@PathVariable Long id, Authentication authentication) {
        try {
            User currentUser = currentUser(authentication);
            if (UserRoleUtils.isSubUser(currentUser)) {
                return forbiddenSubUser();
            }
            materialLibraryService.deleteDraft(id, currentUser);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException ex) {
            return badRequest(ex.getMessage());
        } catch (Exception ex) {
            log.error("Error deleting material draft {}", id, ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<?> approve(@PathVariable Long id, Authentication authentication) {
        try {
            User currentUser = currentUser(authentication);
            if (!MaterialLibraryReviewerUtils.canReview(currentUser)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            MaterialLibraryDTOs.ListItem approved = materialLibraryService.approve(id, currentUser);
            return ResponseEntity.ok(approved);
        } catch (IllegalArgumentException ex) {
            return badRequest(ex.getMessage());
        } catch (Exception ex) {
            log.error("Error approving material library entry {}", id, ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<?> reject(
            @PathVariable Long id,
            @Valid @RequestBody MaterialLibraryDTOs.RejectRequest request,
            Authentication authentication) {
        try {
            User currentUser = currentUser(authentication);
            if (!MaterialLibraryReviewerUtils.canReview(currentUser)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            MaterialLibraryDTOs.ListItem rejected = materialLibraryService.reject(
                    id, currentUser, request.getReviewComment());
            return ResponseEntity.ok(rejected);
        } catch (IllegalArgumentException ex) {
            return badRequest(ex.getMessage());
        } catch (Exception ex) {
            log.error("Error rejecting material library entry {}", id, ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private User currentUser(Authentication authentication) {
        CustomUserPrincipal principal = (CustomUserPrincipal) authentication.getPrincipal();
        return principal.getUser();
    }

    private boolean isSubUser(Authentication authentication) {
        return UserRoleUtils.isSubUser(currentUser(authentication));
    }

    private ResponseEntity<Map<String, String>> forbiddenSubUser() {
        Map<String, String> body = new HashMap<>();
        body.put("message", "子账号无权访问材质库");
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
    }

    private ResponseEntity<Map<String, String>> badRequest(String message) {
        Map<String, String> body = new HashMap<>();
        body.put("message", message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }
}
