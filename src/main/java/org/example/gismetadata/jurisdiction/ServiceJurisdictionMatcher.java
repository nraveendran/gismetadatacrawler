package org.example.gismetadata.jurisdiction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ServiceJurisdictionMatcher {
    private static final Logger log = LoggerFactory.getLogger(ServiceJurisdictionMatcher.class);

    private final ServiceJurisdictionRepository repository;

    public ServiceJurisdictionMatcher(ServiceJurisdictionRepository repository) {
        this.repository = repository;
    }

    public void match() {
        log.info("Starting service jurisdiction matching.");
        int cityMatches = repository.insertCityMatches();
        int countyMatches = repository.insertCountyMatches();
        int cogMatches = repository.insertCogMatchesForUnmatchedServices();
        log.info(
                "Service jurisdiction matching finished. cityMatches={}, countyMatches={}, cogMatches={}",
                cityMatches,
                countyMatches,
                cogMatches);
    }
}
