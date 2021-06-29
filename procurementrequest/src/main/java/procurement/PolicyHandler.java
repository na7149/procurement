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
    @Autowired DeliveryrequestRepository deliveryrequestRepository;
    @Autowired InspectionResultRepository inspectionResultRepository;

    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverInspectionRequestPatched_ReceiveInspectionRequest(@Payload InspectionRequestPatched inspectionRequestPatched){

        if(!inspectionRequestPatched.validate()) return;
        // Get Methods


        // Sample Logic //
        System.out.println("\n\n##### listener ReceiveInspectionRequest : " + inspectionRequestPatched.toJson() + "\n\n");
    }
    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverProcurementRequestCanceled_InspectionResultCanceled(@Payload ProcurementRequestCanceled procurementRequestCanceled){

        if(!procurementRequestCanceled.validate()) return;
        // Get Methods


        // Sample Logic //
        System.out.println("\n\n##### listener InspectionResultCanceled : " + procurementRequestCanceled.toJson() + "\n\n");
    }


    @StreamListener(KafkaProcessor.INPUT)
    public void whatever(@Payload String eventString){}


}
