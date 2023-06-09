package com.ssg.webpos.service;

import com.ssg.webpos.domain.*;
import com.ssg.webpos.domain.enums.CouponStatus;
import com.ssg.webpos.domain.enums.OrderStatus;
import com.ssg.webpos.domain.enums.PayMethod;
import com.ssg.webpos.domain.enums.RoleUser;
import com.ssg.webpos.dto.*;
import com.ssg.webpos.dto.cartDto.CartAddDTO;
import com.ssg.webpos.dto.cartDto.CartAddRequestDTO;
import com.ssg.webpos.dto.coupon.CouponAddRequestDTO;
import com.ssg.webpos.dto.delivery.DeliveryRedisAddRequestDTO;
import com.ssg.webpos.dto.point.PointDTO;
import com.ssg.webpos.dto.point.PointUseRequestDTO;
import com.ssg.webpos.repository.*;
import com.ssg.webpos.repository.cart.CartRedisRepository;
import com.ssg.webpos.repository.cart.CartRepository;
import com.ssg.webpos.repository.delivery.DeliveryRedisImplRepository;
import com.ssg.webpos.repository.order.OrderRepository;
import com.ssg.webpos.repository.product.ProductRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.transaction.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

@SpringBootTest
@Transactional
public class PaymentsServiceTest {
  @Autowired
  PaymentsService paymentsService;
  @Autowired
  PointUseHistoryRepository pointUseHistoryRepository;
  @Autowired
  CartRedisRepository cartRedisRepository;
  @Autowired
  CouponRepository couponRepository;
  @Autowired
  CouponService couponService;
  @Autowired
  OrderRepository orderRepository;
  @Autowired
  ProductRepository productRepository;
  @Autowired
  UserRepository userRepository;
  @Autowired
  PointService pointService;
  @Autowired
  PointRepository pointRepository;
  @Autowired
  CartRepository cartRepository;
  @Autowired
  PointSaveHistoryRepository pointSaveHistoryRepository;
  @Autowired
  DeliveryRedisImplRepository deliveryRedisImplRepository;

  @BeforeEach
  void setup() {


  }

  @Test
  @DisplayName("결제 완료 시 재고 업데이트")
  void updateStockIfPaymentSuccess() throws Exception {
    Long productId1 = 11L;
    Long productId2 = 12L;
    int cartQty1 = 3;
    int cartQty2 = 5;
    saveRedisCart(productId1, productId2, cartQty1, cartQty2);
    Product product1 = productRepository.findById(productId1).get();
    Product product2 = productRepository.findById(productId2).get();
    int beforeStock1 = product1.getStock();
    int beforeStock2 = product2.getStock();

    System.out.println("beforeStock1 = " + beforeStock1);
    System.out.println("beforeStock2 = " + beforeStock2);

    PaymentsDTO paymentsDTO = new PaymentsDTO();
    paymentsDTO.setPosId(2L);
    paymentsDTO.setStoreId(2L);
    paymentsDTO.setSuccess(true);
    paymentsDTO.setName("사과");
    paymentsDTO.setPaidAmount(BigDecimal.valueOf(10000));
    paymentsDTO.setPg("kakaopay");

    paymentsService.processPaymentCallback(paymentsDTO);

    Product afterProduct1 = productRepository.findById(productId1).get();
    Product afterProduct2 = productRepository.findById(productId2).get();

    int afterStock1 = afterProduct1.getStock();
    int afterStock2 = afterProduct2.getStock();

    System.out.println("afterStock1 = " + afterStock1);
    System.out.println("afterStock2 = " + afterStock2);

    assertEquals(beforeStock1 - cartQty1, afterStock1);
    assertEquals(beforeStock2 - cartQty2, afterStock2);
  }
  @Test
  @DisplayName("장바구니 추가 후 쿠폰 적용: 비회원일 경우 쿠폰 상태 NOT_USED -> USED")
  void addToCartWithCouponIfPaymentSuccess() throws Exception {
    Long productId1 = 50L;
    Long productId2 = 51L;
    int cartQty1 = 3;
    int cartQty2 = 5;
    saveRedisCart(productId1, productId2, cartQty1, cartQty2);
    Order order = new Order();
    order.setOrderStatus(OrderStatus.SUCCESS);
    order.setPayMethod(PayMethod.CREDIT_CARD);
    Order saveOrder = orderRepository.save(order);
    saveRedisPoint();
    Coupon createCoupon = createCoupon();
    System.out.println("beforeCouponStatus" + createCoupon.getCouponStatus());
    saveRedisCoupon(createCoupon);
    Coupon coupon = couponService.updateCouponStatusToUsed(createCoupon.getId());
    coupon.setOrder(order);
    order.getCouponList().add(coupon);

    processPayment();

    CouponStatus afterCouponStatus = createCoupon.getCouponStatus();
    System.out.println("afterCouponStatus = " + afterCouponStatus);

    assertEquals(CouponStatus.USED, afterCouponStatus);
  }

