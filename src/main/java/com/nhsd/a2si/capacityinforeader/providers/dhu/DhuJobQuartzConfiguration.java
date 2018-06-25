package com.nhsd.a2si.capacityinforeader.providers.dhu;

import org.quartz.SimpleTrigger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.quartz.JobDetailFactoryBean;
import org.springframework.scheduling.quartz.SimpleTriggerFactoryBean;

@Configuration
public class DhuJobQuartzConfiguration {

    // The values injected from configuration in this class relate to the scheduling of the job
    // and NOT about the job itself
    @Value("${dhuFtpJob.key}")
    private String dhuFtpJobKey;

    @Value("${dhuFtpJob.description}")
    private String duuFtpJobDescription;

    @Value("${dhuFtpJob.startDelay}")
    private Long dhuFtpJobStartDelay;

    @Value("${dhuFtpJob.repeatInterval}")
    private Long dhuFtpJobRepeatInterval;

    /**
     * Define the DHU FTP Job
     *
     * @return DHU FTP Job
     */
    @Bean(name = "dhuFtpJobDetails")
    public JobDetailFactoryBean dhuFtpJobDetails() {

        JobDetailFactoryBean dhuFtpJobDetails = new JobDetailFactoryBean();
        dhuFtpJobDetails.setJobClass(DhuReadCapacityInformationFtpFileJob.class);
        dhuFtpJobDetails.setDescription(duuFtpJobDescription);
        dhuFtpJobDetails.setDurability(true);
        dhuFtpJobDetails.setName(dhuFtpJobKey);

        return dhuFtpJobDetails;

    }

    /**
     * Define the DHU FTP Job Trigger
     *
     * @return Simple Trigger for DHU FTP Job
     */
    @Bean(name = "dhuFtpJobTrigger")
    public SimpleTriggerFactoryBean dhuFtpJobTrigger() {

        SimpleTriggerFactoryBean dhuFtpJobTrigger = new SimpleTriggerFactoryBean();
        dhuFtpJobTrigger.setJobDetail(dhuFtpJobDetails().getObject());
        dhuFtpJobTrigger.setStartDelay(dhuFtpJobStartDelay);
        dhuFtpJobTrigger.setRepeatInterval(dhuFtpJobRepeatInterval);
        dhuFtpJobTrigger.setRepeatCount(SimpleTrigger.REPEAT_INDEFINITELY);
        dhuFtpJobTrigger.setMisfireInstruction(
                SimpleTrigger.MISFIRE_INSTRUCTION_RESCHEDULE_NEXT_WITH_REMAINING_COUNT);

        return dhuFtpJobTrigger;

    }

}
