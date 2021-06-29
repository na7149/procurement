package procurement;

import procurement.config.kafka.KafkaProcessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    }
    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverProcurementRequestCanceled_CancelProcurementNotice(@Payload ProcurementRequestCanceled procurementRequestCanceled){

        if(!procurementRequestCanceled.validate()) return;
        // Get Methods


        // Sample Logic //
        System.out.println("\n\n##### listener CancelProcurementNotice : " + procurementRequestCanceled.toJson() + "\n\n");
    }


    @StreamListener(KafkaProcessor.INPUT)
    public void whatever(@Payload String eventString){}


}
