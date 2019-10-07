package io.flowing.trip.saga.camunda.springboot;

import static org.springframework.web.bind.annotation.RequestMethod.PUT;

import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TripRestController {
  
  @Autowired
  private TripBookingSaga tripBookingSaga; 
  
  @RequestMapping(path = "/trip", method = PUT)
  public void bookTrip(HttpServletResponse response) {    
    tripBookingSaga.bookTrip();
    // we do it asynchronously, so send a 202 (see https://github.com/berndruecker/flowing-retail/tree/master/rest/ for more details on this)
    response.setStatus(HttpServletResponse.SC_ACCEPTED);    
  }

}