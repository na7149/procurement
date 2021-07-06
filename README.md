# procurement
물품조달 시스템 구축 과제

![image](https://user-images.githubusercontent.com/84000959/124288509-38d01f00-db8c-11eb-8a8f-d352ba6eabaa.png)

### Repositories

- https://github.com/na7149/procurement.git



### Table of contents

- [서비스 시나리오]

  - [기능적 요구사항]

  - [비기능적 요구사항]

  - [Microservice명]

- [분석/설계]

- [구현]

  - [DDD 의 적용]

  - [폴리글랏 퍼시스턴스]

  - [동기식 호출 과 Fallback 처리]

  - [비동기식 호출 / 시간적 디커플링 / 장애격리 / 최종 (Eventual) 일관성 테스트]

- [운영]

  - [Deploy]

  - [Config Map]

  - [Persistence Volume]
  
  - [Circuit Breaker]
   
  - [Autoscale (HPA)]

  - [Zero-Downtime deploy (Readiness Probe)] 

  - [Self-healing (Liveness Probe)]


# 서비스 시나리오

### 기능적 요구 사항

```
• 수요기관담당자는 조달요청서를 등록한다.
• 조달요청서는 납품관리 서비스로 전달(연계)된다.
• 조달청담당자는 조달요청서에 납품요구서 정보를 갱신한다.
• 납품요구 서가 물품납품 서비스로 전달(연계)된다.
• 조달업체담당자는 검사검수요청서를 등록한다.
• 검사검수요청서가 납품요구 서비스로 전달(연계)된다.
• 수요기관담당자는 검사검수요청서에 검사결과를 갱신한다.
• 검사결과가 갱신되면 납품관리 서비스에 검사결과 정보가 공지(갱신)된다.
• 수요기관담당자는 조달요청을 취소 할 수 있다.
• 조달요청이 취소되면 납품요구, 검사요청, 검사결과도 취소된다.
• 검사결과 정보 등록 시 조달업체 담당자에게 SMS를 발송한다.
※ 위 시나리오는 가상의 절차로, 실제 업무와 다를 수 있습니다.
```

### 비기능적 요구 사항

```
1. 트랜잭션
  - 검사결과가 등록되면 납품관리 서비스에 검사결과 정보가 등록되어야 한다. (Sync 호출)
2. 장애격리
  - 물품납품 서비스가 동작되지 않더라도 납품요구, 납품관리 서비스는 365일 24시간 받을 수 있어야 한다. Async (event-driven), Eventual Consistency
  - 납품요구 서비스가 과중되면 사용자를 잠시 동안 받지 않고 납품요구를 잠시 후에 하도록 유도한다. Circuit breaker, fallback
3. 성능
  - 조달업체는 납품현황조회 화면에서 검사 상태를 확인 할 수 있어야 한다.CQRS - 조회전용 서비스
```

참고 시나리오

![image](https://user-images.githubusercontent.com/84000959/124525282-ab195b80-de39-11eb-8f95-ce311407dfe5.png)


### Microservice명

```
납품요구 – procurementrequest
납품관리 - procurementmanagement
물품납품 - goodsdelivery
문자알림 - notification
CQRS - mypage
```


# 분석/설계

### AS-IS 조직 (Horizontally-Aligned)

![1  AS-IS조직](https://user-images.githubusercontent.com/84000922/122162394-7b1c0f80-ceae-11eb-95c4-8952596bb623.png)




### TO-BE 조직 (Vertically-Aligned)

![image](https://user-images.githubusercontent.com/84000959/124289090-e3484200-db8c-11eb-946f-f12c9e7c1a43.png)




### 이벤트 도출

![image](https://user-images.githubusercontent.com/84000959/124289178-fd822000-db8c-11eb-8bc3-bca55ac54b88.png)




### 부적격 이벤트 탈락

![image](https://user-images.githubusercontent.com/84000959/124289227-096de200-db8d-11eb-9741-7c6325dca180.png)

```
- 과정중 도출된 잘못된 도메인 이벤트들을 걸러내는 작업을 수행
- 검수결과등록됨, 세금계산서발급됨, 수수료납부됨 등 : 후행 시나리오라서 제외
- 조달업체선택됨, 조달요청메뉴선택됨, 검사현황조회됨 : UI 의 이벤트이지, 업무적인 의미의 이벤트가 아니라서 제외
- 문자발송됨 : 문자 발송 후 이벤트가 없어서 제외
```




### 액터, 커맨드 부착하여 읽기 좋게

![image](https://user-images.githubusercontent.com/84000959/124344035-103b3a00-dc0b-11eb-8f1a-ddbed796161c.png)




### 어그리게잇으로 묶기

![image](https://user-images.githubusercontent.com/84000959/124344023-f7328900-dc0a-11eb-8100-52e34b4fb379.png)

```
- 납품요구, 납품관리, 물품납품은 그와 연결된 command 와 event 들에 의하여 트랜잭션이 유지되어야 하는 단위로 그들 끼리 묶어줌
```




### 바운디드 컨텍스트로 묶기

![image](https://user-images.githubusercontent.com/84000959/124344029-01ed1e00-dc0b-11eb-8fc4-b9adac6526cf.png)

```
도메인 서열 분리
- Core Domain: 납품요구, 납품관리 : 없어서는 안될 핵심 서비스이며, 연견 Up-time SLA 수준을 99.999% 목표, 납품요구 서비스 배포주기는 1개월 1회 미만, 납풉관리 서비스 배포주기는 1주일 1회 미만
- Supporting Domain: 물품납품 : 경쟁력을 내기 위한 서비스이며, SLA 수준은 연간 60% 이상 uptime 목표, 배포주기는 각 팀의 자율이나 표준 스프린트 주기가 1주일 이므로 1주일 1회 이상을 기준으로 함. 
- General Domain: Notification : 문자알림 서비스는 3rd Party 외부 서비스를 사용하는 것이 경쟁력이 높음 (핑크색으로 이후 전환할 예정)
```




### 폴리시 부착, 이동 및 컨텍스트 매핑(점선은 Pub/Sub, 실선은 Req/Resp)

![image](https://user-images.githubusercontent.com/84000959/124289453-4f2aaa80-db8d-11eb-8008-3dfed1de46f8.png)



### 1차 완성본에 대한 기능적 요구사항을 커버하는지 검증 (1/2)

![image](https://user-images.githubusercontent.com/84000959/124289545-69648880-db8d-11eb-9cb6-89d27cd2f75a.png)

```
1) 수요기관담당자는 조달요청서를 등록한다. 조달요청서는 납품관리 서비스로 전달(연계)된다.
2) 조달청담당자는 조달요청서에 납품요구서 정보를 갱신한다. 납품요구 서가 물품납품 서비스로 전달(연계)된다.
3) 조달업체담당자는 검사검수요청서를 등록한다. 검사검수요청서가 납품요구 서비스로 전달(연계)된다.
4) 수요기관담당자는 검사검수요청서에 검사결과를 갱신한다. 검사결과가 갱신되면 납품관리 서비스에 검사결과 정보가 공지(갱신)된다.
```




### 1차 완성본에 대한 기능적 요구사항을 커버하는지 검증 (2/2)

![image](https://user-images.githubusercontent.com/84000959/124289625-7da88580-db8d-11eb-8702-ccec3e421ffe.png)

```
1) 조달요청이 취소되면 납품요구와 검사결과도 취소된다.
2) 납품요구가 취소되면 검사요청도 취소된다.
3) 검사결과 정보 등록 시 조달업체 담당자에게 SMS를 발송한다.
```




### 1차 완성본에 대한 비기능적 요구사항을 커버하는지 검증

![image](https://user-images.githubusercontent.com/84000959/124289788-a9c40680-db8d-11eb-815a-f407fedbc828.png)

```
1. 트랜잭션
  - 검사결과가 등록되면 납품관리 서비스에 검사결과 정보가 공지(갱신)되어야 한다. (Sync 호출)
2. 장애격리
  - 물품납품 서비스가 동작되지 않더라도 납품요구, 납품관리 서비스는 365일 24시간 받을 수 있어야 한다. Async (event-driven), Eventual Consistency
  - 납품요구 서비스가 과중되면 사용자를 잠시 동안 받지 않고 납품요구를 잠시 후에 하도록 유도한다. Circuit breaker, fallback
3. 성능
  - 조달업체는 납품현황조회 화면에서 검사 상태를 확인 할 수 있어야 한다.CQRS - 조회전용 서비스
```




### 헥사고날 아키텍처 다이어그램 도출

![image](https://user-images.githubusercontent.com/84000959/124289858-bba5a980-db8d-11eb-81d9-d3354ce8d72d.png)




### Git Organization / Repositories

![image](https://user-images.githubusercontent.com/84000959/124289904-c95b2f00-db8d-11eb-9557-4e8f0c329aab.png)




# 구현


(서비스 별 포트) 분석/설계 단계에서 도출된 헥사고날 아키텍처에 따라, 각 BC별로 대변되는 마이크로 서비스들을 스프링부트 등으로 구현하였다. 구현한 각 서비스를 로컬에서 실행하는 방법은 아래와 같다 (각자의 포트넘버는 8081 ~ 8085, 8088 이다)

```
cd procurementrequest
mvn spring-boot:run

cd procurementmanagement
mvn spring-boot:run 

cd goodsdelivery
mvn spring-boot:run  

cd notification
mvn spring-boot:run

cd mypage
mvn spring-boot:run

cd gateway
mvn spring-boot:run
```

## DDD 의 적용

- (Entity 예시) 각 서비스내에 도출된 핵심 Aggregate Root 객체를 Entity 로 선언하였다: (아래 예시는 납품요구 Entity). 이때 가능한 현업에서 사용하는 언어 (유비쿼터스 랭귀지)를 그대로 사용하려고 노력했다.

```
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
```
- (Repository 예시) Entity Pattern 과 Repository Pattern 을 적용하여 JPA 를 통하여 다양한 데이터소스 유형 (RDB or NoSQL) 에 대한 별도의 처리가 없도록 데이터 접근 어댑터를 자동 생성하기 위하여 Spring Data REST 의 RestRepository 를 적용하였다
```
package procurement;

import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(collectionResourceRel="inspectionResults", path="inspectionResults")
public interface InspectionResultRepository extends PagingAndSortingRepository<InspectionResult, Long>{

    InspectionResult findByProcNo(String procNo);
}
```

적용 후 REST API 의 테스트

- 수요기관담당자는 조달요청서를 등록한다. (Command-POST)
```
    http POST localhost:8082/deliveryrequests procNo=p01 procTitle=title01
    http GET http://localhost:8082/deliveryrequests/1
```
![image](https://user-images.githubusercontent.com/84000959/124300988-24931e80-db9a-11eb-9f3f-8afba52ee256.png)

  - 조달요청서는 납품관리 서비스로 전달(연계)된다. (Async-Policy)
```
    http GET http://localhost:8081/deliverymanagements/1
```
![image](https://user-images.githubusercontent.com/84000959/124301100-4ee4dc00-db9a-11eb-8e42-47055d3d6268.png)

  - 조달청담당자는 조달요청서에 납품요구서 정보를 갱신한다. (Command-PATCH)
```
    http PATCH http://localhost:8081/deliverymanagements/1 procNo=p01 companyNo=c01 companyNm=redbull
    http GET http://localhost:8081/deliverymanagements/1
```
![image](https://user-images.githubusercontent.com/84000959/124301154-60c67f00-db9a-11eb-9607-ac5a10e88a5f.png)

  - 납품요구서가 물품납품 서비스로 전달(연계)된다. (Async-Policy)
```
    http GET http://localhost:8083/goodsdeliveries/1
```
![image](https://user-images.githubusercontent.com/84000959/124301217-75a31280-db9a-11eb-87c4-456f4403674a.png)

  - 조달업체담당자는 검사검수요청서를 등록한다. (Command-PATCH)
```
    http PATCH http://localhost:8083/goodsdeliveries/1 procNo=p01 companyPhoneNo=010-1234-1234
    http GET http://localhost:8083/goodsdeliveries/1
```
![image](https://user-images.githubusercontent.com/84000959/124301272-8bb0d300-db9a-11eb-9341-cc06346e2040.png)

  - 검사검수요청서가 납품요구 서비스로 전달(연계)된다. (Async-Policy)
```
    http GET http://localhost:8082/inspectionResults/2
```
![image](https://user-images.githubusercontent.com/84000959/124301580-fd891c80-db9a-11eb-806a-6ae29a50c461.png)

  - 수요기관담당자는 검사검수요청서에 검사결과를 갱신한다. (Command-PATCH)
```
    http PATCH http://localhost:8082/inspectionResults/2 procNo=p01 inspectionSuccFlag=true
    http GET http://localhost:8082/inspectionResults/2
```
![image](https://user-images.githubusercontent.com/84000959/124303682-ad5f8980-db9d-11eb-8cc0-4dd64bad6483.png)

  - 검사결과가 notifaction 서비스로 전달(연계)된다. (Async-Policy)
```
    http GET http://localhost:8085/smsHistories/1
```
![image](https://user-images.githubusercontent.com/84000959/124303719-ba7c7880-db9d-11eb-88e4-3e86e8036459.png)

  - 검사결과가 갱신되면 납품관리 서비스에 검사결과 정보가 공지(갱신)된다. (Sync-Req/Res)
```
    http GET http://localhost:8081/deliverymanagements/1
```
![image](https://user-images.githubusercontent.com/84000959/124303785-ce27df00-db9d-11eb-9fd0-c7694d648fc7.png)

  - 납품관리 서비스 Down 시 납품요구 서비스의 검사결과 갱신도 실패한다. (Sync-Req/Res)
```
    http PATCH http://localhost:8082/inspectionresults/2 procNo=p01 inspectionSuccFlag=true
```
![image](https://user-images.githubusercontent.com/84000959/124302557-5311f900-db9c-11eb-9274-ed58471f98a0.png)

  - mypage 서비스에서 납품현황을 조회한다. (CQRS)
```
http GET localhost:8084/deliveryStatusInquiries
```
![image](https://user-images.githubusercontent.com/84000959/124303831-df70eb80-db9d-11eb-84f4-01cb3cc794f9.png)

  - gateway-납품현황조회(Gateway 8088포트로 진입점 통일)
```
http GET localhost:8088/deliveryStatusInquiries
```
![image](https://user-images.githubusercontent.com/84000959/124303877-ed267100-db9d-11eb-8d7e-a125da1965fb.png)



## 폴리글랏 퍼시스턴스

(H2DB, HSQLDB 사용) notification(문자알림) 서비스는 문자알림 이력이 많이 쌓일 수 있으므로 자바로 작성된 관계형 데이터베이스인 HSQLDB를 사용하기로 하였다. 이를 위해 pom.xml 파일에 아래 설정을 추가하였다.

```
# pom.xml
<dependency>
	<groupId>org.hsqldb</groupId>
    	<artifactId>hsqldb</artifactId>
	<scope>runtime</scope>
</dependency>
```
![image](https://user-images.githubusercontent.com/84000959/124303719-ba7c7880-db9d-11eb-88e4-3e86e8036459.png)

- 납품요구, 납품관리, 물품납품 등 나머지 서비스는 H2 DB를 사용한다.
```
<dependency>
	<groupId>com.h2database</groupId>
	<artifactId>h2</artifactId>
	<scope>runtime</scope>
</dependency>
```

## 동기식 호출 과 Fallback 처리

분석단계에서의 조건 중 하나로 검사결과등록(납품요구)->검사결과공지(납품관리) 간의 호출은 동기식 일관성을 유지하는 트랜잭션으로 처리하기로 하였다. 호출 프로토콜은 이미 앞서 Rest Repository 에 의해 노출되어있는 REST 서비스를 FeignClient 를 이용하여 호출하도록 한다.

- (동기호출-Req)검사결과공지 서비스를 호출하기 위하여 Stub과 (FeignClient) 를 이용하여 Service 대행 인터페이스 (Proxy) 를 구현 
```
# DeliverymanagementService.java
package procurement.external;

@FeignClient(name="procurementmanagement", url="http://localhost:8081", fallback=DeliverymanagementServiceFallback.class)
public interface DeliverymanagementService {
    
    @RequestMapping(method= RequestMethod.GET, path="/deliverymanagements/announceInspectionResult")
    public boolean announceInspectionResult(@RequestParam("procNo") String procNo, @RequestParam("companyNo") String companyNo, 
    @RequestParam("companyNm") String companyNm, @RequestParam("inspectionSuccFlag") Boolean inspectionSuccFlag);

}
```

- (Fallback) 검사결과갱신 서비스가 정상적으로 호출되지 않을 경우 Fallback 처리
```
package procurement.external;

import org.springframework.stereotype.Component;

@Component
public class DeliverymanagementServiceFallback implements DeliverymanagementService{

    @Override
    public boolean announceInspectionResult(String procNo,String companyNo, String companyNm, Boolean inspectionSuccFlag){
        
        System.out.println("★★★★★★★★★★★Circuit breaker has been opened. Fallback returned instead.★★★★★★★★★★★");
        return false;
    }
}
```

```
feign:
  hystrix:
    enabled: true
```

- (동기호출-Res) 검사결과공지 서비스 (정상 호출)
```
# DeliverymanagementController.java
package procurement;

 @RestController
 public class DeliverymanagementController {
    @Autowired
    DeliverymanagementRepository deliverymanagementRepository;

    @RequestMapping(value = "/deliverymanagements/announceInspectionResult",
       method = RequestMethod.GET,
       produces = "application/json;charset=UTF-8")
    public boolean announceInspectionResult(HttpServletRequest request, HttpServletResponse response) {
       boolean status = false;

       String procNo = String.valueOf(request.getParameter("procNo"));
       
       System.out.println("@@@@@@@@@@@@@@@@@companyNm@" + request.getParameter("companyNm"));
       
       Deliverymanagement deliverymanagement = deliverymanagementRepository.findByProcNo(procNo);

        if(deliverymanagement.getProcAgency() == null || "조달청".equals(deliverymanagement.getProcAgency()) == false){
            deliverymanagement.setCompanyNo(request.getParameter("companyNo"));
            deliverymanagement.setCompanyNm(request.getParameter("companyNm"));
            deliverymanagement.setInspectionSuccFlag(Boolean.parseBoolean(request.getParameter("inspectionSuccFlag")));

            deliverymanagementRepository.save(deliverymanagement);

            status = true;
       }

       return status;
    }

 }
```

- (동기호출-PostUpdate) 검사결과가 갱신된 직후(@PostUpdate) 검사결과 공지를 요청하도록 처리 (검사 성공이 아닌 경우, 이후 로직 스킵)
```
# BiddingExamination.java (Entity)

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
    }
```

- (동기호출-테스트) 동기식 호출에서는 호출 시간에 따른 타임 커플링이 발생하며, 납품관리 시스템이 장애가 나면 검사결과 등록도 못 한다는 것을 확인:

```
# 납품관리(deliverymanagement) 서비스를 잠시 내려놓음 (ctrl+c)

#검사결과 등록(PATCH) : Fail
http PATCH http://localhost:8082/inspectionresults/1 procNo=p01 inspectionSuccFlag=true

#납품관리 서비스 재기동
cd procurementmanagement
mvn spring-boot:run

#검사결과 등록(PATCH) : Success
http PATCH http://localhost:8082/inspectionresults/1 procNo=p01 inspectionSuccFlag=true
```

- 또한 과도한 요청시에 서비스 장애가 도미노 처럼 벌어질 수 있다. (서킷브레이커, 폴백 처리는 운영단계에서 설명한다.)




## 비동기식 호출 / 시간적 디커플링 / 장애격리 / 최종 (Eventual) 일관성 테스트


납품요구 서비스에 조달요청 등록된 후에 납품관리 서비스에 알려주는 행위는 동기식이 아니라 비 동기식으로 처리하여 데이터 연계를 위하여 조달요청 트랜잭션이 블로킹 되지 않도록 처리한다.
 
- (Publish) 이를 위하여 입찰공고 기록을 남긴 후에 곧바로 등록 되었다는 도메인 이벤트를 카프카로 송출한다(Publish)
 
```
    @PostPersist
    public void onPostPersist(){
        ProcurementRequestPosted procurementRequestPosted = new ProcurementRequestPosted();
        BeanUtils.copyProperties(this, procurementRequestPosted);
        procurementRequestPosted.publishAfterCommit();

    }
```
- (Subscribe-등록) 납품관리 서비스에서는 조달요청 등록 이벤트를 수신하면 조달요청 정보를 등록하는 정책을 처리하도록 PolicyHandler를 구현한다:

```
    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverProcurementRequestPosted_ReceiveProcurementRequest(@Payload ProcurementRequestPosted procurementRequestPosted){

        if(!procurementRequestPosted.validate()) return;

        System.out.println("\n\n##### listener ReceiveProcurementRequest : " + procurementRequestPosted.toJson() + "\n\n");

        Deliverymanagement deliverymanagement = new Deliverymanagement();
        deliverymanagement.setProcNo(procurementRequestPosted.getProcNo());
        deliverymanagement.setProcTitle(procurementRequestPosted.getProcTitle());
        deliverymanagement.setProcContents(procurementRequestPosted.getProcContents());
        deliverymanagement.setProcPrice(procurementRequestPosted.getProcPrice());
        deliverymanagement.setProcAgency(procurementRequestPosted.getProcAgency());
        deliverymanagement.setProcQty(procurementRequestPosted.getProcQty());

        deliverymanagementRepository.save(deliverymanagement);
    }
```
- (Subscribe-취소) 납품관리 서비스에서는 조달요청 등록이 취소됨 이벤트를 수신하면 조달요청 정보를 삭제하는 정책을 처리하도록 PolicyHandler를 구현한다:
  
```
    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverProcurementRequestCanceled_CancelProcurementNotice(@Payload ProcurementRequestCanceled procurementRequestCanceled){

        if(!procurementRequestCanceled.validate()) return;

        Deliverymanagement deliverymanagement = deliverymanagementRepository.findByProcNo(procurementRequestCanceled.getProcNo());
        
        deliverymanagementRepository.delete(deliverymanagement);
    }
```

- (장애격리) 납품요구, 납품관리 서비스는 물품납품 서비스와 완전히 분리되어 있으며, 이벤트 수신에 따라 처리되기 때문에, 물품납품 서비스가 유지보수로 인해 잠시 내려간 상태라도 납품요구, 납품관리 서비스에 영향이 없다:
```
# 물품납품 서비스를 잠시 내려놓음 (ctrl+c)

# 조달요청서 등록 : Success
http POST localhost:8082/deliveryrequests procNo=p01 procTitle=title01
#조달요청서에 납품요구서 정보를 갱신 : Success
http PATCH http://localhost:8081/deliverymanagements/1 procNo=p01 companyNo=c01 companyNm=redbull

#납품관리에서 검사결과 갱신 여부 확인
http GET http://localhost:8081/deliverymanagements/1     # 검사결과 갱신 안 됨 확인

#물품납품 서비스 기동
cd goodsdelivery
mvn spring-boot:run

#검사요청 등록 : Success
http PATCH http://localhost:8083/goodsdeliveries/1 procNo=p01 companyPhoneNo=010-1234-1234

#검사결과 등록 : Success
http PATCH http://localhost:8082/inspectionresults/1 procNo=p01 inspectionSuccFlag=true

#납품관리에서 검사결과 갱신 여부 확인
http GET http://localhost:8081/deliverymanagements/1     # 검사결과 갱신됨 확인
```

# 운영:

컨테이너화된 마이크로서비스의 자동 배포/조정/관리를 위한 쿠버네티스 환경 운영

## Deploy

- GitHub 에서 로컬로 소스 clone
```
git clone --recurse-submodules https://github.com/na7149/procurement.git
```

- azure login
```
az login

az acr login --name user06acr
az aks get-credentials --resource-group user06-rsrcgrp --name user06-aks
az acr show --name user06acr --query loginServer --output table
```

- (필요시) Azure AKS에 ACR Attach 설정
```
az aks update -n user06-aks -g user06-rsrcgrp --attach-acr user06acr
```

- 기본 namespace 지정 및 namespace 생성
```
kubectl config set-context --current --namespace=procurement
kubectl create ns procurement
```

- (필요시) 기존 delete svc,deployment 전체 삭제
```
kubectl delete svc,deployment --all
kubectl get all
watch kubectl get all
```

- 배포진행
1.procurement/procurementrequest/kubernetes/deployment.yml 파일 수정 (procurementmanagement/goodsdelivery/mypage/notification/gateway 동일)

![image](https://user-images.githubusercontent.com/84000959/124421574-b5394c80-dd9c-11eb-95b0-c1666a757eb0.png)

2.procurement/procurementrequest/kubernetes/service.yaml 파일 수정 (procurementmanagement/goodsdelivery/mypage/notification 동일)

![image](https://user-images.githubusercontent.com/84000959/124378596-3507de00-dced-11eb-8c0a-e7a98f1804ce.png)

3.procurement/gateway/kubernetes/service.yaml 파일 수정

![image](https://user-images.githubusercontent.com/84000959/124378607-48b34480-dced-11eb-8730-674f13820ed5.png)


- 각 서비스 폴더에서 ACR 컨테이너이미지 빌드 및 배포 (procurementmanagement/goodsdelivery/mypage/notification/gateway 동일)
```
cd procurementrequest
mvn package
az acr build --registry user06acr --image user06acr.azurecr.io/procurementrequest:v1 .
cd kubernates
kubectl apply -f deployment.yml
kubectl apply -f service.yaml
```

- 나머지 서비스에 대해서도 동일하게 등록을 진행함
```
az acr build --registry user06acr --image user06acr.azurecr.io/procurementmanagement:v1 .
az acr build --registry user06acr --image user06acr.azurecr.io/goodsdelivery:v1 .
az acr build --registry user06acr --image user06acr.azurecr.io/mypage:v1  .
az acr build --registry user06acr --image user06acr.azurecr.io/notification:v1  .
az acr build --registry user06acr --image user06acr.azurecr.io/gateway:v1 .
```

- 배포결과 확인
``` 
kubectl get all
``` 
![image](https://user-images.githubusercontent.com/84000959/124437074-a27d4280-ddb1-11eb-8bf8-d873b21c591c.png)


- Kafka 설치
``` 
 Lab 환경에서 생성 시
curl https://raw.githubusercontent.com/helm/helm/master/scripts/get > get_helm.sh
chmod 700 get_helm.sh
./get_helm.sh

kubectl --namespace kube-system create sa tiller 
kubectl create clusterrolebinding tiller --clusterrole cluster-admin --serviceaccount=kube-system:tiller
helm init --service-account tiller

helm repo add incubator https://charts.helm.sh/incubator
helm repo update

kubectl create ns kafka
helm install --name my-kafka --namespace kafka incubator/kafka

kubectl get all -n kafka

-- 로컬 환경에서 생성 시
--kubectl --namespace kube-system create sa tiller 
--kubectl create clusterrolebinding tiller --clusterrole cluster-admin --serviceaccount=kube-system:tiller
--
--helm repo add incubator https://charts.helm.sh/incubator
--helm repo update
--kubectl create ns kafka
--helm install my-kafka --namespace kafka incubator/kafka
--
--kubectl get svc my-kafka -n kafka

-- topic 신규 생성
--kubectl -n kafka exec my-kafka-0 -- /usr/bin/kafka-topics --zookeeper my-kafka-zookeeper:2181 --topic procurement --create --partitions 1 --replication-factor 1
--kubectl -n kafka exec my-kafka-0 -- /usr/bin/kafka-topics --zookeeper my-kafka-zookeeper:2181 --list
-- 기존에 설치된 것 지우려면
--kubectl delete namespace kafka
--helm del --purge my-kafka
``` 

## Config Map
ConfigMap을 사용하여 변경가능성이 있는 설정을 관리

- 납품요구(procurementrequest) 서비스에서 동기호출(Req/Res방식)로 연결되는 납품관리(procurementmanagement) 서비스 url 정보 일부를 ConfigMap을 사용하여 구현

- 파일 수정
  - 납품관리서비스 요청 소스 (procurement/procurementrequest/src/main/java/procurement/external/DeliverymanagementService.java)

```
package procurement.external;

@FeignClient(name="procurementmanagement", url="http://${api.url.procurement}:8080", fallback=DeliverymanagementServiceFallback.class)
public interface DeliverymanagementService {
    
    @RequestMapping(method= RequestMethod.GET, path="/deliverymanagements/announceInspectionResult")
    public boolean announceInspectionResult(@RequestParam("procNo") String procNo, @RequestParam("companyNo") String companyNo, 
    @RequestParam("companyNm") String companyNm, @RequestParam("inspectionSuccFlag") Boolean inspectionSuccFlag);

}
```

- Yaml 파일 수정
  - application.yml (procurement/procurementrequest/src/main/resources/application.yml)
```
api:
  url:
    procurement: ${procurement-url}
```
  - deploy yml (procurement/procurementrequest/kubernetes/deployment.yml)
```
          env:
            - name: procurement-url
              valueFrom:
                configMapKeyRef:
                  name: procurement-cm
                  key: url
```

- Config Map 생성 및 생성 확인
```
kubectl create configmap procurement-cm --from-literal=url=procurementmanagement
kubectl get cm
-- kubectl delete configmap procurement-cm
```

![image](https://user-images.githubusercontent.com/84000959/124436688-339fe980-ddb1-11eb-80ad-006953548983.png)

```
kubectl get cm procurement-cm -o yaml
```

![image](https://user-images.githubusercontent.com/84000959/124436778-4d413100-ddb1-11eb-92a3-e0c2a77b212b.png)


## Persistence Volume
Persistence Volume 생성, Mount, 로그 파일 생성
- procurementrequest-pvc.yml : PVC 생성 파일
```
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: v-disk
  namespace: procurement
spec:
  accessModes:
  - ReadWriteMany
  storageClassName: azurefile
  resources:
    requests:
      storage: 1Gi
```

- deployment.yml : Container에 Volumn Mount
```
      volumeMounts:
            - name: volume
              mountPath: "/mnt/azure"
      volumes:
      - name: volume
        persistentVolumeClaim:
          claimName: procurementrequest-disk
```

- application.yml : PVC Mount 경로
```
logging:
  level:
    root: info
  file: /mnt/azure/logs/procurementrequest.log
```

- 마운트 경로에 logging file 생성 확인
```
$ kubectl exec -it pod/procurementrequest-785dd46db4-hcct8 -n procurement -- /bin/sh
$ cd /mnt/azure/logs
$ tail -n 20 -f procurementrequest.log
```
![image](https://user-images.githubusercontent.com/84000959/124458128-97cda800-ddc7-11eb-967e-0d10b5d0c420.png)

## Circuit Breaker
서킷 브레이킹 프레임워크의 선택: Spring FeignClient + Istio를 설치하여, procurement namespace에 주입하여 구현함
시나리오는 검사결과갱신–>검사결과공지 연결을 RESTful Request/Response 로 연동하여 구현이 되어있고, 검사결과갱신 요청이 과도할 경우 CB 를 통하여 장애격리

- Istio 다운로드 및 PATH 추가, 설치, namespace에 istio주입
```
$ curl -L https://istio.io/downloadIstio | ISTIO_VERSION=1.7.1 TARGET_ARCH=x86_64 sh -
※ istio v1.7.1은 Kubernetes 1.16이상에서만 동작
```

- istio 설치
```
$ istioctl install --set profile=demo --set hub=gcr.io/istio-release
※ Docker Hub Rate Limiting 우회 설정
```

- procurement namespace에 istio주입
```
$ kubectl label namespace procurement istio-injection=enabled
```

- Virsual Service 생성 (Timeout 3초 설정)
```
kubectl apply -f procurementrequest-istio.yaml
```
- /procurement/procurementrequest/kubernetes/procurementrequest-istio.yaml 파일
```
apiVersion: networking.istio.io/v1alpha3
kind: VirtualService
metadata:
  name: vs-procurementrequest-network-rule
  namespace: procurement
spec:
  hosts:
  - procurementrequest
  http:
  - route:
    - destination:
        host: procurementrequest
    timeout: 3s
```

- (필요시) VirtualService 삭제
```
kubectl get VirtualService
kubectl delete VirtualService vs-procurementmanagement-network-rule
```

- procurementrequest 서비스 재배포 후 Pod에 CB 부착 확인
![image](https://user-images.githubusercontent.com/84000959/124481640-74b0f180-dde3-11eb-9302-bc88065c8b83.png)

- siege 생성 (로드제너레이터 설치)
```
kubectl apply -f - <<EOF
apiVersion: v1
kind: Pod
metadata:
  name: siege
  namespace: procurement
spec:
  containers:
  - name: siege
    image: apexacme/siege-nginx
EOF
```
![image](https://user-images.githubusercontent.com/84000959/124437529-1ddef400-ddb2-11eb-84b6-a1c15d54a672.png)


- 부하테스터 siege 툴을 통한 서킷 브레이커 동작 확인(동시사용자 100명, 10초 동안 실시)
```
kubectl exec -it pod/siege  -c siege -n procurement -- /bin/bash
siege -c100 -t10S -v --content-type "application/json" 'http://procurementrequest:8080/deliveryrequests/1 PATCH {"procNo":"t01","companyNo":"c01","companyNm":"hehheh99","inspectionSuccFlag":"true"}'
```
![image](https://user-images.githubusercontent.com/84000959/124494137-71246700-ddf1-11eb-9be4-48456667153d.png)
![image](https://user-images.githubusercontent.com/84000959/124494196-800b1980-ddf1-11eb-9877-33bb16548e72.png)
![image](https://user-images.githubusercontent.com/84000959/124494262-94e7ad00-ddf1-11eb-8898-ee9865026ba4.png)

    - 운영시스템은 죽지 않고 지속적으로 CB 에 의하여 적절히 회로가 열림과 닫힘이 벌어지면서 자원을 보호하고 있음을 보여줌.
    - 99.79% 정상적으로 처리되었음.

## Autoscale (HPA)
앞서 CB(Circuit breaker)는 시스템을 안정되게 운영할 수 있게 해줬지만 사용자의 요청을 100% 받아들여주지 못했기 때문에 이에 대한 보완책으로 자동화된 확장 기능을 적용하고자 한다.

- 리소스에 대한 사용량 정의(procurement/procurementmanagement/kubernetes/deployment.yml)

![image](https://user-images.githubusercontent.com/84000959/124421819-342e8500-dd9d-11eb-9f83-953c92b496b0.png)

- Autoscale 설정 (request값의 20%를 넘어서면 Replica를 10개까지 동적으로 확장)
```
kubectl autoscale deployment procurementmanagement --cpu-percent=20 --min=1 --max=10
```
![image](https://user-images.githubusercontent.com/84000959/124437404-00118f00-ddb2-11eb-80b4-31478d1e2304.png)

-- (필요시) horizontalpodautoscaler 삭제
```
kubectl delete horizontalpodautoscaler procurementmanagement -n procurement
```

- 부하발생 (50명 동시사용자, 30초간 부하)
```
kubectl exec -it pod/siege  -c siege -n procurement -- /bin/bash
siege -c50 -t30S -v --content-type "application/json" 'http://procurementmanagement:8080/deliverymanagements POST {"procNo":"pp01","procTitle":"ppTitle"}'
```
- 모니터링 (부하증가로 스케일아웃되어지는 과정을 별도 창에서 모니터링)
```
watch kubectl get all
```
- 자동스케일아웃으로 Availablity 100% 결과 확인 (시간이 좀 흐른 후 스케일 아웃이 벌어지는 것을 확인, siege의 로그를 보아도 전체적인 성공률이 높아진 것을 확인함)

- 로그 확인
```
kubectl logs -f pod/procurementmanagement-69444dbc9-z6pvp -c procurementmanagement
```

- (필요시) Pod 크기 조정
```
kubectl scale --replicas=1 deployment/procurementmanagement
```

1.테스트전

![image](https://user-images.githubusercontent.com/84000959/124495802-869a9080-ddf3-11eb-836e-b33e18cc6785.png)

2.테스트후

![image](https://user-images.githubusercontent.com/84000959/124495372-fc522c80-ddf2-11eb-9789-e18934c985aa.png)

3.부하발생 결과

![image](https://user-images.githubusercontent.com/84000959/124495436-10962980-ddf3-11eb-8402-7eba921b3829.png)



## Zero-Downtime deploy (Readiness Probe)
쿠버네티스는 각 컨테이너의 상태를 주기적으로 체크(Health Check)해서 문제가 있는 컨테이너는 서비스에서 제외한다.

- deployment.yml에 readinessProbe 설정 후 미설정 상태 테스트를 위해 주석처리함 
  depolyment.yml(procurement/procurementmanagement/kubernetes/deployment.yml)
```
readinessProbe:
httpGet:
  path: '/actuator/health'
  port: 8080
initialDelaySeconds: 10
timeoutSeconds: 2
periodSeconds: 5
failureThreshold: 10
```

- deployment.yml에서 readiness 설정 제거 후, 배포중 siege 테스트 진행

![image](https://user-images.githubusercontent.com/84000959/124499995-ec8a1680-ddf9-11eb-95b7-969f43edccfb.png)

```
kubectl exec -it pod/siege  -c siege -n procurement -- /bin/bash
siege -c100 -t5S -v --content-type "application/json" 'http://procurementmanagement:8080/deliverymanagements POST {"procNo":"pp01","procTitle":"ppTitle"}'
```

1.배포 중 부하테스트 수행 시 POD 상태
배포 중인 POD들과 정상 실행중인 POD 존재
hpa 설정에 의해 target 지수 초과하여 POD scale-out 진행됨

![image](https://user-images.githubusercontent.com/84000959/124501096-eb59e900-ddfb-11eb-98b9-fd4a32959a69.png)

2.배포 중 부하테스트 수행 결과(siege)
배포가 진행되는 동안 부하테스트를 진행한 결과, 정상 실행중인 pod로의 요청은 성공(201), 배포중인 pod로의 요청은 실패(503 - Service Unavailable) 확인

![image](https://user-images.githubusercontent.com/84000959/124501180-09274e00-ddfc-11eb-8c7d-34eb1b44bd89.png)


- deployment.yml에 readinessProbe 설정 후 부하발생 및 Availability 100% 확인

![image](https://user-images.githubusercontent.com/84000959/124502308-3117b100-ddfe-11eb-89ad-5548be5c3d9f.png)

1.배포 중 부하테스트 수행 시 POD 상태
배포 중인 POD들과 정상 실행중인 POD 존재

![image](https://user-images.githubusercontent.com/84000959/124502103-c6667580-ddfd-11eb-9cb2-8f1784087690.png)

2.배포 중 부하테스트 수행 결과(siege)
readiness 정상 적용 후, Availability 100% 확인

![image](https://user-images.githubusercontent.com/84000959/124502144-e007bd00-ddfd-11eb-8059-8f23ec42ee11.png)


## Self-healing (Liveness Probe)
쿠버네티스는 각 컨테이너의 상태를 주기적으로 체크(Health Check)해서 문제가 있는 컨테이너는 자동으로재시작한다.

- depolyment.yml 파일의 path 및 port를 잘못된 값으로 변경
  depolyment.yml(procurement/procurementmanagement/kubernetes/deployment.yml)
```
 livenessProbe:
    httpGet:
        path: '/actuator/failed'
        port: 8090
      initialDelaySeconds: 30
      timeoutSeconds: 2
      periodSeconds: 5
      failureThreshold: 5
```

- liveness 설정 적용되어 컨테이너 재시작 되는 것을 확인
  Retry 시도 확인 (pod 생성 "RESTARTS" 숫자가 늘어나는 것을 확인) 

![image](https://user-images.githubusercontent.com/84000959/124503030-a6d04c80-ddff-11eb-922c-ee15f9ceefac.png)

- depolyment.yml 파일의 path 및 port를 정상으로 원복
  depolyment.yml(procurement/procurementmanagement/kubernetes/deployment.yml)
```
 livenessProbe:
    httpGet:
        path: '/actuator/health'
        port: 8080
      initialDelaySeconds: 120
      timeoutSeconds: 2
      periodSeconds: 5
      failureThreshold: 5
```

- liveness 설정 적용되어 컨테이너 재시작 되지 않는 것 확인

![image](https://user-images.githubusercontent.com/84000959/124503549-c74cd680-de00-11eb-8532-28c5a2338ce5.png)



## The End.
