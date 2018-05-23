package scheduling.component;

import java.util.List;

import com.graphhopper.jsprit.core.problem.constraint.HardActivityConstraint;
import com.graphhopper.jsprit.core.problem.misc.JobInsertionContext;
import com.graphhopper.jsprit.core.problem.solution.route.activity.TourActivity;

import beans.JobOrderConstraint;

public class JobOrderActivityConstraint implements HardActivityConstraint {

	private List<JobOrderConstraint> jobOrderConstraints;
	
	public JobOrderActivityConstraint(List<JobOrderConstraint> jobOrderConstraints) {
		this.jobOrderConstraints = jobOrderConstraints;
	}
	
	@Override
	public ConstraintsStatus fulfilled(JobInsertionContext iFacts,
			TourActivity prevAct, TourActivity newAct,
			TourActivity nextAct, double prevActDepTime) {
		
		if(jobOrderConstraints != null && jobOrderConstraints.size() > 0) {
			
			List<TourActivity> activities = iFacts.getRoute().getActivities();
			
			for(int const_index = 0; const_index < jobOrderConstraints.size(); const_index++) {
				
				String idBefore = jobOrderConstraints.get(const_index).getBeforeJobId();
				String idAfter = jobOrderConstraints.get(const_index).getAfterJobId();
				int beforeIndex = 0;
				int afterIndex = 0;
				boolean foundBeforeAct = false;
				boolean foundAfterAct = false;
				
				for(int act_index = 0; act_index < activities.size(); act_index++) {
					
					TourActivity tourActivity = activities.get(act_index);
					
					if(tourActivity.getLocation().getId().equals(idBefore) 
							&& tourActivity.getName().equals("service")) {
						beforeIndex = act_index;
						foundBeforeAct = true;
					}
					if(tourActivity.getLocation().getId().equals(idAfter)
							&& tourActivity.getName().equals("service")) {
						afterIndex = act_index;
						foundAfterAct = true;
					}
					
				}
				
				if(foundAfterAct && foundBeforeAct 
						&& beforeIndex < afterIndex
						&& otherConstraintsDoNotFail(activities, const_index, jobOrderConstraints)) {
					return ConstraintsStatus.FULFILLED;
				}
				if(foundAfterAct && foundBeforeAct && beforeIndex > afterIndex) {
					return ConstraintsStatus.NOT_FULFILLED;
				}
				
			}
			
		}
		
		return ConstraintsStatus.FULFILLED;
	}

	private boolean otherConstraintsDoNotFail(List<TourActivity> activities, 
			int index, List<JobOrderConstraint> jobConstraintsFinal) {
		
		if(index >= jobConstraintsFinal.size()) {
			return true;
		} else {
			for(int i = index; i < jobConstraintsFinal.size(); i++) {
				
				String idBefore = jobConstraintsFinal.get(i).getBeforeJobId();
				String idAfter = jobConstraintsFinal.get(i).getAfterJobId();
				int beforeIndex = 0;
				int afterIndex = 0;
				boolean foundBeforeAct = false;
				boolean foundAfterAct = false;
				
				for(int act_index = 0; act_index < activities.size(); act_index++) {
					
					TourActivity tourActivity = activities.get(act_index);
					
					if(tourActivity.getLocation().getId().equals(idBefore) 
							&& tourActivity.getName().equals("service")) {
						beforeIndex = act_index;
						foundBeforeAct = true;
					}
					if(tourActivity.getLocation().getId().equals(idAfter)
							&& tourActivity.getName().equals("service")) {
						afterIndex = act_index;
						foundAfterAct = true;
					}
				}
				
				if(foundAfterAct && foundBeforeAct && beforeIndex > afterIndex) {
					return false;
				}
			}
		}
		
		return true;
	}
}
