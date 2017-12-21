package scheduling;

//import java.util.List;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletContext;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.ResponseEntity;

import com.google.common.base.Predicates;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

//import beans.GeoPoint;
//import rest.RoutingConnector;
import scheduling.component.AppointmentPlanner;
//import scheduling.component.TourOptimizer;
import scheduling.controller.PlanningController;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.ApiKey;
import springfox.documentation.service.AuthorizationScope;
import springfox.documentation.service.Contact;
import springfox.documentation.service.SecurityReference;
import springfox.documentation.service.VendorExtension;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spi.service.contexts.SecurityContext;
import springfox.documentation.spring.web.paths.RelativePathProvider;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger.web.ApiKeyVehicle;
import springfox.documentation.swagger.web.SecurityConfiguration;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@SpringBootApplication
@EnableSwagger2
@ComponentScan(basePackageClasses = {
	    PlanningController.class,
	    AppointmentPlanner.class
	})
public class Application {

    public static void main(String[] args) /*throws Exception*/ {
        SpringApplication.run(Application.class, args);
        
        /*
 		RoutingConnector router = new RoutingConnector();
        TourOptimizer optimizer = new TourOptimizer(router);
        
        GeoPoint[] pointArr;
        pointArr = new GeoPoint[] {
        		new GeoPoint(51.0405489,13.6575849502255),
				new GeoPoint(50.8533642,11.893690966753),
				new GeoPoint(50.56468,10.25533),
				new GeoPoint(50.8679921,10.825524),
				new GeoPoint(50.9171709,11.025134),
				new GeoPoint(48.8098985,12.9908809),
				new GeoPoint(50.8767416,11.7023218),
				new GeoPoint(50.6949499,12.0599328),
				new GeoPoint(50.9375682,12.2615406),
				new GeoPoint(51.0405489,13.6575849502255)
        };
        
//        List<Double[]> route = router.getRoute(pointArr);
        
        List<Double[]> bestRoute = optimizer.getOptimalRoute(pointArr);
        
        System.out.println("Travel time original: " + 
        						optimizer.calculateTravelTimes(
        								optimizer.shufflePoints(pointArr,new Integer[]{0,1,2,3,4,5,6,7})
        								));
        
        System.out.println("Travel time opt.: " + 
				optimizer.calculateTravelTimes(
						optimizer.shufflePoints(pointArr,new Integer[]{7,3,0,1,2,4,5,6})
						));
        
        System.out.println("Travel distance original: " + 
				optimizer.calculateTravelDistances(
						optimizer.shufflePoints(pointArr,new Integer[]{0,1,2,3,4,5,6,7})
						));

		System.out.println("Travel distance opt.: " + 
		optimizer.calculateTravelDistances(
				optimizer.shufflePoints(pointArr,new Integer[]{7,3,0,1,2,4,5,6})
				));*/
        
//        total: 40320
//        Best time: 93
//        Best combination: [7, 3, 0, 1, 2, 4, 5, 6]
    }

    @Bean
    public Docket schedulingApi(ServletContext servletContext) { 
        return new Docket(DocumentationType.SWAGGER_2)
          .groupName("excell-scheduling-api")
          .select()
          .apis(RequestHandlerSelectors.any()) 
          .paths(Predicates.not(PathSelectors.regex("/error")))
          .paths(Predicates.not(PathSelectors.regex("/health")))
          .paths(Predicates.not(PathSelectors.regex("/health.json")))
          .build()
          .genericModelSubstitutes(ResponseEntity.class)
          .protocols(Sets.newHashSet("https"))
//          .host("localhost:44434")
          //.host("141.64.5.234/excell-scheduling-api")
          .host("dlr-integration.minglabs.com")
          .securitySchemes(Lists.newArrayList(apiKey()))
          .securityContexts(Lists.newArrayList(securityContext()))
          .apiInfo(apiInfo())
          .pathProvider(new RelativePathProvider(servletContext) {
                @Override
                public String getApplicationBasePath() {
                    return "/api/v1/service-request/schedulingservice";
                }
            });
    }
    
	private ApiKey apiKey() {
		return new ApiKey("api_key", "Authorization", "header");
	}
	
    private SecurityContext securityContext() {
        return SecurityContext.builder()
            .securityReferences(defaultAuth())
            .forPaths(PathSelectors.regex("/*.*"))
            .build();
    }

    private List<SecurityReference> defaultAuth() {
    	List<SecurityReference> ls = new ArrayList<>();
    	AuthorizationScope[] authorizationScopes = new AuthorizationScope[0];
    	SecurityReference s = new SecurityReference("api_key", authorizationScopes);
    	ls.add(s);
    	return ls;
    }

	@Bean
	public SecurityConfiguration security() {
		return new SecurityConfiguration(null, null, null, null, "Token", ApiKeyVehicle.HEADER, "Authorization", ",");
	}
	
    private ApiInfo apiInfo() {
        ApiInfo apiInfo = new ApiInfo(
          "ExCELL Scheduling API",
          "Diese API liefert eine Liste von möglichen Zeitfenstern für Termine oder Dienstleistungen, basierend auf Routenoptimierung. "
          + "Die Hauptanwendungsfälle für den Tourenplanungsalgorithmus sind einerseits das Pflegeszenario und andererseits das Logistikszenario. "
          + "Für das Pflegeszenario werden Fahrzeuge und dazugehörige Pflegedienstleistungen optimiert, "
          + "wohingegen für das Logistikszenario Fahrzeuge und Kundentermine analysiert werden. "
          + "Der Tourenplanungsalgorithmus nimmt für die obigen Anwendungsfälle JSON entgegen und generiert JSON als Rückgabe.\n\n"
          + "The Scheduling API is designed to optimize the route of a trip with multiple stops."
          + " It offers two endpoints which follow different purposes:\n"
          + "schedulingnew finds the best fit for a new appointment into a given schedule of existing appointments (JSON array).\n"
          + "schedulingcare plans the complete schedules for multiple cars."
          + " Therefore, it requires a detailed description (JSON) about different client constraints.\n",
          "1.0",
          "Use only for testing",
          new Contact(
        		  "Beuth Hochschule für Technik Berlin - Labor für Rechner- und Informationssysteme - MAGDa Gruppe",
        		  "https://projekt.beuth-hochschule.de/magda/poeple",
        		  "spieper@beuth-hochschule"),
          "Apache 2",
          "http://www.apache.org/licenses/LICENSE-2.0",
          new ArrayList<VendorExtension>());
        return apiInfo;
    }
    
}