  @Test
  void updateStockAndAddToCart() {
    Long productId1 = 51L;
    Long productId2 = 52L;
    int cartQty1 = 3;
    int cartQty2 = 5;
    saveRedisCart(productId1, productId2, cartQty1, cartQty2);
    Order order = new Order();
    order.setOrderStatus(OrderStatus.SUCCESS);
    order.setPayMethod(PayMethod.CREDIT_CARD);
    Order saveOrder = orderRepository.save(order);

    List<Map<String, Object>> cartItemList = cartRedisRepository.findCartItems("2-2");
    for (Map<String, Object> cartItem : cartItemList) {
      CartAddDTO cartAddDTO = new CartAddDTO();
      cartAddDTO.setProductId((Long) cartItem.get("productId"));
      cartAddDTO.setCartQty((int) cartItem.get("cartQty"));
      Product product = paymentsService.updateStockAndAddToCart(cartAddDTO);
      List<Cart> cartList = order.getCartList();
      Cart cart = new Cart(product, saveOrder);
      cart.setQty(cartAddDTO.getCartQty());
      cartList.add(cart);
      cartRepository.saveAll(cartList);
    }
  }
  @Test
  @DisplayName("장바구니 추가 후 쿠폰 적용: 회원일 경우 쿠폰 상태 NOT_USED -> USED")
  void addToCartWithCouponIfPaymentSuccessAndUser() throws Exception {

    Long productId1 = 50L;
    Long productId2 = 51L;
    int cartQty1 = 3;
    int cartQty2 = 5;
    saveRedisCart(productId1, productId2, cartQty1, cartQty2);
    Long userId = saveRedisPoint();
    Order order = new Order();
    order.setOrderStatus(OrderStatus.SUCCESS);
    order.setPayMethod(PayMethod.CREDIT_CARD);
    Order saveOrder = orderRepository.save(order);
    saveRedisPoint();
    Coupon createCoupon = createCoupon();
    System.out.println("beforeCouponStatus" + createCoupon.getCouponStatus());
    saveRedisCoupon(createCoupon);
    Coupon coupon = couponService.updateCouponStatusToUsed(createCoupon.getId());
    coupon.setOrder(order);
    order.getCouponList().add(coupon);
    User user = userRepository.findById(userId).get();
    coupon.setUser(user);

    processPayment();

    CouponStatus afterCouponStatus = createCoupon.getCouponStatus();
    System.out.println("afterCouponStatus = " + afterCouponStatus);

    assertEquals(CouponStatus.USED, afterCouponStatus);
  }

  
  @Test
  @DisplayName("point 테이블 point_amount 업데이트")
  void SavePointUseHistoryAndPointSaveHistory() {
    Long userId = saveRedisPoint();
    System.out.println("userId = " + userId);
    cartRedisRepository.findUserId("2-2");
    Long productId1 = 50L;
    Long productId2 = 51L;
    int cartQty1 = 3;
    int cartQty2 = 5;
    saveRedisCart(productId1, productId2, cartQty1, cartQty2);
    Order order = new Order();
    order.setOrderStatus(OrderStatus.SUCCESS);
    order.setPayMethod(PayMethod.CREDIT_CARD);
    Order saveOrder = orderRepository.save(order);
    PaymentsDTO paymentsDTO = new PaymentsDTO();
    paymentsDTO.setPosId(2L);
    paymentsDTO.setStoreId(2L);
    paymentsDTO.setSuccess(true);
    paymentsDTO.setName("사과");
    paymentsDTO.setPointAmount(50);
    paymentsDTO.setPg("kcp");
    int finalTotalPrice = 100000;
    paymentsDTO.setPaidAmount(BigDecimal.valueOf(finalTotalPrice));
    int pointUseAmount = paymentsDTO.getPointAmount();
    Point point = userRepository.findById(userId).get().getPoint();
    PointUseHistory pointUseHistory = new PointUseHistory(pointUseAmount, saveOrder, point);
    PointUseHistory savePointUse = pointUseHistoryRepository.save(pointUseHistory);
    order.setPointUsePrice(pointUseAmount);
    int pointSaveAmount = pointService.updatePoint(finalTotalPrice);
    PointSaveHistory pointSaveHistory = new PointSaveHistory(pointSaveAmount, order, point);
    PointSaveHistory savePointSave = pointSaveHistoryRepository.save(pointSaveHistory);
    System.out.println("pointSaveHistory = " + pointSaveHistory);
    paymentsService.processPaymentCallback(paymentsDTO);
    int pointSaveAmount1 = savePointSave.getPointSaveAmount();
    System.out.println("pointSaveAmount1 = " + pointSaveAmount1);
    System.out.println("savePointUse = " + savePointUse);

  }




//  @Test
//  void test_processCartItems_Test() {
//    Long productId1 = 11L;
//    Long productId2 = 12L;
//    int cartQty1 = 3;
//    int cartQty2 = 5;
//    String compositeId = "2-2";
//    Order order = Order.builder()
//        .orderStatus(OrderStatus.SUCCESS)
//        .payMethod(PayMethod.CREDIT_CARD)
//        .orderName("키위 외 2건")
//        .build();
//    orderRepository.save(order);
//    saveRedisCart(productId1, productId2, cartQty1, cartQty2);
//    List<Map<String, Object>> cartItemList = cartRedisRepository.findCartItems(compositeId);
//    for (Map<String, Object> cartItem : cartItemList) {
//      CartAddDTO cartAddDTO = new CartAddDTO();
//      cartAddDTO.setProductId((Long) cartItem.get("productId"));
//      cartAddDTO.setCartQty((int) cartItem.get("cartQty"));
//      Product product = paymentsService.updateStockAndAddToCart(cartAddDTO);
//      List<Cart> cartList = order.getCartList();
//      Cart cart = new Cart(product, order);
//      cart.setQty(cartAddDTO.getCartQty());
//      System.out.println("cart = " + cart);
//      cartList.add(cart);
//      cartRepository.saveAll(cartList);
//    }
//  }
  @Test
  @DisplayName("결제 수단 설정 테스트: creditCard")
  void SaveOrderPayMethodCreditCard() {
    // Given
    Long productId1 = 11L;
    Long productId2 = 12L;
    int cartQty1 = 3;
    int cartQty2 = 5;
    saveRedisCart(productId1, productId2, cartQty1, cartQty2);
    PaymentsDTO paymentsDTO = new PaymentsDTO();
    paymentsDTO.setPosId(2L);
    paymentsDTO.setStoreId(2L);
    paymentsDTO.setSuccess(true);
    paymentsDTO.setName("사과");
    paymentsDTO.setPointAmount(50);
    paymentsDTO.setPg("nice");
    int finalTotalPrice = 100000;
    paymentsDTO.setPaidAmount(BigDecimal.valueOf(finalTotalPrice));
    Order saveOrder= paymentsService.processPaymentCallback(paymentsDTO);
    String pgProvider = paymentsDTO.getPg();
    System.out.println("saveOrder = " + saveOrder);
    if (pgProvider.equals("kakaopay")) {
      assertEquals(PayMethod.KAKAO_PAY, saveOrder.getPayMethod());
    } else if (pgProvider.equals("nice")) {
      assertEquals(PayMethod.CREDIT_CARD, saveOrder.getPayMethod());
    } else if (pgProvider.equals("kcp")) {
      assertEquals(PayMethod.SAMSUNG_PAY, saveOrder.getPayMethod());
    }

  }
  @Test
  @DisplayName("결제 수단 설정 테스트: kakaoPay")
  void SaveOrderPayMethodKakaoPay() {
    // Given
    Long productId1 = 11L;
    Long productId2 = 12L;
    int cartQty1 = 3;
    int cartQty2 = 5;
    saveRedisCart(productId1, productId2, cartQty1, cartQty2);
    PaymentsDTO paymentsDTO = new PaymentsDTO();
    paymentsDTO.setPosId(2L);
    paymentsDTO.setStoreId(2L);
    paymentsDTO.setSuccess(true);
    paymentsDTO.setName("사과");
    paymentsDTO.setPointAmount(50);
    paymentsDTO.setPg("kakaopay");
    int finalTotalPrice = 100000;
    paymentsDTO.setPaidAmount(BigDecimal.valueOf(finalTotalPrice));
    Order saveOrder= paymentsService.processPaymentCallback(paymentsDTO);
    String pgProvider = paymentsDTO.getPg();

    System.out.println("saveOrder = " + saveOrder);
    System.out.println("saveOrder = " + saveOrder);
    if (pgProvider.equals("kakaopay")) {
      assertEquals(PayMethod.KAKAO_PAY, saveOrder.getPayMethod());
    } else if (pgProvider.equals("nice")) {
      assertEquals(PayMethod.CREDIT_CARD, saveOrder.getPayMethod());
    } else if (pgProvider.equals("kcp")) {
      assertEquals(PayMethod.SAMSUNG_PAY, saveOrder.getPayMethod());
    }

  }
  @Test
  @DisplayName("결제 수단 설정 테스트: samsungPay")
  void SaveOrderPayMethodSamsungPay() {
    // Given
    Long productId1 = 11L;
    Long productId2 = 12L;
    int cartQty1 = 3;
    int cartQty2 = 5;
    saveRedisCart(productId1, productId2, cartQty1, cartQty2);
    PaymentsDTO paymentsDTO = new PaymentsDTO();
    paymentsDTO.setPosId(2L);
    paymentsDTO.setStoreId(2L);
    paymentsDTO.setSuccess(true);
    paymentsDTO.setName("사과");
    paymentsDTO.setPointAmount(50);
    paymentsDTO.setPg("kcp");
    int finalTotalPrice = 100000;
    paymentsDTO.setPaidAmount(BigDecimal.valueOf(finalTotalPrice));
    Order saveOrder= paymentsService.processPaymentCallback(paymentsDTO);
    String pgProvider = paymentsDTO.getPg();

    System.out.println("saveOrder = " + saveOrder);
    if (pgProvider.equals("kakaopay")) {
      assertEquals(PayMethod.KAKAO_PAY, saveOrder.getPayMethod());
    } else if (pgProvider.equals("nice")) {
      assertEquals(PayMethod.CREDIT_CARD, saveOrder.getPayMethod());
    } else if (pgProvider.equals("kcp")) {
      assertEquals(PayMethod.SAMSUNG_PAY, saveOrder.getPayMethod());
    }

  }




