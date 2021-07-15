package procurement;

import procurement.config.kafka.KafkaProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

@Service
public class PolicyHandler{
    @Autowired DeliveryrequestRepository deliveryrequestRepository;

    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverInspectionRequestPatched_ReceiveInspectionRequest(@Payload InspectionRequestPatched inspectionRequestPatched){

        if(!inspectionRequestPatched.validate()) return;
        // Get Methods


        // Sample Logic //
        //System.out.println("\n\n##### listener ReceiveInspectionRequest : " + inspectionRequestPatched.toJson() + "\n\n");

        //Deliveryrequest inspectionResult = new Deliveryrequest();
        //inspectionResult.setProcNo(inspectionRequestPatched.getProcNo());
        //inspectionResult.setCompanyNo(inspectionRequestPatched.getCompanyNo());
        //inspectionResult.setCompanyNm(inspectionRequestPatched.getCompanyNm());
        //inspectionResult.setCompanyPhoneNo(inspectionRequestPatched.getCompanyPhoneNo());
        //inspectionResult.setInspectionContents(inspectionRequestPatched.getInspectionContents());

        //deliveryrequestRepository.save(inspectionResult);

        Deliveryrequest inspectionResult = deliveryrequestRepository.findByProcNo(inspectionRequestPatched.getProcNo());
        inspectionResult.setCompanyNo(inspectionRequestPatched.getCompanyNo());
        inspectionResult.setCompanyNm(inspectionRequestPatched.getCompanyNm());
        inspectionResult.setCompanyPhoneNo(inspectionRequestPatched.getCompanyPhoneNo());
        inspectionResult.setInspectionContents(inspectionRequestPatched.getInspectionContents());
        
        deliveryrequestRepository.save(inspectionResult);
    }
    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverProcurementRequestCanceled_InspectionResultCanceled(@Payload ProcurementRequestCanceled procurementRequestCanceled){

        if(!procurementRequestCanceled.validate()) return;
        // Get Methods
                           

        // Sample Logic //
        System.out.println("\n\n##### listener InspectionResultCanceled : " + procurementRequestCanceled.toJson() + "\n\n");

        Deliveryrequest inspectionResult = deliveryrequestRepository.findByProcNo(procurementRequestCanceled.getProcNo());
        
        deliveryrequestRepository.delete(inspectionResult);
    }


    @StreamListener(KafkaProcessor.INPUT)
    public void whatever(@Payload String eventString){}


}