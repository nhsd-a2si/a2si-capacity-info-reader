package com.nhsd.a2si.capacityinforeader.providers.ekhuft;

import com.nhsd.a2si.capacityinforeader.providers.LeadingZeros;
import com.nhsd.a2si.capacityinformation.domain.CapacityInformation;
import com.nhsd.a2si.capacityserviceclient.CapacityServiceClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * East Kent Hospitals University NHS Foundation have a publicly accessible API providing wait times for some of their
 * services.
 * The API DOESN'T use official Service Id codes but uses its own three letter codes.
 * Therefore this job must get the data, lookup the service id and use the "minor_wait" field as the time time
 */
@Component
public class EkhuftReadCapacityInformationRestApiJob implements Job {

    private static final Logger logger = LoggerFactory.getLogger(EkhuftReadCapacityInformationRestApiJob.class);

    private HashMap<String, String> codeToServiceIdMap;

    private DateTimeFormatter capacityInformationDateTimeFormatter = DateTimeFormatter.ofPattern(CapacityInformation.STRING_DATE_FORMAT);


    @Autowired
    private RestTemplate ekhuftApiRestTemplate;

    // The values injected from configuration in this class relate to the job itself and NOT about
    // the scheduling of the job
    @Value("${ekhuftApiJob.apiUrl}")
    private String ekhuftApiUrl;

    /*
     * Spring good practice nowadays prefers construction injection over setter/field injection, however
     * Quartz creates the jobs like the one below and assumes a default constructor so it is not possible
     * to use constructor injection here.
     */
    @Autowired
    private CapacityServiceClient capacityServiceClient;

    public EkhuftReadCapacityInformationRestApiJob() {

        codeToServiceIdMap = new HashMap<>();
        codeToServiceIdMap.put("BHD", "140119");
        codeToServiceIdMap.put("KCH", "1332749337");
        codeToServiceIdMap.put("QEH", "140960");
        codeToServiceIdMap.put("WHH", "140958");
    }

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
    		if (ekhuftApiUrl == null || !ekhuftApiUrl.toLowerCase().startsWith("http")) {
    			return;
    		}

        try {

            ResponseEntity<String> responseEntity =
                    ekhuftApiRestTemplate.exchange(
                            ekhuftApiUrl,
                            HttpMethod.GET,
                            null,
                            String.class);

            HashMap<String, LinkedHashMap> result =
                    new ObjectMapper().readValue(responseEntity.getBody(), HashMap.class);

            LocalDateTime lastUpdated = LocalDateTime.now();

            result.forEach((k, v) -> {

                CapacityInformation capacityInformation = new CapacityInformation();
                capacityInformation.setServiceId(codeToServiceIdMap.get(k));
                String waitTime = (String) v.get("minor_wait");

                if(waitTime.length() > 0) {
                    int iWaitingTimeMinutes = new Integer(LeadingZeros.strip(waitTime));
                    capacityInformation.setWaitingTimeMins(iWaitingTimeMinutes);
                }

                capacityInformation.setLastUpdated(capacityInformationDateTimeFormatter.format(lastUpdated));

                logger.debug("Calling Capacity Service to store Capacity Information {}", capacityInformation);
                try {
                    capacityServiceClient.saveCapacityInformation(capacityInformation);
                } catch (Throwable t) {
                    logger.error("Exception Thrown saving Capacity Information {} in Capacity Service",
                            capacityInformation, t);
                }
                logger.debug("Called Capacity Service that saved Capacity Information {}", capacityInformation);

            });


        } catch (Exception e) {
            logger.error("Unable to get response from {}", ekhuftApiUrl, e);
        }


    }

}
