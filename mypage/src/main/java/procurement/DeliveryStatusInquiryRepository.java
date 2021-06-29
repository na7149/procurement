package procurement;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DeliveryStatusInquiryRepository extends CrudRepository<DeliveryStatusInquiry, Long> {

    List<DeliveryStatusInquiry> findByProcNo(String procNo);

        void deleteByProcNo(String procNo);
}