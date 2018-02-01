# ExCELL Scheduling Service

The Scheduling API is designed to optimize the route of a trip with multiple stops. It offers two endpoints which follow different purposes:
schedulingnew finds the best fit for a new appointment into a given schedule of existing appointments (JSON array).
schedulingcare plans the complete schedules for multiple cars. Therefore, it requires a detailed description (JSON) about different client constraints.

## API Doc

This projects provides a [Swagger](https://swagger.io/) interface to support the Open API initiative. The Java library [Springfox](http://springfox.github.io/springfox/) is used to automatically create the swagger UI configuration from annotations in the Java Spring code.

An online version of the scheduling API is available on the ExCELL Developer Portal: [Try it out!](https://www.excell-mobility.de/developer/docs.php?service=scheduling_service). You need to sign up first in order to access the services from the portal. Every user receives a token that he/she has to use for authorization for each service.


## Developers

* Stephan Piper (BHS)
* Felix Kunde (BHS)


## Contact

* spieper [at] beuth-hochschule.de
* fkunde [at] beuth-hochschule.de


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
