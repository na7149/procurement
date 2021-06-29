package procurement;

import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(collectionResourceRel="goodsdeliveries", path="goodsdeliveries")
public interface GoodsdeliveryRepository extends PagingAndSortingRepository<Goodsdelivery, Long>{


}
