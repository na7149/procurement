package procurement;

import procurement.config.kafka.KafkaProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

@Service
public class PolicyHandler{
    @Autowired DeliverymanagementRepository deliverymanagementRepository;

    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverProcurementRequestPosted_ReceiveProcurementRequest(@Payload ProcurementRequestPosted procurementRequestPosted){

        if(!procurementRequestPosted.validate()) return;
        // Get Methods


        // Sample Logic //
        System.out.println("\n\n##### listener ReceiveProcurementRequest : " + procurementRequestPosted.toJson() + "\n\n");

        Deliverymanagement deliverymanagement = new Deliverymanagement();
        deliverymanagement.setProcNo(procurementRequestPosted.getProcNo());
        deliverymanagement.setProcTitle(procurementRequestPosted.getProcTitle());
        deliverymanagement.setProcContents(procurementRequestPosted.getProcContents());
        deliverymanagement.setProcPrice(procurementRequestPosted.getProcPrice());
        deliverymanagement.setProcAgency(procurementRequestPosted.getProcAgency());
        deliverymanagement.setProcQty(procurementRequestPosted.getProcQty());

        deliverymanagementRepository.save(deliverymanagement);
    }
    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverProcurementRequestCanceled_CancelProcurementNotice(@Payload ProcurementRequestCanceled procurementRequestCanceled){

        if(!procurementRequestCanceled.validate()) return;
        // Get Methods


        // Sample Logic //
        System.out.println("\n\n##### listener CancelProcurementNotice : " + procurementRequestCanceled.toJson() + "\n\n");

        Deliverymanagement deliverymanagement = deliverymanagementRepository.findByProcNo(procurementRequestCanceled.getProcNo());
        
        deliverymanagementRepository.delete(deliverymanagement);
    }


    @StreamListener(KafkaProcessor.INPUT)
    public void whatever(@Payload String eventString){}


}
