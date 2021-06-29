package procurement;

public class ProcurementNoticeCanceled extends AbstractEvent {

    private Long id;
    private String procNo;

    public ProcurementNoticeCanceled(){
        super();
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
}