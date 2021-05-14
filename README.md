# R4 project
Welcome to FHIR R4 sample project

## Getting Started
These instructions will get you a copy of the project up and running on your local machine for development and testing purposes. See deployment for notes on how to deploy the project on a live system.

### Prerequisites
*	Java 8
*	Tomcat 8 or 9
*	Postgres SQL DB 10.x
*	Maven 3.5.X

## Installing

### Clone FHIR Server repository 
Clone FHIR Server repository using command 

```
$ git clone https://github.com/drajer-health/medmorph-kar.git
```

### Postgres Configuration

**Create the Database**

Create the database by running the below command in command prompt

```
$ createdb -h localhost -p 5432 -U postgres r4
```

### Tomcat Configuration 

**Update application database properties.**

Open application.properties file under fhir-server/src/main/resources and change below properties and save the file. 

```  
jdbc.url=jdbc:postgresql://localhost:5432/r4
jdbc.username=postgres
jdbc.password=postgres
```

### Built Application 
Run Maven build to build application war file. 
```
$ mvn clean install 
```
This will generate a war file under target/{application-name}.war. Copy this to your tomcat webapp directory for deployment.

**Start Tomcat service**

## Verification 
Verify using Postman or equivalent tool by running various FHIR APIs on the R4 server. Postman Collection is added to the repository 'Connectathon-May-2021.postman_collection'
```
For example: http://localhost:<port>/r4/fhir/PlanDefinition/[id]
```
  
