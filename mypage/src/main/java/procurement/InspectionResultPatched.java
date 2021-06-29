package procurement;

public class InspectionResultPatched extends AbstractEvent {

    private Long id;
    private String procNo;
    private String companyNo;
    private String companyNm;
    private String companyPhoneNo;
    private Boolean inspectionSuccFlag;

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
    public Boolean getInspectionSuccFlag() {
        return inspectionSuccFlag;
    }

    public void setInspectionSuccFlag(Boolean inspectionSuccFlag) {
        this.inspectionSuccFlag = inspectionSuccFlag;
    }
}