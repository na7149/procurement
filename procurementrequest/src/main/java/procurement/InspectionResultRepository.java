package procurement;

import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(collectionResourceRel="inspectionResults", path="inspectionResults")
public interface InspectionResultRepository extends PagingAndSortingRepository<InspectionResult, Long>{

    InspectionResult findByProcNo(String procNo);


}
