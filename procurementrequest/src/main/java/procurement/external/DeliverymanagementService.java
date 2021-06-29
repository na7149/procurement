
package procurement.external;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.Date;

@FeignClient(name="procurementmanagement", url="http://procurementmanagement:8080")
public interface DeliverymanagementService {
    @RequestMapping(method= RequestMethod.PATCH, path="/deliverymanagements")
    public void announceInspectionResult(@RequestBody Deliverymanagement deliverymanagement);

}

