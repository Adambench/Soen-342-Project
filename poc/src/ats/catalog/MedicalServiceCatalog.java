package ats.catalog;

import ats.domain.MedicalService;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public class MedicalServiceCatalog {
    private final Map<String, MedicalService> services = new LinkedHashMap<>();

    public MedicalService findById(String serviceId) {
        return serviceId == null ? null : services.get(serviceId.trim().toUpperCase());
    }

    public void add(MedicalService ms) {
        services.put(ms.getServiceId(), ms);
    }

    public Collection<MedicalService> getAll() {
        return services.values();
    }
}
