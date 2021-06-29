package procurement;

import javax.persistence.*;
import org.springframework.beans.BeanUtils;
import java.util.List;
import java.util.Date;

@Entity
@Table(name="Goodsdelivery_table")
public class Goodsdelivery {

    @Id
    @GeneratedValue(strategy=GenerationType.AUTO)
    private Long id;
    private String procNo;
    private String companyNo;
    private String companyNm;
    private String companyPhoneNo;
    private String inspectionContents;

    @PostUpdate
    public void onPostUpdate(){
        InspectionRequestPatched inspectionRequestPatched = new InspectionRequestPatched();
        BeanUtils.copyProperties(this, inspectionRequestPatched);
        inspectionRequestPatched.publishAfterCommit();

    }
    @PreUpdate
    public void onPreUpdate(){
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
    public String getProcNo() {
        return procNo;
    }

    public void setProcNo(String procNo) {
        this.procNo = procNo;
    }
    public String getCompanyNo() {
        return companyNo;
    }

    public void setCompanyNo(String companyNo) {
        this.companyNo = companyNo;
    }
    public String getCompanyNm() {
        return companyNm;
    }

    public void setCompanyNm(String companyNm) {
        this.companyNm = companyNm;
    }
    public String getCompanyPhoneNo() {
        return companyPhoneNo;
    }

    public void setCompanyPhoneNo(String companyPhoneNo) {
        this.companyPhoneNo = companyPhoneNo;
    }
    public String getInspectionContents() {
        return inspectionContents;
    }

    public void setInspectionContents(String inspectionContents) {
        this.inspectionContents = inspectionContents;
    }




}
