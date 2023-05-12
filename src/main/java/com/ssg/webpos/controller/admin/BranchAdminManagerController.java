package com.ssg.webpos.controller.admin;

import com.ssg.webpos.domain.Order;
import com.ssg.webpos.domain.SettlementDay;
import com.ssg.webpos.domain.SettlementMonth;
import com.ssg.webpos.dto.SettlementDayReportDTO;
import com.ssg.webpos.dto.SettlementMonthDetailRequestDTO;
import com.ssg.webpos.dto.SettlementMonthReportDTO;
import com.ssg.webpos.service.SettlementDayService;
import com.ssg.webpos.service.SettlementMonthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.swing.text.DateFormatter;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/v1/manager") // 팀장님한테 점장 로그인 구현 기능 받으면 manager에서 branchadmin-manager로 변경
@Slf4j
@RequiredArgsConstructor
public class BranchAdminManagerController {
    // manager 기능 : 재고 조회, 수정, 삭제, 재고 리포트(주말 재고 현황) 제출
    //               정산 조회, 정산 내역 리포트(일별, 월별 정산내역) 제출
    // 리포트가 이미 제출된 경우 버튼을 비활성화
    private final SettlementDayService settlementDayService;
    private final SettlementMonthService settlementMonthService;



    //조건을 명시한 상태에서 settlement_day 내역 GET (조건 : store_id=1L, settlement_date="2023-05-08")
    @GetMapping("/settlementDay-constant-date")
    public List<SettlementDayReportDTO> settlementDayByConstantDate() { //점장 로그인 기능 구현시 팀장님 코드 활용
//        팀장님 작성 BranchAdmin branchAdmin = principalDetail.getUser(); 이거 활용해서 로그인한 사람의 소속 가게 store_id불러올 수 있다.
//        팀장님 작성 Long storeId = branchAdmin.getStore().getId();
//        지금 해야하는 것 : 요청 받은 날짜의 결제 내역을 나오게 하는 것
        List<SettlementDay> settlementDays = settlementDayService.selectByStoreIdAndDay(1L,"2023-05-08");
        List<SettlementDayReportDTO> reportDTOs = new ArrayList<>();

        for (SettlementDay settlementDay : settlementDays) {
            SettlementDayReportDTO reportDTO = new SettlementDayReportDTO();
            reportDTO.setSettlementDayId(settlementDay.getId());
            reportDTO.setSettlementPrice(settlementDay.getSettlementPrice());
            reportDTO.setSettlementDate(settlementDay.getSettlementDate());
            reportDTO.setStoreId(settlementDay.getStore().getId());
            reportDTO.setStoreName(settlementDay.getStore().getName());
            reportDTO.setCreatedDate(settlementDay.getCreatedDate());
            reportDTOs.add(reportDTO);
        }

        return reportDTOs;
    }

    // 일별 내역 조회
    // 날짜를 req 받는 상태에서 settlement_day 내역 GET
    // 예시 URL : http://localhost:8080/api/v1/manager/test2?settlementDate=2023-05-08
    // next level : 점장 로그인시 점장의 해당 store_id 정산내역 표시
    @GetMapping("/settlementDay-variable-date")
    public List<SettlementDayReportDTO> settlementDayByVariableDate(@RequestParam("settlementDate")String SettlementDate) throws DateTimeParseException {
        // 20022-05-08같이 테이블에 없는 날짜 내역이 올 경우 에러 -> DateTimeParseException
        //
        try {
            List<SettlementDay> settlementDays = settlementDayService.selectByStoreIdAndDay(1L,SettlementDate);
            List<SettlementDayReportDTO> reportDTOs = new ArrayList<>();

            for (SettlementDay settlementDay : settlementDays) {
                SettlementDayReportDTO reportDTO = new SettlementDayReportDTO();
                reportDTO.setSettlementDayId(settlementDay.getId());
                reportDTO.setSettlementPrice(settlementDay.getSettlementPrice());
                reportDTO.setSettlementDate(settlementDay.getSettlementDate());
                reportDTO.setStoreId(settlementDay.getStore().getId());
                reportDTO.setStoreName(settlementDay.getStore().getName());
                reportDTO.setCreatedDate(settlementDay.getCreatedDate());
                reportDTOs.add(reportDTO);
            }

            return reportDTOs;
        } catch (Exception e) {
            return Collections.emptyList();
        }

    }

