package procurement.external;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@FeignClient(name="procurementmanagement", url="http://${api.url.procurement}:8080", fallback=DeliverymanagementServiceFallback.class)
//@FeignClient(name="procurementmanagement", url="http://localhost:8081", fallback=DeliverymanagementServiceFallback.class)
public interface DeliverymanagementService {
    
    @RequestMapping(method= RequestMethod.GET, path="/deliverymanagements/announceInspectionResult")
    public boolean announceInspectionResult(@RequestParam("procNo") String procNo, @RequestParam("companyNo") String companyNo, 
    @RequestParam("companyNm") String companyNm, @RequestParam("inspectionSuccFlag") Boolean inspectionSuccFlag);

}