  @Test
  @DisplayName("serialNumber")
  void generateSerialNumber() {
    List<Order> orderList = new ArrayList<>();
    Long storeId = 2L;
    Long posId = 2L;
    String orderDateStr = LocalDateTime.now().format(DateTimeFormatter.BASIC_ISO_DATE);
    String generatedSerialNumber1 = paymentsService.generateSerialNumber(orderList, storeId, posId);
    Assertions.assertEquals(orderDateStr + "02020001", generatedSerialNumber1);
    Order newOrder = new Order();
    orderList.add(newOrder);
    String generatedSerialNumber2 = paymentsService.generateSerialNumber(orderList, storeId, posId);
    Assertions.assertEquals(orderDateStr + "02020002", generatedSerialNumber2);
  }

  private void saveRedisCart(Long productId1, Long productId2, int cartQty1, int cartQty2) {
  PosStoreCompositeId posStoreCompositeId = new PosStoreCompositeId();
  posStoreCompositeId.setPos_id(2L);
  posStoreCompositeId.setStore_id(2L);
  CartAddRequestDTO requestDTO = new CartAddRequestDTO();
  requestDTO.setPosId(posStoreCompositeId.getPos_id());
  requestDTO.setStoreId(posStoreCompositeId.getStore_id());
  requestDTO.setTotalPrice(10000);
  String compositeId = String.valueOf(posStoreCompositeId.getStore_id()) + "-" + String.valueOf(posStoreCompositeId.getPos_id());

  List<CartAddDTO> cartItemList = new ArrayList<>();
  CartAddDTO cartAddDTO1 = new CartAddDTO();
  cartAddDTO1.setProductId(productId1);
  cartAddDTO1.setCartQty(cartQty1);
  cartItemList.add(cartAddDTO1);

  CartAddDTO cartAddDTO2 = new CartAddDTO();
  cartAddDTO2.setProductId(productId2);
  cartAddDTO2.setCartQty(cartQty2);
  cartItemList.add(cartAddDTO2);

  requestDTO.setCartItemList(cartItemList);
  cartRedisRepository.delete(compositeId);
  cartRedisRepository.saveCart(requestDTO);
}


