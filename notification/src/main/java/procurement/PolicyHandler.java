package procurement;

import procurement.config.kafka.KafkaProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

@Service
public class PolicyHandler{
    @Autowired SmsHistoryRepository smsHistoryRepository;

    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverInspectionResultPatched_SendSms(@Payload InspectionResultPatched inspectionResultPatched){

        if(!inspectionResultPatched.validate()) return;
        // Get Methods


        // Sample Logic //
        System.out.println("\n\n##### listener SendSms : " + inspectionResultPatched.toJson() + "\n\n");

        SmsHistory smsHistory = new SmsHistory();
        smsHistory.setPhoneNo(inspectionResultPatched.getCompanyPhoneNo());
        smsHistory.setContents("검수 완료 되었습니다~~~");

        smsHistoryRepository.save(smsHistory);
    }


    @StreamListener(KafkaProcessor.INPUT)
    public void whatever(@Payload String eventString){}


}
