package procurement;

import javax.persistence.*;
import org.springframework.beans.BeanUtils;
import java.util.List;
import java.util.Date;

@Entity
@Table(name="InspectionResult_table")
public class InspectionResult {

    @Id
    @GeneratedValue(strategy=GenerationType.AUTO)
    private Long id;
    private String procNo;
    private String companyNo;
    private String companyNm;
    private String companyPhoneNo;
    private String inspectionContents;
    private Boolean inspectionSuccFlag;

    @PostUpdate
    public void onPostUpdate(){
        InspectionResultPatched inspectionResultPatched = new InspectionResultPatched();
        BeanUtils.copyProperties(this, inspectionResultPatched);
        inspectionResultPatched.publishAfterCommit();

        //Following code causes dependency to external APIs
        // it is NOT A GOOD PRACTICE. instead, Event-Policy mapping is recommended.

        procurement.external.Deliverymanagement deliverymanagement = new procurement.external.Deliverymanagement();
        // mappings goes here
        Application.applicationContext.getBean(procurement.external.DeliverymanagementService.class)
            .announceInspectionResult(deliverymanagement);

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
    public Boolean getInspectionSuccFlag() {
        return inspectionSuccFlag;
    }

    public void setInspectionSuccFlag(Boolean inspectionSuccFlag) {
        this.inspectionSuccFlag = inspectionSuccFlag;
    }




}
