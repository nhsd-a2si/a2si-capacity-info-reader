# a2si-capacity-info-reader
This application is designed to have all scheduled jobs to get data from providers and feed it
into the Capacity Service.
It uses Quartz, an open source scheduler. Quartz has been configured to use a MySQL database to store the scheduled 
job details.
Using a database is the way Quartz ensures only one server at a time runs a job. It uses database locking 
when a server tries to run a job.

This is a Spring Boot application, when running a Maven build, it also creates a zip file that includes
the Spring Boot application and a Docker file, which is a specific set of instructions describing the 
Docker image to be built.

## DHU Job 
Read file from DHU's FTP site. Job runs under Quartz. It will add the data into the Capacity Service 
cache

DHU store the updated capacity information within a text file that resides on an FTP Server. 
The FTP server is deployed within AWS and DHU *MUST* push the file to the 
correct FTP server. 
This will be important when NHS Digital take over the deployment, the existing FTP Server should NOT 
be relied upon to exist permanently because it is deployed under an account owned and paid for by BJSS.
The job reads the file from the FTP server and calls the Capacity Service 
(using the Capacity Service client module) to add the new or updated information.

## Getting started
First, download the code from GitHub. This can be done using the desktop git tool, an IDE which supports git or by downloading the code as a zip file which you can then extract.

Next, install the dev tools and dependencies....

##Installation of Development Tools and Dependencies
Install Git for Windows:
Install official git release: https://git-scm.com/download/win

Or install GitHub Desktop which also includes a GUI interface for git management: https://desktop.github.com/

###Install Java Development Kit 8:
http://www.oracle.com/technetwork/java/javase/downloads/

###Install Maven 3:
https://maven.apache.org/download.cgi

###Environment Variables
Ensure that the system environment variables for Java and Maven are set correctly, as described below...

M2_HOME should point to the install directory of your local Maven install folder, e.g.
M2_HOME C:\Maven\apache-maven-3.3.9

JAVA_HOME should point to the install directory of your local Java JDK install folder, e.g.

JAVA_HOME C:\Program Files\Java\jdk1.8.0_121

PATH should contain the bin directory of both M2_HOME and JAVA_HOME, e.g.

```
...;%JAVA_HOME%\bin;%M2_HOME%\bin;...
```

## Dependencies
Before building this module, the following modules must have been downloaded and built, using "mvn install"
to add them into your local Maven Repository.

1) a2si-capacity-service-client

## Use maven to build the project:

cd {projectRoot}
mvn clean install

the Maven "install" goal stores the built artifact into the local Maven repository, 
making it accessible to other projects using this repository.

The application is going to be deployed in AWS using Elastic Beanstalk, using Docker as a container. Elastic Beanstalk
allows Spring Boot applications to be packaged along with a DockerFile in a single zip file. This zip file is all
that is required to deploy into AWS Elastic Beanstalk. Environment variables may be required to define the 
Spring Profiles Active variable.

## Running the Application
The MySQL schema MUST be pre-populated with the Quartz tables and it's small amount of configuration data. 
Use the ```qrtz_tables for MySql.sql``` file in the ```resources``` folder to create these.

Connection to the FTP server uses Secure FTP and requires a pem file holding the RSA key to access 
the DHU FTP server. This is stored in the ```resources``` folder and referenced by the configuration yml files.
It is possible that other integration partners could also have an FTP server and so PEM files should be named
with reference to the provider of data.

## Maintaining the Application
The application forms the basis for adding multiple jobs to get capacity information from different providers.
A new provider would warrant a new job and job configuration class.  
It is recommended that classes for a new provider are all assigned to a single package that identifies
the provider.
It is expected that all integrations to providers will follow of the following patterns:
10. FTP Server integration where the provider pushes files to an FTP server and a job is create to 
grab these files and store capacity information by using the data in the FTP file to call the 
Capacity Service.
20. Job accesses API of the provider. This job would make periodic calls to a provider hosted API that 
allowed current capacity information to be accessed. The job would retrieve the latest information and call
the Capacity Service
30. If a provider was able to access the Capacity Service directly then there would be no need for any 
scheduled jobs and it would be up to the provider to directly feed info into the Capacity Service. 
This is perhaps the ideal scenario, there is no extra bespoke development for NHS Digital although a 
recommendation would be to create an unique username and password for the provider, and to ensure the
multiple sets of credentials could be easily maintained, including easy revocation.  

## Application Configuration
Following a best practice approach that comes from Spring, configuration files hold data specific to 
environments whilst wiring of class dependencies is done via Java Configuration classes. These classes
use the configuration files to set simple properties and are created when the application starts.

