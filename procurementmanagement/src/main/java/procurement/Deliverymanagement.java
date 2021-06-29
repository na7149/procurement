package procurement;

import javax.persistence.*;
import org.springframework.beans.BeanUtils;
import java.util.List;
import java.util.Date;

@Entity
@Table(name="Deliverymanagement_table")
public class Deliverymanagement {

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
    private Boolean inspectionSuccFlag;

    @PostUpdate
    public void onPostUpdate(){
        DeliveryRequestPatched deliveryRequestPatched = new DeliveryRequestPatched();
        BeanUtils.copyProperties(this, deliveryRequestPatched);
        deliveryRequestPatched.publishAfterCommit();

    }
    @PostRemove
    public void onPostRemove(){
        ProcurementNoticeCanceled procurementNoticeCanceled = new ProcurementNoticeCanceled();
        BeanUtils.copyProperties(this, procurementNoticeCanceled);
        procurementNoticeCanceled.publishAfterCommit();

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
    public Boolean getInspectionSuccFlag() {
        return inspectionSuccFlag;
    }

    public void setInspectionSuccFlag(Boolean inspectionSuccFlag) {
        this.inspectionSuccFlag = inspectionSuccFlag;
    }




}
