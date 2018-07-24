package com.nhsd.a2si.capacityinforeader.providers.dhu;

import com.nhsd.a2si.capacityinformation.domain.CapacityInformation;
import com.nhsd.a2si.capacityserviceclient.CapacityServiceClient;
import com.jcraft.jsch.*;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.FileCopyUtils;

import java.io.*;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Properties;
import java.util.Vector;

/**
 * DHU drop files with the latest capacity information onto an FTP server
 * This code will fetch the files and set the capacity information for the Service Ids in the file
 * It may set the information to the same as previously processed if a new file to replace the old one
 * has not been sent to the FTP server, however there are no side effect to processing the same information twice
 * and it is much simpler than trying to decipher of the file has been processed before
 */
@Component
public class DhuReadCapacityInformationFtpFileJob implements Job {

    private static final Logger logger = LoggerFactory.getLogger(DhuReadCapacityInformationFtpFileJob.class);

    private static DateTimeFormatter ftpFiledateTimeFormatter1 =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.nnnnnnnnn");

    private static DateTimeFormatter ftpFiledateTimeFormatter2 =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private DateTimeFormatter capacityInformationDateTimeFormatter =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    //private static final String remoteFileName = "ELRUCUCCWaitingTimes.csv";
    private static final String lineElementSeparator = ",";

    // The values injected from configuration in this class relate to the job itself and NOT about
    // the scheduling of the job
    @Value("${dhuFtpJob.ftpServer}")
    private String ftpServer;

    @Value("${dhuFtpJob.ftpPort}")
    private int ftpPort;

    @Value("${dhuFtpJob.ftpUsername}")
    private String ftpUsername;

    @Value("${dhuFtpJob.privateKeyFileName}")
    private String privateKeyFileName;

    /*
     * Spring good practice nowadays prefers construction injection over setter/field injection, however
     * Quartz creates the jobs like the one below and assumes a default constructor so it is not possible
     * to use constructor injection here.
     */
    @Autowired
    private CapacityServiceClient capacityServiceClient;

    public DhuReadCapacityInformationFtpFileJob() {

    }

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {

        CapacityInformation capacityInformation;

        try {
            JSch jSch = new JSch();

            byte[] privateKey = null;

            ClassPathResource privateKeyFile = new ClassPathResource(privateKeyFileName);
            try {
                privateKey = FileCopyUtils.copyToByteArray(privateKeyFile.getInputStream());
            } catch (IOException e) {
                logger.warn("IOException", e);
            }

            jSch.addIdentity(ftpUsername, privateKey, null, new byte[0]);

            Session session = jSch.getSession(ftpUsername, ftpServer, ftpPort);

            Properties properties = new Properties();
            properties.put("StrictHostKeyChecking", "no");

            session.setConfig(properties);
            session.connect(5000);

            ChannelSftp channelSftp = (ChannelSftp) session.openChannel("sftp");
            channelSftp.connect();

            // NOTE THAT THE RWE FILE CURRENTLY HAS A DIFFERENT DATE FORMAT THAN THE ELRUCUCC file so the code
            // must try both formats.

            // Get List of all files on server with suffix of "csv" in the 'DHU' directory
            String folderName = "dhu";
            channelSftp.cd(folderName);
            Vector<ChannelSftp.LsEntry> csvFileList = channelSftp.ls("*.csv");

            // For each entry in the list, get the file name and process each file
            for(ChannelSftp.LsEntry csvFile : csvFileList) {
                InputStream inputStream = channelSftp.get(csvFile.getFilename());
                try {

                    logger.info("Processing CSV file - {}/{}, last changed {}", folderName, csvFile.getFilename(),  csvFile.getAttrs().getMtimeString());

                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));

                    String line;

                    while ((line = bufferedReader.readLine()) != null) {

                        String[] lineElements = line.split(lineElementSeparator);

                        // Create Capacity Information object and put it into cache
                        // CapacityInformation capacityInformation = new CapacityInformation();
                        // Split line into strings, create capacity Information bean, add it to cache using
                        // a rest api called to the capacity service

                        // ignore the first line because it is just a header row
                        if (!lineElements[0].equals("DOS_Service_ID")) {

                            capacityInformation = new CapacityInformation();
                            capacityInformation.setServiceId(lineElements[0]);
                            try {
                            	// If there is a waiting time, convert it to integer and set the property
                            	// Else, if it is blank then null the property
                            	// This gives the data supplier the ability to remove waiting time information
                            	if (lineElements[2].trim().length() > 0) {
	                            	int iWaitingTimeMinutes = new Integer(lineElements[2]);
	                            	capacityInformation.setWaitingTimeMins(iWaitingTimeMinutes);
                            	} else {
                            		capacityInformation.setWaitingTimeMins(null);
                            	}
                            } catch (NumberFormatException nfe) {
                            	logger.error("Waiting time mins for Service Id {} was not numeric. Value was {}", capacityInformation.getServiceId(), lineElements[2]);
                            	continue;
                            }
                            
                            LocalDateTime lastUpdated;

                            try {
                                lastUpdated = LocalDateTime.parse(lineElements[6], ftpFiledateTimeFormatter1);
                            } catch (DateTimeParseException dtpe) {
                                lastUpdated = LocalDateTime.parse(lineElements[6], ftpFiledateTimeFormatter2);
                            }

                            capacityInformation.setLastUpdated(
                                    capacityInformationDateTimeFormatter.format(lastUpdated));

                            logger.info("Calling Capacity Service to store Capacity Information {}", capacityInformation);
                            try {
                                capacityServiceClient.saveCapacityInformation(capacityInformation);
                            } catch (Throwable t) {
                                logger.error("Exception Thrown saving Capacity Information {} in Capacity Service", capacityInformation, t);
                            }
                            logger.debug("Called Capacity Service that saved Capacity Information {}", capacityInformation);
                        }
                    }

                } catch (IOException io) {
                    System.out.println("Exception occurred during reading file from SFTP server due to " +
                            io.getMessage());
                    io.getMessage();

                }
            }

            channelSftp.disconnect();
            session.disconnect();

        } catch (SftpException sftpException) {
            logger.error("SftpException Exception thrown", sftpException);
        } catch (JSchException jschException) {
            logger.error("Jsch Exception thrown", jschException);
        }
    }

}
