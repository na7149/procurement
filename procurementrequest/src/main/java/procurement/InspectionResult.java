package procurement;

import javax.persistence.*;
import org.springframework.beans.BeanUtils;

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
    public void onPostUpdate() throws Exception{

        //Following code causes dependency to external APIs
        // it is NOT A GOOD PRACTICE. instead, Event-Policy mapping is recommended.

        // 검사 성공이 아니면 Skip.
        if(getInspectionSuccFlag() == false) return;

        try{
            // mappings goes here
            boolean isUpdated = ProcurementrequestApplication.applicationContext.getBean(procurement.external.DeliverymanagementService.class)
            .announceInspectionResult(getProcNo(), getCompanyNo(), getCompanyNm(), getInspectionSuccFlag());

            if(isUpdated == false){
                throw new Exception("납품관리 서비스에 검사결과 정보가 공지되지 않음");
            }
        }catch(java.net.ConnectException ce){
            throw new Exception("납품관리 서비스 연결 실패");
        }catch(Exception e){
            throw new Exception("납품관리 서비스 처리 실패");
        }

        InspectionResultPatched inspectionResultPatched = new InspectionResultPatched();
        BeanUtils.copyProperties(this, inspectionResultPatched);
        inspectionResultPatched.publishAfterCommit();
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
