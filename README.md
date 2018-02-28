# ExCELL Scheduling Service

The Scheduling API is designed to optimize the route of a trip with multiple stops. It offers two endpoints which follow different purposes:
schedulingnew finds the best fit for a new appointment into a given schedule of existing appointments (JSON array).
schedulingcare plans the complete schedules for multiple cars. Therefore, it requires a detailed description (JSON) about different client constraints. The service acts as a wrapper for [jspirit](https://github.com/graphhopper/jsprit) by Graphhopper.


## Setup

This web service comes as a [SpringBoot](https://projects.spring.io/spring-boot/) application so it's very easy to test it on your local machine. If you run the service from inside a Java IDE a Tomcat server will be launched and you can access the service through a browser via localhost:44434.

### Build it

The project is using [Maven](https://maven.apache.org/) as a build tool and for managing the software dependencies. So in order to build the software you should install Maven on your machine. To create an executable JAR file for your local machine open you favourite shell environment and run:

<pre>mvn clean package</pre>

This creates a JAR file called `tourenplanung-0.0.1-SNAPSHOT.jar`. You can change the name in the pom.xml file.

### Run it

On your local machine run the JAR with:

<pre>java -jar tourenplanung-0.0.1-SNAPSHOT.jar</pre>

On a remote machine use it is necessary to specify the location of the OSM file and routing graph directory. You might also want to change the server port

<pre>java -jar tourenplanung-0.0.1-SNAPSHOT.jar --server.port=44444</pre>


## API Doc

This projects provides a [Swagger](https://swagger.io/) interface to support the Open API initiative. The Java library [Springfox](http://springfox.github.io/springfox/) is used to automatically create the swagger UI configuration from annotations in the Java Spring code.

An online version of the scheduling API is available on the ExCELL Developer Portal: [Try it out!](https://www.excell-mobility.de/developer/docs.php?service=scheduling_service). You need to sign up first in order to access the services from the portal. Every user receives a token that he/she has to use for authorization for each service.


## Developers

* Stephan Piper (BHS)
* Felix Kunde (BHS)
* Maximilian Allies (BHS)


## Contact

* spieper [at] beuth-hochschule.de
* fkunde [at] beuth-hochschule.de


## Acknowledgement
The Scheduling Service has been realized within the ExCELL project funded by the Federal Ministry for Economic Affairs and Energy (BMWi) and German Aerospace Center (DLR) - agreement 01MD15001B.


## Special Thanks

* Graphopper Team


## Disclaimer

THIS SOFTWARE IS PROVIDED "AS IS" AND "WITH ALL FAULTS." 
BHS MAKES NO REPRESENTATIONS OR WARRANTIES OF ANY KIND CONCERNING THE 
QUALITY, SAFETY OR SUITABILITY OF THE SKRIPTS, EITHER EXPRESSED OR 
IMPLIED, INCLUDING WITHOUT LIMITATION ANY IMPLIED WARRANTIES OF 
MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, OR NON-INFRINGEMENT.

IN NO EVENT WILL BHS BE LIABLE FOR ANY INDIRECT, PUNITIVE, SPECIAL, 
INCIDENTAL OR CONSEQUENTIAL DAMAGES HOWEVER THEY MAY ARISE AND EVEN IF 
BHS HAS BEEN PREVIOUSLY ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
