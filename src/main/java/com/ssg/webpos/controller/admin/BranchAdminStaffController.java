package com.ssg.webpos.controller.admin;

import com.ssg.webpos.domain.Product;
import com.ssg.webpos.domain.ProductRequest;
import com.ssg.webpos.domain.StockReport;
import com.ssg.webpos.domain.Store;
import com.ssg.webpos.dto.stock.StoreIdStockReportResponseDTO;
import com.ssg.webpos.dto.stock.stockSubmit.*;
import com.ssg.webpos.repository.ProductRequestRepository;
import com.ssg.webpos.repository.StockReportRepository;
import com.ssg.webpos.repository.product.ProductRepository;
import com.ssg.webpos.repository.store.StoreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/staff")
@Slf4j
@RequiredArgsConstructor
public class BranchAdminStaffController {
    private final ProductRequestRepository productRequestRepository;
    private final ProductRepository productRepository;
    private final StoreRepository storeRepository;
    private final StockReportRepository stockReportRepository;

    // store_id별 재고내역 조회
    @GetMapping("/stock-report-view/{storeId}")
    @Transactional
    public ResponseEntity stockReportStoreId(@PathVariable("storeId") String storeId) {
        try {
            // String으로 받아 Long으로 파싱하겠습니다.
            Long Id = Long.parseLong(storeId);
            List<StockReport> stockReports = stockReportRepository.findByStoreId(Id);
            List<StoreIdStockReportResponseDTO> lists = new ArrayList<>();
            // storeId로 받은 여러개의 stockReport
            for (StockReport stockReport : stockReports) {
                StoreIdStockReportResponseDTO DTO = new StoreIdStockReportResponseDTO();
                DTO.setCurrentStock(stockReport.getCurrentStock());
                DTO.setSubmit(stockReport.isSubmit()); // boolean은 get이 아닌 is 그대로 가져간다.
                Product product = stockReport.getProduct();
                DTO.setProductName(product.getName());
                DTO.setProductSalePrice(product.getSalePrice());
                DTO.setCategory(product.getCategory());
                lists.add(DTO);
            }
            return new ResponseEntity<>(lists,HttpStatus.OK);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity(HttpStatus.BAD_REQUEST);
        }
    }
    // store_id별 재고내역 수정( 상품의 재고수량 수정 -> product의 stock 이 변경되어야한다.stock_report의 current_stock이 변경되어야한다.)
    // store_id의 stockReport에서 currentStock을 수정한다.
    // 실제로 실행되는 지는 확인 못함
    @PostMapping("/stock-report-modify/store-id")
    public ResponseEntity stockReportStoreIdModify(@RequestParam Long storeId, @RequestParam int newCurrentStock) {
        try {
            List<StockReport> findStockReports = stockReportRepository.findByStoreId(storeId);
            for (StockReport findStockReport : findStockReports) {
                findStockReport.setCurrentStock(newCurrentStock);
            }
            stockReportRepository.saveAll(findStockReports);
            return new ResponseEntity(HttpStatus.OK);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // 발주 신청해야하는 상품 목록에서 발주 신청한 상품들을 제출
    // 반환하는 것은 없고 입력값을 활용해 ProductRequst 엔티티 생성, 생성된 엔티티를 JpaRepository를 활용해 DB에 저장
    @PostMapping("/stock-report/submit")
    public ResponseEntity stockReportSubmit(@RequestBody SubmitRequestDTOList submitRequestDTOList) {
        try {
            List<SubmitRequestDTO> submitRequestDTOs = submitRequestDTOList.getSubmitRequestDTOList();
            // REST API에서 활용할 DTO 작성 시작
//            submitResponseDTOList.setSubmitResponseDTO(submitResponseDTOs); // 이제 이 DTO를 product_request 에 JpaRepository를 활용해 입력하고 싶다.
            // REST API에서 활용할 DTO 작성 종료
            // 테이블에 입력할 정보 따로, POST REST API에서 활용할 것 따로 작성했습니다.
            // 테이블에는 store_id를 포함한 열이 필요하고 POST REST API에서는 store_id와 다른 칼럼들을 나누어서 사용하기 때문에
            // DTO 를 사용하면 테이블에 넣기 어렵고 엔티티 사용하면 연관관계때문에 Lazy에러 발생할 것 같고..
            List<ProductRequest> productRequests = new ArrayList<>(); // DTO를 엔티티로 변환한 다음 테이블에 입력할 것이다.
            // iter 명령어로 for-each문 생성

            for(SubmitRequestDTO submitRequestDTO : submitRequestDTOs) {
                ProductRequest productRequest = new ProductRequest();
                productRequest.setQty(submitRequestDTO.getQty());
                Optional<Product> findProduct = productRepository.findById(submitRequestDTO.getProductId());
                productRequest.setProduct(findProduct.get()); // 양방향 연관관계 필요없음(product에서 product_request 조회x)
                Optional<Store> store = storeRepository.findById(submitRequestDTOList.getStoreId());
                productRequest.addStoreWithAssociation(store.get());
                productRequests.add(productRequest);
            }
            productRequestRepository.saveAll(productRequests);

            // **StockProduct에서 발주 신청 상품을 가진 열의 isSubmit을 1로 변경한다.**
            // 중첩 반복문을 활용하겠다.
            for (SubmitRequestDTO submitRequestDTO : submitRequestDTOs) {
                // StockReport의 prdocutId
                Optional<StockReport> findStockReport = stockReportRepository.findById(submitRequestDTO.getStockReportId());
                StockReport stockReport = findStockReport.get();
                stockReport.setSubmit(true);
            }
            return new ResponseEntity(HttpStatus.NO_CONTENT);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity(HttpStatus.BAD_REQUEST);
        }
    }

    // 기간별 재고 조회
    @GetMapping("/stock-report-view/{storeId}/{startDate}/{endDate}")
    @Transactional
    public ResponseEntity stockReportAllByCreatedDateBetween(@PathVariable("storeId") String storeId, @PathVariable("startDate") String startDate, @PathVariable("endDate") String endDate) {
        try {
            Long id = Long.parseLong(storeId);
            LocalDateTime end = LocalDateTime.parse(endDate);
            LocalDateTime start = LocalDateTime.parse(startDate);
            List<StockReport> stockReports = stockReportRepository.findByStoreIdAndCreatedDateBetween(id,start,end);
            List<StoreIdStockReportResponseDTO> lists = new ArrayList<>();
            // storeId로 받은 여러개의 stockReport
            for (StockReport stockReport : stockReports) {
                StoreIdStockReportResponseDTO DTO = new StoreIdStockReportResponseDTO();
                DTO.setCurrentStock(stockReport.getCurrentStock());
                DTO.setSubmit(stockReport.isSubmit()); // boolean은 get이 아닌 is 그대로 가져간다.
                Product product = stockReport.getProduct();
                DTO.setProductName(product.getName());
                DTO.setProductSalePrice(product.getSalePrice());
                DTO.setCategory(product.getCategory());
                lists.add(DTO);
            }
            return new ResponseEntity<>(lists,HttpStatus.OK);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity(HttpStatus.BAD_REQUEST);
        }
    }
}