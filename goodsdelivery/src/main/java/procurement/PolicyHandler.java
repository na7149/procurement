package procurement;

import procurement.config.kafka.KafkaProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

@Service
public class PolicyHandler{
    @Autowired GoodsdeliveryRepository goodsdeliveryRepository;

    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverDeliveryRequestPatched_ReceiveDeliveryRequest(@Payload DeliveryRequestPatched deliveryRequestPatched){

        if(!deliveryRequestPatched.validate()) return;
        // Get Methods


        // Sample Logic //
        System.out.println("\n\n##### listener ReceiveDeliveryRequest : " + deliveryRequestPatched.toJson() + "\n\n");

        Goodsdelivery goodsdelivery = new Goodsdelivery();
        goodsdelivery.setProcNo(deliveryRequestPatched.getProcNo());

        goodsdeliveryRepository.save(goodsdelivery);
    }
    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverProcurementNoticeCanceled_CancelInspectionRequest(@Payload ProcurementNoticeCanceled procurementNoticeCanceled){

        if(!procurementNoticeCanceled.validate()) return;
        // Get Methods


        // Sample Logic //
        System.out.println("\n\n##### listener CancelInspectionRequest : " + procurementNoticeCanceled.toJson() + "\n\n");

        Goodsdelivery goodsdelivery = goodsdeliveryRepository.findByProcNo(procurementNoticeCanceled.getProcNo());
        
        goodsdeliveryRepository.delete(goodsdelivery);
    }


    @StreamListener(KafkaProcessor.INPUT)
    public void whatever(@Payload String eventString){}


}