  private void saveRedisCoupon(Coupon coupon) throws Exception {
    CouponAddRequestDTO couponAddRequestDTO = new CouponAddRequestDTO();
    couponAddRequestDTO.setPosId(2L);
    couponAddRequestDTO.setStoreId(2L);
    couponAddRequestDTO.setSerialNumber(coupon.getSerialNumber());
    cartRedisRepository.saveCoupon(couponAddRequestDTO);

    Map<String, Map<String, List<Object>>> cartall = cartRedisRepository.findAll();
    System.out.println("cartall = " + cartall);
  }
  private Coupon createCoupon() {
    Coupon coupon = new Coupon();
    coupon.setCouponStatus(CouponStatus.NOT_USED);
    coupon.setName("500원");
    coupon.setSerialNumber("666666666");
    coupon.setDeductedPrice(500);
    coupon.setExpiredDate(LocalDate.now().plusDays(7));
    Coupon saveCoupon = couponRepository.save(coupon);
    return saveCoupon;
  }
  private PaymentsDTO processPayment() {
    PaymentsDTO paymentsDTO = new PaymentsDTO();
    paymentsDTO.setPosId(2L);
    paymentsDTO.setStoreId(2L);
    paymentsDTO.setSuccess(true);
    paymentsDTO.setName("사과");
    paymentsDTO.setPointAmount(50);
    int finalTotalPrice = 100000;
    paymentsDTO.setPaidAmount(BigDecimal.valueOf(finalTotalPrice));
    paymentsDTO.setPg("kakaopay");
    paymentsDTO.setCouponUsePrice(500);
    Order order = paymentsService.processPaymentCallback(paymentsDTO);
    System.out.println("processPayment order = " + order);
    return paymentsDTO;
  }

