package procurement;

import javax.persistence.*;
import org.springframework.beans.BeanUtils;
import java.util.List;
import java.util.Date;

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




}
