package scheduling.component;

import java.util.List;

import com.graphhopper.jsprit.core.problem.constraint.HardActivityConstraint;
import com.graphhopper.jsprit.core.problem.misc.JobInsertionContext;
import com.graphhopper.jsprit.core.problem.solution.route.activity.TourActivity;

import beans.ServiceVehicleConstraint;

public class ServiceVehicleActivityConstraint implements HardActivityConstraint {

	private List<ServiceVehicleConstraint> serviceVehicleConstraint;
	
	public ServiceVehicleActivityConstraint(List<ServiceVehicleConstraint> serviceVehicleConstraint) {
		this.serviceVehicleConstraint = serviceVehicleConstraint;
	}
	
	@Override
	public ConstraintsStatus fulfilled(JobInsertionContext iFacts,
			TourActivity prevAct, TourActivity newAct,
			TourActivity nextAct, double prevActDepTime) {
		
		if(serviceVehicleConstraint != null && serviceVehicleConstraint.size() > 0) {
			
			String vehicleId = iFacts.getNewVehicle().getId();
			List<TourActivity> activities = iFacts.getRoute().getActivities();
			boolean fullfilledOnce = true;
			
			for(int const_index = 0; const_index < serviceVehicleConstraint.size(); const_index++) {
				for(int act_index = 0; act_index < activities.size(); act_index++) {
					String serviceID = activities.get(act_index).getLocation().getId();
					if (serviceVehicleConstraint.get(const_index).getVehicleId().equals(vehicleId) ||
					    serviceVehicleConstraint.get(const_index).getServiceId().equals(serviceID)) {
						
						if (serviceVehicleConstraint.get(const_index).getVehicleId().equals(vehicleId)
								&& serviceVehicleConstraint.get(const_index).getServiceId().equals(serviceID)
								&& activities.get(act_index).getName().equals("service")) {
							fullfilledOnce = true;
							break;
						}
						else
							fullfilledOnce = false;
					}
				}
			}
			
			if (fullfilledOnce)
				return ConstraintsStatus.FULFILLED;
			else
				return ConstraintsStatus.NOT_FULFILLED;
		}
		
		return ConstraintsStatus.FULFILLED;
	}
}