  private Long saveRedisPoint() {
    cartRedisRepository.delete("2-2");
    User user = new User();
    user.setName("고경환12");
    user.setEmail("5656@naver.com");
    user.setPassword("1234");
    user.setPhoneNumber("01032244099");
    user.setRole(RoleUser.NORMAL);
    User saveUser = userRepository.save(user);
    PointDTO pointDTO = new PointDTO();
    pointDTO.setPhoneNumber(saveUser.getPhoneNumber());
    pointDTO.setPointMethod("phoneNumber");
    pointDTO.setStoreId(2L);
    pointDTO.setPosId(2L);
    String compositeId = "2-2";
    cartRedisRepository.savePoint(pointDTO);
    Long userId = cartRedisRepository.findUserId(compositeId);
    return userId;

  }

  @Test
  void saveRedisDelivery() {
    DeliveryRedisAddRequestDTO deliveryRedisAddRequestDTO = DeliveryRedisAddRequestDTO.builder()
        .storeId(2L)
        .posId(2L)
        .deliveryName("우리집")
        .userName("김진아")
        .phoneNumber("01032244099")
        .address("부산광역시 남구")
        .requestDeliveryTime("12:00~15:00")
        .postCode("052508")
        .requestInfo("부재 시, 경비실에 맡겨주세요.")
        .build();
//    deliveryRedisAddRequestDTOList.add(deliveryRedisAddRequestDTO);
    // when
    deliveryRedisImplRepository.saveDelivery(deliveryRedisAddRequestDTO);
  }

}
