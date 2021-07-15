package procurement;

import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(collectionResourceRel="deliveryrequests", path="deliveryrequests")
public interface DeliveryrequestRepository extends PagingAndSortingRepository<Deliveryrequest, Long>{

    Deliveryrequest findByProcNo(String procNo);


}
