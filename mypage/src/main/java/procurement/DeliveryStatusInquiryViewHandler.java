package procurement;

import procurement.config.kafka.KafkaProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Service
public class DeliveryStatusInquiryViewHandler {


    @Autowired
    private DeliveryStatusInquiryRepository deliveryStatusInquiryRepository;

    @StreamListener(KafkaProcessor.INPUT)
    public void whenProcurementRequestPosted_then_CREATE_1 (@Payload ProcurementRequestPosted procurementRequestPosted) {
        try {

            if (!procurementRequestPosted.validate()) return;

            // view 객체 생성
            DeliveryStatusInquiry deliveryStatusInquiry = new DeliveryStatusInquiry();
            // view 객체에 이벤트의 Value 를 set 함
            deliveryStatusInquiry.setProcNo(procurementRequestPosted.getProcNo());
            deliveryStatusInquiry.setProcTitle(procurementRequestPosted.getProcTitle());
            // view 레파지 토리에 save
            deliveryStatusInquiryRepository.save(deliveryStatusInquiry);

        }catch (Exception e){
            e.printStackTrace();
        }
    }


    @StreamListener(KafkaProcessor.INPUT)
    public void whenInspectionResultPatched_then_UPDATE_1(@Payload InspectionResultPatched inspectionResultPatched) {
        try {
            if (!inspectionResultPatched.validate()) return;
                // view 객체 조회

                    List<DeliveryStatusInquiry> deliveryStatusInquiryList = deliveryStatusInquiryRepository.findByProcNo(inspectionResultPatched.getProcNo());
                    for(DeliveryStatusInquiry deliveryStatusInquiry : deliveryStatusInquiryList){
                    // view 객체에 이벤트의 eventDirectValue 를 set 함
                    deliveryStatusInquiry.setCompanyNm(inspectionResultPatched.getCompanyNm());
                    deliveryStatusInquiry.setInspectionSuccFlag(inspectionResultPatched.getInspectionSuccFlag());
                // view 레파지 토리에 save
                deliveryStatusInquiryRepository.save(deliveryStatusInquiry);
                }

        }catch (Exception e){
            e.printStackTrace();
        }
    }

    @StreamListener(KafkaProcessor.INPUT)
    public void whenProcurementRequestCanceled_then_DELETE_1(@Payload ProcurementRequestCanceled procurementRequestCanceled) {
        try {
            if (!procurementRequestCanceled.validate()) return;
            // view 레파지 토리에 삭제 쿼리
            deliveryStatusInquiryRepository.deleteByProcNo(procurementRequestCanceled.getProcNo());
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}

