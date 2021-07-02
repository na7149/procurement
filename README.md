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

  - [Autoscale (HPA)]

  - [Config Map]

  - [Zero-Downtime deploy (Readiness Probe)] 

  - [Self-healing (Liveness Probe)]

  - [Circuit Breaker]

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
• 검사결과가 갱신되면 납품관리 서비스에 검사결과 정보가 갱신(공지)된다.
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

![image](https://user-images.githubusercontent.com/84000959/124289285-1ab6ee80-db8d-11eb-802e-723c4b54c776.png)




### 어그리게잇으로 묶기

![image](https://user-images.githubusercontent.com/84000959/124289322-230f2980-db8d-11eb-8767-a8b7b4fd67b0.png)

```
- 납품요구, 납품관리, 물품납품은 그와 연결된 command 와 event 들에 의하여 트랜잭션이 유지되어야 하는 단위로 그들 끼리 묶어줌
```




### 바운디드 컨텍스트로 묶기

![image](https://user-images.githubusercontent.com/84000959/124289379-3621f980-db8d-11eb-8f35-e7bc5bdd9ca2.png)

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
4) 수요기관담당자는 검사검수요청서에 검사결과를 갱신한다. 검사결과가 갱신되면 납품관리 서비스에 검사결과 정보가 갱신(공지)된다.
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
  - 검사결과가 등록되면 납품관리 서비스에 검사결과 정보가 등록되어야 한다. (Sync 호출)
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

- 적용 후 REST API 의 테스트
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

  - 납품요구 서가 물품납품 서비스로 전달(연계)된다. (Async-Policy)
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

  - 검사결과가 갱신되면 납품관리 서비스에 검사결과 정보가 갱신(공지)된다. (Sync-Req/Res)
```
    http GET http://localhost:8081/deliverymanagements/1
```
![image](https://user-images.githubusercontent.com/84000959/124303785-ce27df00-db9d-11eb-9fd0-c7694d648fc7.png)

  - 납품관리 서비스 Down 시 납품요구 서비스의 검사결과 갱신도 실패한다. (Sync-Req/Res)
```
    http PATCH http://localhost:8082/inspectionresults/1 procNo=p01 inspectionSuccFlag=true
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

(H2DB, HSQLDB 사용) Notification(문자알림) 서비스는 문자알림 이력이 많이 쌓일 수 있으므로 자바로 작성된 관계형 데이터베이스인 HSQLDB를 사용하기로 하였다. 이를 위해 pom.xml 파일에 아래 설정을 추가하였다.

```
# pom.xml
<dependency>
	<groupId>org.hsqldb</groupId>
    	<artifactId>hsqldb</artifactId>
	<scope>runtime</scope>
</dependency>
```
![image](https://user-images.githubusercontent.com/84000959/122328060-e7a81480-cf69-11eb-9955-954f88b7ec1b.png)

- 입찰관리, 입찰참여, 입찰심사 등 나머지 서비스는 H2 DB를 사용한다.
```
<dependency>
	<groupId>com.h2database</groupId>
	<artifactId>h2</artifactId>
	<scope>runtime</scope>
</dependency>
```

## 동기식 호출 과 Fallback 처리

분석단계에서의 조건 중 하나로 심사결과등록(입찰심사)->낙찰자정보등록(입찰관리) 간의 호출은 동기식 일관성을 유지하는 트랜잭션으로 처리하기로 하였다. 호출 프로토콜은 이미 앞서 Rest Repository 에 의해 노출되어있는 REST 서비스를 FeignClient 를 이용하여 호출하도록 한다. 

- (동기호출-Req)낙찰자정보 등록 서비스를 호출하기 위하여 Stub과 (FeignClient) 를 이용하여 Service 대행 인터페이스 (Proxy) 를 구현 
```
# (BiddingExamination) BiddingManagementService.java
package bidding.external;

@FeignClient(name="BiddingManagement", url="http://${api.url.bidding}:8080", fallback=BiddingManagementServiceFallback.class)
public interface BiddingManagementService {

    @RequestMapping(method= RequestMethod.GET, path="/biddingManagements/registSucessBidder")
    public boolean registSucessBidder(@RequestParam("noticeNo") String noticeNo,
    @RequestParam("succBidderNm") String succBidderNm, @RequestParam("phoneNumber") String phoneNumber);

}
```

- (Fallback) 낙찰자정보 등록 서비스가 정상적으로 호출되지 않을 경우 Fallback 처리
```
# (BiddingExamination) BiddingManagementServiceFallback.java
package bidding.external;

import org.springframework.stereotype.Component;

@Component
public class BiddingManagementServiceFallback implements BiddingManagementService{

    @Override
    public boolean registSucessBidder(String noticeNo,String succBidderNm, String phoneNumber){
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

- (동기호출-Res) 낙찰자자정보 등록 서비스 (정상 호출)
```
# (BiddingManagement) BiddingManagementController.java
package bidding;

 @RestController
 public class BiddingManagementController {

    @Autowired
    BiddingManagementRepository biddingManagementRepository;

    @RequestMapping(value = "/biddingManagements/registSucessBidder",
       method = RequestMethod.GET,
       produces = "application/json;charset=UTF-8")
    public boolean registSucessBidder(HttpServletRequest request, HttpServletResponse response) {
       boolean status = false;

       String noticeNo = String.valueOf(request.getParameter("noticeNo"));
       
       BiddingManagement biddingManagement = biddingManagementRepository.findByNoticeNo(noticeNo);

       if(biddingManagement.getDemandOrgNm() == null || "조달청".equals(biddingManagement.getDemandOrgNm()) == false){
            biddingManagement.setSuccBidderNm(request.getParameter("succBidderNm"));
            biddingManagement.setPhoneNumber(request.getParameter("phoneNumber"));

            biddingManagementRepository.save(biddingManagement);

            status = true;
       }

       return status;
    }

 }
```

- (동기호출-PostUpdate) 심사결과가 등록된 직후(@PostUpdate) 낙찰자정보 등록을 요청하도록 처리 (낙찰자가 아닌 경우, 이후 로직 스킵)
```
# BiddingExamination.java (Entity)

    @PostUpdate
    public void onPostUpdate(){
        // 낙찰업체가 아니면 Skip.
        if(getSuccessBidderFlag() == false) return;

        try{
            // mappings goes here
            boolean isUpdated = BiddingExaminationApplication.applicationContext.getBean(bidding.external.BiddingManagementService.class)
            .registSucessBidder(getNoticeNo(), getCompanyNm(), getPhoneNumber());

            if(isUpdated == false){
                throw new Exception("입찰관리 서비스의 입찰공고에 낙찰자 정보가 갱신되지 않음");
            }
        }catch(java.net.ConnectException ce){
            throw new Exception("입찰관리 서비스 연결 실패");
        }catch(Exception e){
            throw new Exception("입찰관리 서비스 처리 실패");
        }
```

- (동기호출-테스트) 동기식 호출에서는 호출 시간에 따른 타임 커플링이 발생하며, 입찰관리 시스템이 장애가 나면 입찰심사 등록도 못 한다는 것을 확인:

```
# 입찰관리(BiddingManagement) 서비스를 잠시 내려놓음 (ctrl+c)

#심사결과 등록 : Fail
http PATCH http://localhost:8083/biddingExaminations/1 noticeNo=n01 participateNo=p01 successBidderFlag=true

#입찰관리 서비스 재기동
cd BiddingManagement
mvn spring-boot:run

#심사결과 등록 : Success
http PATCH http://localhost:8083/biddingExaminations/1 noticeNo=n01 participateNo=p01 successBidderFlag=true
```

- 또한 과도한 요청시에 서비스 장애가 도미노 처럼 벌어질 수 있다. (서킷브레이커, 폴백 처리는 운영단계에서 설명한다.)




## 비동기식 호출 / 시간적 디커플링 / 장애격리 / 최종 (Eventual) 일관성 테스트


입찰공고가 등록된 후에 입찰참여 시스템에 알려주는 행위는 동기식이 아니라 비 동기식으로 처리하여 입찰참여 시스템의 처리를 위하여 입찰공고 트랜잭션이 블로킹 되지 않도록 처리한다.
 
- (Publish) 이를 위하여 입찰공고 기록을 남긴 후에 곧바로 등록 되었다는 도메인 이벤트를 카프카로 송출한다(Publish)
 
```
@Entity
@Table(name="BiddingManagement_table")
public class BiddingManagement {

 ...
    @PostPersist
    public void onPostPersist(){
        NoticeRegistered noticeRegistered = new NoticeRegistered();
        BeanUtils.copyProperties(this, noticeRegistered);
        noticeRegistered.publishAfterCommit();
    }
```
- (Subscribe-등록) 입찰참여 서비스에서는 입찰공고 등록됨 이벤트를 수신하면 입찰공고 번호를 등록하는 정책을 처리하도록 PolicyHandler를 구현한다:

```
@Service
public class PolicyHandler{

    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverNoticeRegistered_RecieveBiddingNotice(@Payload NoticeRegistered noticeRegistered){

        if(!noticeRegistered.validate()) return;

        if(noticeRegistered.isMe()){
            BiddingParticipation biddingParticipation = new BiddingParticipation();
            biddingParticipation.setNoticeNo(noticeRegistered.getNoticeNo());

            biddingParticipationRepository.save(biddingParticipation);
        }
    }

```
- (Subscribe-취소) 입찰참여 서비스에서는 입찰공고가 취소됨 이벤트를 수신하면 입찰참여 정보를 삭제하는 정책을 처리하도록 PolicyHandler를 구현한다:
  
```
@Service
public class PolicyHandler{
    @Autowired BiddingParticipationRepository biddingParticipationRepository;

    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverNoticeCanceled_CancelBiddingParticipation(@Payload NoticeCanceled noticeCanceled){

        if(!noticeCanceled.validate()) return;

        if(noticeCanceled.isMe()){
            BiddingParticipation biddingParticipation = biddingParticipationRepository.findByNoticeNo(noticeCanceled.getNoticeNo());
            biddingParticipationRepository.delete(biddingParticipation);
        }
            
    }

```

- (장애격리) 입찰관리, 입찰참여 시스템은 입찰심사 시스템과 완전히 분리되어 있으며, 이벤트 수신에 따라 처리되기 때문에, 입찰심사 시스템이 유지보수로 인해 잠시 내려간 상태라도 입찰관리, 입찰참여 서비스에 영향이 없다:
```
# 입찰심사 서비스 (BiddingExamination) 를 잠시 내려놓음 (ctrl+c)

#입찰공고 등록 : Success
http POST localhost:8081/biddingManagements noticeNo=n33 title=title33
#입찰참여 등록 : Success
http PATCH http://localhost:8082/biddingParticipations/2 noticeNo=n33 participateNo=p33 companyNo=c33 companyNm=doremi33 phoneNumber=010-1234-1234

#입찰관리에서 낙찰업체명 갱신 여부 확인
http localhost:8081/biddingManagements/2     # 낙찰업체명 갱신 안 됨 확인

#입찰심사 서비스 기동
cd BiddingExamination
mvn spring-boot:run

#심사결과 등록 : Success
http PATCH http://localhost:8083/biddingExaminations/2 noticeNo=n33 participateNo=p33 successBidderFlag=true

#입찰관리에서 낙찰업체명 갱신 여부 확인
http localhost:8081/biddingManagements/2     # 낙찰업체명 갱신됨 확인
```

# 운영:

컨테이너화된 마이크로서비스의 자동 배포/조정/관리를 위한 쿠버네티스 환경 운영

## Deploy

- GitHub 와 연결 후 로컬빌드를 진행 진행
```
	cd team
	mkdir sourcecode
	cd sourcecode
	git clone --recurse-submodules https://github.com/21-2-1team/bidding03.git
	
	cd bidding
	cd BiddingExamination
	mvn package
	
	cd ../BiddingManagement
	mvn package
	
	cd ../BiddingParticipation
	mvn package
	
	cd ../MyPage
	mvn package
	
	
	cd ../Notification
	mvn package
	
	
	cd ../gateway
        mvn package
```
- namespace 등록 및 변경
```
kubectl config set-context --current --namespace=bidding  --> bidding namespace 로 변경

kubectl create ns bidding
```

- ACR 컨테이너이미지 빌드
```
az acr build --registry user01skccacr --image user01skccacr.azurecr.io/biddingexamination:latest .
```
![image](https://user-images.githubusercontent.com/70736001/122502677-096cce80-d032-11eb-96e7-84a8024ab45d.png)

나머지 서비스에 대해서도 동일하게 등록을 진행함
```
az acr build --registry user01skccacr --image user01skccacr.azurecr.io/biddingmanagement:latest .
az acr build --registry user01skccacr --image user01skccacr.azurecr.io/biddingparticipation:latest .
az acr build --registry user01skccacr --image user01skccacr.azurecr.io/biddingparticipation:latest .
az acr build --registry user01skccacr --image user01skccacr.azurecr.io/mypage:latest  .
az acr build --registry user01skccacr --image user01skccacr.azurecr.io/notification:latest  .
az acr build --registry user01skccacr --image user01skccacr.azurecr.io/gateway:latest .
```

- 배포진행

1.bidding/BiddingExamination/kubernetes/deployment.yml 파일 수정 (BiddingManagement/BiddingParticipation/MyPage/Notification/gateway 동일)

![image](https://user-images.githubusercontent.com/70736001/122512566-011d8f00-d044-11eb-8bd5-91d939f7ab1b.png)

2.bidding/BiddingExamination/kubernetes/service.yaml 파일 수정 (BiddingManagement/BiddingParticipation/MyPage/Notification 동일)

![image](https://user-images.githubusercontent.com/70736001/122512673-26aa9880-d044-11eb-8587-38f8cd261326.png)

3.bidding/gateway/kubernetes/service.yaml 파일 수정

![image](https://user-images.githubusercontent.com/70736001/122503123-da0a9180-d032-11eb-9283-224d7860c9c3.png)

4. 배포작업 수행
``` 
	cd gateway/kubernetes
	kubectl apply -f deployment.yml
	kubectl apply -f service.yaml
	
	cd ../../BiddingExamination/kubernetes
	kubectl apply -f deployment.yml
	kubectl apply -f service.yaml
	
	cd ../../BiddingManagement/kubernetes
	kubectl apply -f deployment.yml
	kubectl apply -f service.yaml
	
	
	cd ../../BiddingParticipation/kubernetes
	kubectl apply -f deployment.yml
	kubectl apply -f service.yaml
	
	
	cd ../../MyPage/kubernetes
	kubectl apply -f deployment.yml
	kubectl apply -f service.yaml
	
	
	cd ../../Notification/kubernetes
	kubectl apply -f deployment.yml
	kubectl apply -f service.yaml
``` 

5. 배포결과 확인
``` 
kubectl get all
``` 
![image](https://user-images.githubusercontent.com/70736001/122503307-2b1a8580-d033-11eb-83fc-63b0f2154e3b.png)

- Kafka 설치
``` 
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
``` 
설치 후 서비스 재기동

## Autoscale (HPA)
앞서 CB(Circuit breaker)는 시스템을 안정되게 운영할 수 있게 해줬지만 사용자의 요청을 100% 받아들여주지 못했기 때문에 이에 대한 보완책으로 자동화된 확장 기능을 적용하고자 한다.

- 리소스에 대한 사용량 정의(bidding/BiddingManagement/kubernetes/deployment.yml)
![image](https://user-images.githubusercontent.com/70736001/122503960-49cd4c00-d034-11eb-8ab4-b322e7383cc0.png)

- Autoscale 설정 (request값의 20%를 넘어서면 Replica를 10개까지 동적으로 확장)
```
kubectl autoscale deployment biddingmanagement --cpu-percent=20 --min=1 --max=10
```

- siege 생성 (로드제너레이터 설치)
```
kubectl apply -f - <<EOF
apiVersion: v1
kind: Pod
metadata:
  name: siege
  namespace: bidding
spec:
  containers:
  - name: siege
    image: apexacme/siege-nginx
EOF
```
- 부하발생 (50명 동시사용자, 30초간 부하)
```
kubectl exec -it pod/siege  -c siege -n bidding -- /bin/bash
siege -c50 -t30S -v --content-type "application/json" 'http://52.231.8.61:8080/biddingManagements POST {"noticeNo":1,"title":"AAA"}'
```
- 모니터링 (부하증가로 스케일아웃되어지는 과정을 별도 창에서 모니터링)
```
watch kubectl get al
```
- 자동스케일아웃으로 Availablity 100% 결과 확인 (시간이 좀 흐른 후 스케일 아웃이 벌어지는 것을 확인, siege의 로그를 보아도 전체적인 성공률이 높아진 것을 확인함)

1.테스트전

![image](https://user-images.githubusercontent.com/70736001/122504322-0aebc600-d035-11eb-883f-35110d9d0457.png)

2.테스트후

![image](https://user-images.githubusercontent.com/70736001/122504349-1e972c80-d035-11eb-814e-a5ab909215c4.png)

3.부하발생 결과

![image](https://user-images.githubusercontent.com/70736001/122504389-31a9fc80-d035-11eb-976e-f43261d1a8c2.png)

## Config Map
ConfigMap을 사용하여 변경가능성이 있는 설정을 관리

- 입찰심사(BiddingExamination) 서비스에서 동기호출(Req/Res방식)로 연결되는 입찰관리(BiddingManagement) 서비스 url 정보 일부를 ConfigMap을 사용하여 구현

- 파일 수정
  - 입찰심사 소스 (BiddingExamination/src/main/java/bidding/external/BiddingManagementService.java)

![image](https://user-images.githubusercontent.com/70736001/122505096-9dd93000-d036-11eb-91b7-0ec57b6e1b10.png)

- Yaml 파일 수정
  - application.yml (BiddingExamination/src/main/resources/application.yml)
  - deploy yml (BiddingExamination/kubernetes/deployment.yml)

![image](https://user-images.githubusercontent.com/70736001/122505177-c5c89380-d036-11eb-91b3-f399547b50ff.png)

- Config Map 생성 및 생성 확인
```
kubectl create configmap bidding-cm --from-literal=url=BiddingManagement
kubectl get cm
```

![image](https://user-images.githubusercontent.com/70736001/122505221-dc6eea80-d036-11eb-8757-b97f8d75baff.png)

```
kubectl get cm bidding-cm -o yaml
```

![image](https://user-images.githubusercontent.com/70736001/122505270-f6103200-d036-11eb-8c96-513f95448989.png)

```
kubectl get pod
```

![image](https://user-images.githubusercontent.com/70736001/122505313-0fb17980-d037-11eb-9b57-c0d14f468a1c.png)


## Zero-Downtime deploy (Readiness Probe)
쿠버네티스는 각 컨테이너의 상태를 주기적으로 체크(Health Check)해서 문제가 있는 컨테이너는 서비스에서 제외한다.

- deployment.yml에 readinessProbe 설정 후 미설정 상태 테스트를 위해 주석처리함 
```
readinessProbe:
httpGet:
  path: '/biddingManagements'
  port: 8080
initialDelaySeconds: 10
timeoutSeconds: 2
periodSeconds: 5
failureThreshold: 10
```

- deployment.yml에서 readinessProbe 미설정 상태로 siege 부하발생

![image](https://user-images.githubusercontent.com/70736001/122505873-2906f580-d038-11eb-86b8-2f8388f82dd1.png)

```
kubectl exec -it pod/siege  -c siege -n bidding -- /bin/bash
siege -c100 -t5S -v --content-type "application/json" 'http://20.194.120.4:8080/biddingManagements POST {"noticeNo":1,"title":"AAA"}
```
1.부하테스트 전

![image](https://user-images.githubusercontent.com/70736001/122506020-75eacc00-d038-11eb-99df-4a4b90478bc3.png)

2.부하테스트 후

![image](https://user-images.githubusercontent.com/70736001/122506060-84d17e80-d038-11eb-8449-b94b28a0f385.png)

3.생성중인 Pod 에 대한 요청이 들어가 오류발생

![image](https://user-images.githubusercontent.com/70736001/122506129-a03c8980-d038-11eb-8822-5ec57926b900.png)

- 정상 실행중인 biddingmanagement으로의 요청은 성공(201),비정상 적인 요청은 실패(503 - Service Unavailable) 확인

- hpa 설정에 의해 target 지수 초과하여 biddingmanagement scale-out 진행됨

- deployment.yml에 readinessProbe 설정 후 부하발생 및 Availability 100% 확인

![image](https://user-images.githubusercontent.com/70736001/122506358-2527a300-d039-11eb-84cb-62eb09687bda.png)

1.부하테스트 전

![image](https://user-images.githubusercontent.com/70736001/122506400-3c669080-d039-11eb-8e5e-a4f76b0e2956.png)

2.부하테스트 후

![image](https://user-images.githubusercontent.com/70736001/122506421-4be5d980-d039-11eb-92a2-44e7827299bf.png)

3.readiness 정상 적용 후, Availability 100% 확인

![image](https://user-images.githubusercontent.com/70736001/122506471-61f39a00-d039-11eb-9077-608f375e27f3.png)


## Self-healing (Liveness Probe)
쿠버네티스는 각 컨테이너의 상태를 주기적으로 체크(Health Check)해서 문제가 있는 컨테이너는 자동으로재시작한다.

- depolyment.yml 파일의 path 및 port를 잘못된 값으로 변경
  depolyment.yml(BiddingManagement/kubernetes/deployment.yml)
```
 livenessProbe:
    httpGet:
        path: '/biddingmanagement/failed'
        port: 8090
      initialDelaySeconds: 30
      timeoutSeconds: 2
      periodSeconds: 5
      failureThreshold: 5
```




![image](https://user-images.githubusercontent.com/70736001/122506714-d75f6a80-d039-11eb-8bd0-223490797b58.png)

- liveness 설정 적용되어 컨테이너 재시작 되는 것을 확인
  Retry 시도 확인 (pod 생성 "RESTARTS" 숫자가 늘어나는 것을 확인) 

1.배포 전

![image](https://user-images.githubusercontent.com/70736001/122506797-fb22b080-d039-11eb-9a0b-754e0fea45b2.png)

2.배포 후

![image](https://user-images.githubusercontent.com/70736001/122506831-0c6bbd00-d03a-11eb-880c-dc8d3e00798f.png)

## Circuit Breaker
서킷 브레이킹 프레임워크의 선택: Spring FeignClient + Hystrix 옵션을 사용하여 구현함
시나리오는 심사결과등록(입찰심사:BiddingExamination)-->낙찰자정보등록(입찰관리:BiddingManagement) 시의 연결을 RESTful Request/Response 로 연동하여 구현이 되어있고, 낙찰자정보등록이 과도할 경우 CB 를 통하여 장애격리.


- Hystrix 를 설정: 요청처리 쓰레드에서 처리시간이 1000ms가 넘어서기 시작하면 CB 작동하도록 설정

**application.yml (BiddingExamination)**
```
feign:
  hystrix:
    enabled: true

hystrix:
  command:
    default:
      execution.isolation.thread.timeoutInMilliseconds: 1000
```
![image](https://user-images.githubusercontent.com/70736001/122508631-3a9ecc00-d03d-11eb-9bce-a786225df40f.png)

- 피호출 서비스(입찰관리:biddingmanagement) 의 임의 부하 처리 - 800ms에서 증감 300ms 정도하여 800~1100 ms 사이에서 발생하도록 처리
BiddingManagementController.java
```
req/res를 처리하는 피호출 function에 sleep 추가

	try {
	   Thread.sleep((long) (800 + Math.random() * 300));
	} catch (InterruptedException e) {
	   e.printStackTrace();
	}
```
![image](https://user-images.githubusercontent.com/70736001/122508689-5609d700-d03d-11eb-9e08-8eadc904d391.png)

- req/res 호출하는 위치가 onPostUpdate에 있어 실제로 Data Update가 발생하지 않으면 호출이 되지 않는 문제가 있어 siege를 2개 실행하여 Update가 지속적으로 발생하게 처리 함
```
siege -c2 –t20S  -v --content-type "application/json" 'http://20.194.120.4:8080/biddingExaminations/1 PATCH {"noticeNo":"n01","participateNo":"p01","successBidderFlag":"true"}'
siege -c2 –t20S  -v --content-type "application/json" 'http://20.194.120.4:8080/biddingExaminations/1 PATCH {"noticeNo":"n01","participateNo":"p01","successBidderFlag":"false"}'
```
![image](https://user-images.githubusercontent.com/70736001/122508763-7b96e080-d03d-11eb-90f8-8380277cdc17.png)
