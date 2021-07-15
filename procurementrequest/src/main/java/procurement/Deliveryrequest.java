package procurement;

import javax.persistence.*;
import org.springframework.beans.BeanUtils;

@Entity
@Table(name="Deliveryrequest_table")
public class Deliveryrequest {

    @Id
    @GeneratedValue(strategy=GenerationType.AUTO)
    private Long id;
    private String procNo;
    private String procTitle;
    private String procContents;
    private Integer procPrice;
    private String procAgency;
    private Double procQty;

    private String companyNo;
    private String companyNm;
    private String companyPhoneNo;
    private String inspectionContents;
    private Boolean inspectionSuccFlag;

    @PreUpdate
    public void onPreUpdate() throws Exception{

        //Following code causes dependency to external APIs
        // it is NOT A GOOD PRACTICE. instead, Event-Policy mapping is recommended.

        // 검사 성공이 아니면 Skip.
        if(getInspectionSuccFlag() == false) return;

            // mappings goes here
            boolean isUpdated = ProcurementrequestApplication.applicationContext.getBean(procurement.external.DeliverymanagementService.class)
            .announceInspectionResult(getProcNo(), getCompanyNo(), getCompanyNm(), getInspectionSuccFlag());

            if(isUpdated == false){
                throw new Exception("납품관리 서비스에 검사결과 정보가 공지되지 않음");
            }

    }

    @PostUpdate
    public void onPostUpdate() throws Exception{
        InspectionResultPatched inspectionResultPatched = new InspectionResultPatched();
        BeanUtils.copyProperties(this, inspectionResultPatched);
        inspectionResultPatched.publishAfterCommit();
    }
    
    @PostPersist
    public void onPostPersist(){
        ProcurementRequestPosted procurementRequestPosted = new ProcurementRequestPosted();
        BeanUtils.copyProperties(this, procurementRequestPosted);
        procurementRequestPosted.publishAfterCommit();

    }
    @PostRemove
    public void onPostRemove(){
        ProcurementRequestCanceled procurementRequestCanceled = new ProcurementRequestCanceled();
        BeanUtils.copyProperties(this, procurementRequestCanceled);
        procurementRequestCanceled.publishAfterCommit();

    }
    @PrePersist
    public void onPrePersist(){
    }
    @PreRemove
    public void onPreRemove(){
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
    public String getProcTitle() {
        return procTitle;
    }

    public void setProcTitle(String procTitle) {
        this.procTitle = procTitle;
    }
    public String getProcContents() {
        return procContents;
    }

    public void setProcContents(String procContents) {
        this.procContents = procContents;
    }
    public Integer getProcPrice() {
        return procPrice;
    }

    public void setProcPrice(Integer procPrice) {
        this.procPrice = procPrice;
    }
    public String getProcAgency() {
        return procAgency;
    }

    public void setProcAgency(String procAgency) {
        this.procAgency = procAgency;
    }
    public Double getProcQty() {
        return procQty;
    }

    public void setProcQty(Double procQty) {
        this.procQty = procQty;
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