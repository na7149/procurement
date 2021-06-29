package procurement;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

 @RestController
 public class DeliverymanagementController {
    @Autowired
    DeliverymanagementRepository deliverymanagementRepository;

    @RequestMapping(value = "/deliverymanagement/announceInspectionResult",
       method = RequestMethod.GET,
       produces = "application/json;charset=UTF-8")
    public boolean announceInspectionResult(HttpServletRequest request, HttpServletResponse response) {
       boolean status = false;

       String procNo = String.valueOf(request.getParameter("procNo"));
       
       System.out.println("@@@@@@@@@@@@@@@@@companyNm@" + request.getParameter("companyNm"));
       
       Deliverymanagement deliverymanagement = deliverymanagementRepository.findByProcNo(procNo);

        if(deliverymanagement.getProcAgency() == null || "조달청".equals(deliverymanagement.getProcAgency()) == false){
            deliverymanagement.setCompanyNo(request.getParameter("companyNo"));
            deliverymanagement.setCompanyNm(request.getParameter("companyNm"));
            deliverymanagement.setInspectionSuccFlag(Boolean.parseBoolean(request.getParameter("inspectionSuccFlag")));

            deliverymanagementRepository.save(deliverymanagement);

            status = true;
       }

       return status;
    }

 }
