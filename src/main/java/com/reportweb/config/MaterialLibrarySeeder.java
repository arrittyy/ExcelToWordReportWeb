package com.reportweb.config;

import com.reportweb.entity.MaterialLibraryEntry;
import com.reportweb.repository.MaterialLibraryEntryRepository;
import com.reportweb.service.MaterialCategoryUtils;
import com.reportweb.service.MaterialLibraryService;
import com.reportweb.service.MaterialPropertyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Component
@Order(15)
@RequiredArgsConstructor
@Slf4j
public class MaterialLibrarySeeder implements CommandLineRunner {

    public static final String SYSTEM_USER_ID = "SYSTEM";
    public static final String SYSTEM_USER_NAME = "系统导入";

    private final MaterialLibraryEntryRepository entryRepository;
    private final MaterialPropertyService materialPropertyService;
    @Override
    @Transactional
    public void run(String... args) {
        if (entryRepository.countBySource(MaterialLibraryService.SOURCE_SEEDED) > 0) {
            log.debug("Material library SEEDED entries already exist, skip import");
            return;
        }

        Map<String, Map<String, String>> staticData = materialPropertyService.getStaticMaterialPropertiesSnapshot();
        int imported = 0;
        int skipped = 0;

        for (Map.Entry<String, Map<String, String>> item : staticData.entrySet()) {
            String materialKey = item.getKey();
            if (entryRepository.findByMaterialKeyIgnoreCase(materialKey).isPresent()) {
                skipped++;
                continue;
            }

            Map<String, String> properties = item.getValue() == null
                    ? new HashMap<>()
                    : new HashMap<>(item.getValue());

            MaterialLibraryEntry entry = new MaterialLibraryEntry();
            entry.setMaterialKey(materialKey);
            entry.setPrimaryCategory(MaterialCategoryUtils.inferPrimaryCategory(properties));
            entry.setStatus(MaterialLibraryService.STATUS_APPROVED);
            entry.setSource(MaterialLibraryService.SOURCE_SEEDED);
            entry.setModificationType(MaterialLibraryService.MOD_CREATE);
            entry.setProperties(properties);
            entry.setSubmittedByUserId(SYSTEM_USER_ID);
            entry.setSubmittedByUserName(SYSTEM_USER_NAME);
            entryRepository.save(entry);
            imported++;
        }

        materialPropertyService.refreshMaterialCache();
        log.info("Material library seed complete: imported={}, skipped(existing)={}", imported, skipped);
    }
}