    // 특정 기간 일별 내역 조회
    @GetMapping("/settlementDay-variable-range")
    public List<SettlementDayReportDTO> settlementDayByVariableRange(@RequestParam("startDate") String startDate,
                                              @RequestParam("endDate") String endDate) throws DateTimeParseException {
        try {
            List<SettlementDay> settlementDays = settlementDayService.selectByStoreIdAndDayBetween(1L,startDate,endDate);
            List<SettlementDayReportDTO> reportDTOs = new ArrayList<>();

            for (SettlementDay settlementDay : settlementDays) {
                SettlementDayReportDTO reportDTO = new SettlementDayReportDTO();
                reportDTO.setSettlementDayId(settlementDay.getId());
                reportDTO.setSettlementPrice(settlementDay.getSettlementPrice());
                reportDTO.setSettlementDate(settlementDay.getSettlementDate());
                reportDTO.setStoreId(settlementDay.getStore().getId());
                reportDTO.setStoreName(settlementDay.getStore().getName());
                reportDTO.setCreatedDate(settlementDay.getCreatedDate());
                reportDTOs.add(reportDTO);
            }

            return reportDTOs;
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    // 월별 내역 조회
    // 날짜를 req 받는 상태에서 settlement_day 내역 GET
    // 예시 URL : http://localhost:8080/api/v1/manager/test3?settlementDate=2023-05-08
    // next level : 점장 로그인시 점장의 해당 store_id 정산내역 표시
    @GetMapping("/settlementMonth-variable-date-yyyy-MM")
    public List<SettlementMonthReportDTO> settlementMonthByVariableDateyyyyMM(@RequestParam("settlementDate")String SettlementDate) throws DateTimeParseException {
        // 2테이블에 없는 날짜 내역이 올 경우 에러 -> DateTimeParseException
        // 기존 정산 내역에 있는 정산일자를 yyyy-MM 형식으로 바꾸고 storeName을 추가했습니다.
        try {
            List<SettlementMonth> settlementMonths = settlementMonthService.selectByStoreIdAndDay(1L,SettlementDate);
            List<SettlementMonthReportDTO> reportDTOs = new ArrayList<>();

            for (SettlementMonth settlementMonth : settlementMonths) {
                SettlementMonthReportDTO reportDTO = new SettlementMonthReportDTO();
                reportDTO.setSettlementMontnId(settlementMonth.getId());

                reportDTO.setSettlementPrice(settlementMonth.getSettlementPrice());
                // 월별 정산내역에서 settlementDate를 yyyy-MM-dd에서 yyyy-MM 형식으로 바꾸기 위한 과정 시작
                LocalDate localDate = settlementMonth.getSettlementDate();
                DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM");
                String formattDate = localDate.format(dateTimeFormatter);
                // 월별 정산내역에서 settlementDate를 yyyy-MM-dd에서 yyyy-MM 형식으로 바꾸기 위한 과정 종료
                reportDTO.setSettlementDate(formattDate);
                reportDTO.setStoreId(settlementMonth.getStore().getId());
                reportDTO.setStoreName(settlementMonth.getStore().getName());
                reportDTO.setCreatedDate(settlementMonth.getCreatedDate());
                reportDTOs.add(reportDTO);
            }
            return reportDTOs;
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }
    // 특정 기간 월별 내역 조회
    @GetMapping("/settlementMonth-variable-range-yyyy-MM")
    public List<SettlementMonthReportDTO> settlementMonthByVariableRangeyyyyMM(@RequestParam("StartDate")String StartDate, @RequestParam("EndDate")String EndDate) throws DateTimeParseException {
        try {
            List<SettlementMonth> settlementMonths = settlementMonthService.selectByStoreIdAndDayBetween(1L, StartDate,EndDate);
            List<SettlementMonthReportDTO> reportDTOs = new ArrayList<>();

            for(SettlementMonth settlementMonth: settlementMonths) {
                SettlementMonthReportDTO reportDTO = new SettlementMonthReportDTO();
                reportDTO.setSettlementMontnId(settlementMonth.getId());
                reportDTO.setSettlementPrice(settlementMonth.getSettlementPrice());
                LocalDate localDate = settlementMonth.getSettlementDate();
                DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM");
                String formattedDate = localDate.format(dateTimeFormatter);
                reportDTO.setSettlementDate(formattedDate);
                reportDTO.setStoreId(settlementMonth.getStore().getId());
                reportDTO.setStoreName(settlementMonth.getStore().getName());
                reportDTO.setCreatedDate(settlementMonth.getCreatedDate());
                reportDTOs.add(reportDTO);
            }
            return reportDTOs;
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    // 정산내역의 일별 정산의 상세 주문내역 조회
    // 예시 : 2023-01-01의 정산 내역의 상세 주문내역 조회시 2023-01-01T00:00:00 ~ 2023-01-01T23:59:59의 주문내역 조회

    // 정산내역의 월별 정산의 상세 주문내역 조회
    @PostMapping("/settlement-month/details")
    public ResponseEntity getDetailSettlementMonth(@RequestBody SettlementMonthDetailRequestDTO requestDTO){
        Long store_id = requestDTO.getStore_id();
        LocalDate date = LocalDate.parse(requestDTO.getDate());
        List<Order> orderList =
    }

}