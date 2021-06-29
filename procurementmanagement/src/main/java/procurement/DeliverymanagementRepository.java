package procurement;

import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(collectionResourceRel="deliverymanagements", path="deliverymanagements")
public interface DeliverymanagementRepository extends PagingAndSortingRepository<Deliverymanagement, Long>{


}
