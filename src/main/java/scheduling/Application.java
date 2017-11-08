package scheduling;

//import java.util.List;

import java.util.ArrayList;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.ResponseEntity;



import com.google.common.base.Predicates;

//import beans.GeoPoint;
//import rest.RoutingConnector;
import scheduling.component.AppointmentPlanner;
//import scheduling.component.TourOptimizer;
import scheduling.controller.PlanningController;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.service.VendorExtension;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
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
    public Docket schedulingApi() { 
        return new Docket(DocumentationType.SWAGGER_2)
          .groupName("excell-scheduling-api")
          .select()
          .apis(RequestHandlerSelectors.any()) 
          .paths(Predicates.not(PathSelectors.regex("/error")))
          .paths(Predicates.not(PathSelectors.regex("/health")))
          .paths(Predicates.not(PathSelectors.regex("/health.json")))
          .build()
          .genericModelSubstitutes(ResponseEntity.class)
          //.protocols(Sets.newHashSet("https"))
//          .host("localhost:44434")
          .host("141.64.5.234/excell-scheduling-api")
          .apiInfo(apiInfo())
          ;
    }
    
    private ApiInfo apiInfo() {
        ApiInfo apiInfo = new ApiInfo(
          "ExCELL Scheduling API",
          "This API provides a list of suitable time slots for appointments based on route optimization.",
          "Version 1.0",
          "Use only for testing",
          new Contact(
        		  "Felix Kunde, Stephan Pieper",
        		  "https://projekt.beuth-hochschule.de/magda/poeple",
        		  "spieper@beuth-hochschule"),
          "Apache 2",
          "http://www.apache.org/licenses/LICENSE-2.0",
          new ArrayList<VendorExtension>());
        return apiInfo;
    }
    
}