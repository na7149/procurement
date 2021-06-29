package procurement;

import javax.persistence.*;
import java.util.List;

@Entity
@Table(name="DeliveryStatusInquiry_table")
public class DeliveryStatusInquiry {

        @Id
        @GeneratedValue(strategy=GenerationType.AUTO)
        private Long id;
        private String procNo;
        private String procTitle;
        private String companyNm;
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
        public String getProcTitle() {
            return procTitle;
        }

        public void setProcTitle(String procTitle) {
            this.procTitle = procTitle;
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
