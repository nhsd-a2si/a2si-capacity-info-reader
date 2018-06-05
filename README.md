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
cache.

DHU store the updated capacity information within a text file that resides on an FTP Server. 

The FTP server is deployed within AWS and DHU *MUST* push the file to the 
correct FTP server. 

The job reads the file from the FTP server and calls the Capacity Service 
(using the Capacity Service client module) to add the new or updated information.


## Getting started
1. Set up your [Development Environment](docs/dev_setup.md)

2. Clone this repo to your local machine.

3. Follow the instructions below to configure and run the application.


## Application Dependencies
Before building this module, the following modules must have been downloaded and built, using `mvn clean install`
to add them into your local Maven Repository.

* [a2si-capacity-service-client](https://github.com/nhsd-a2si/a2si-capacity-service-client)

Detailed instructions are in the README of the respective repositories.


## Building the Application
```
cd {projectRoot}
mvn clean package
```

The Maven project builds two deployable artefacts: 

+ the "Uber-Jar" common to a lot of applications built 
using Spring Boot, it contains Tomcat as a container within the jar as well as all of Tomcat's dependencies 
and the application's dependencies

+ a zip file containing the "Uber-Jar" and a Dockerfile 


### Configuring the Application

#### Spring Profiles
Spring Profiles are used to define the configuration used.

This requires an environment variable **`SPRING_PROFILES_ACTIVE`** to be included when building or starting up the capacity service.

The possible values for the **`SPRING_PROFILES_ACTIVE`** variable are:

+ `capacity-info-reader-aws`
+ `capacity-info-reader-local`

There are several configuration files which relate to the different profiles used. These are:

+ **application.yml**  
The default configuration provides defaults for the port the server runs under and some base values which are shared amongst all configuration profiles.

+ **application-capacity-info-reader-aws.yml**  
*Profile name: `capacity-info-reader-aws`*  
For use when running the Info Reader in AWS

+ **application-capacity-info-reader-local.yml**   
*Profile name: `capacity-info-reader-local`*    
For use when running the Info Reader locally on your machine


## Running the Application
The MySQL schema MUST be pre-populated with the Quartz tables and it's small amount of configuration data. 
Use the `qrtz_tables for MySql.sql` file in the `resources` folder to create these.

Connection to the FTP server uses Secure FTP and requires a pem file holding the RSA key to access 
the DHU FTP server. This is stored in the `resources` folder and referenced by the configuration yml files.

It is possible that other integration partners could also have an FTP server and so PEM files should be named
with reference to the provider of data.

### Running Locally
To run the service locally you can specify the configuration properties on the command line instead of using environment variables:

```
java -Dspring.profiles.active=capacity-info-reader-local \
     -Dcapacity.service.client.api.username=dummyValue \
     -Dcapacity.service.client.api.password=dummyValue \
     -jar a2si-capacity-info-reader-0.0.14-SNAPSHOT.jar
```

## Maintaining the Application
The application forms the basis for adding multiple jobs to get capacity information from different providers.

A new provider would warrant a new job and job configuration class.  

It is recommended that classes for a new provider are all assigned to a single package that identifies
the provider.

It is expected that all integrations to providers will follow one of the following patterns:

1. FTP Server integration where the provider pushes files to an FTP server and a job is create to 
grab these files and store capacity information by using the data in the FTP file to call the 
Capacity Service.

2. Job accesses API of the provider. This job would make periodic calls to a provider hosted API that 
allowed current capacity information to be accessed. The job would retrieve the latest information and call
the Capacity Service

3. If a provider was able to access the Capacity Service directly then there would be no need for any 
scheduled jobs and it would be up to the provider to directly feed info into the Capacity Service. 

This is perhaps the ideal scenario, there is no extra bespoke development for NHS Digital although a 
recommendation would be to create an unique username and password for the provider, and to ensure the
multiple sets of credentials could be easily maintained, including easy revocation.  


