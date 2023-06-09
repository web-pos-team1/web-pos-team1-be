package com.ssg.webpos.repository.cart;

import com.ssg.webpos.config.jwt.JwtUtil;
import com.ssg.webpos.domain.PosStoreCompositeId;
import com.ssg.webpos.dto.cartDto.CartAddDTO;
import com.ssg.webpos.dto.cartDto.CartAddRequestDTO;
import com.ssg.webpos.dto.coupon.CouponAddRequestDTO;
import com.ssg.webpos.dto.point.PointDTO;
import com.ssg.webpos.repository.CouponRepository;
import com.ssg.webpos.repository.UserRepository;
import com.ssg.webpos.repository.product.ProductRepository;
import com.ssg.webpos.service.CouponService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public class CartRedisImplRepository implements CartRedisRepository {
  @Autowired
  UserRepository userRepository;

  @Autowired
  CouponService couponService;
  @Autowired
  CouponRepository couponRepository;
  private RedisTemplate<String, Map<String, List<Object>>> redisTemplate;


  private HashOperations hashOperations;

  public CartRedisImplRepository(RedisTemplate<String,Map<String, List<Object>>> redisTemplate) {
    this.redisTemplate = redisTemplate;
    this.hashOperations = redisTemplate.opsForHash();
  }

  @Override
  public void saveCart(CartAddRequestDTO cartAddRequestDTO) {
    String posId = String.valueOf(cartAddRequestDTO.getPosId());
    String storeId = String.valueOf(cartAddRequestDTO.getStoreId());
    String compositeId = storeId + "-" + posId;

    Map<String, List<Object>> posData = (Map<String, List<Object>>) hashOperations.get("CART", compositeId);
    if (posData == null) {
      posData = new HashMap<>();
    }

    List<Object> cartList = new ArrayList<>();

    for (CartAddDTO cartAddDTO : cartAddRequestDTO.getCartItemList()) {
      Map<String, Object> cartItem = new HashMap<>();
      cartItem.put("productId", cartAddDTO.getProductId());
      cartItem.put("cartQty", cartAddDTO.getCartQty());
      cartList.add(cartItem);
    }

    int totalPrice = cartAddRequestDTO.getTotalPrice();
    posData.put("totalPrice", Collections.singletonList(totalPrice));
    int totalOriginPrice = cartAddRequestDTO.getTotalOriginPrice();
    String orderName = cartAddRequestDTO.getOrderName();
    System.out.println("saveCart/orderName = " + orderName);
    posData.put("orderName", Collections.singletonList(orderName));
    posData.put("totalOriginPrice", Collections.singletonList(totalOriginPrice));
    posData.put("cartList", cartList);
    hashOperations.put("CART", compositeId, posData);
  }



  @Override
  public void savePoint(PointDTO pointDTO) {
    String posId = String.valueOf(pointDTO.getPosId());
    String storeId = String.valueOf(pointDTO.getStoreId());
    String pointMethod = pointDTO.getPointMethod();
    String compositeId = storeId + "-" + posId;

    Map<String, List<Object>> posData = (Map<String, List<Object>>) hashOperations.get("CART", compositeId);
    if (posData == null) {
      posData = new HashMap<>();
      hashOperations.put("CART", compositeId, posData);
    }

    posData.put("pointMethod", Collections.singletonList(pointMethod));

    List<Object> phoneNumbers = new ArrayList<>();
    phoneNumbers.add(pointDTO.getPhoneNumber());
    posData.put("phoneNumber", phoneNumbers);

    String phoneNumber = pointDTO.getPhoneNumber();
    Long userId = userRepository.findByPhoneNumber(phoneNumber).get().getId();
    posData.put("userId", Collections.singletonList(userId));

    hashOperations.put("CART", compositeId, posData);
  }



  @Override
  public void saveCoupon(CouponAddRequestDTO couponAddRequestDTO) {
    String storeId = String.valueOf(couponAddRequestDTO.getStoreId());
    String posId = String.valueOf(couponAddRequestDTO.getPosId());
    String compositeId = storeId + "-" + posId;
    String serialNumber = couponAddRequestDTO.getSerialNumber();
    String validationMessage = couponService.validateCoupon(serialNumber);
    boolean couponValid = validationMessage.equals("유효한 쿠폰입니다.");

    Map<String, List<Object>> posData = (Map<String, List<Object>>) hashOperations.get("CART", compositeId);
    if (posData == null) {
      posData = new HashMap<>();
      hashOperations.put("CART", compositeId, posData);
    }
    posData.put("useCoupon", Collections.singletonList(couponValid));
    if (couponValid) {
      Long couponId = couponRepository.findBySerialNumber(serialNumber).get().getId();
      int deductedPrice = couponRepository.findBySerialNumber(serialNumber).get().getDeductedPrice();
      String name = couponRepository.findBySerialNumber(serialNumber).get().getName();

      posData.put("couponId", Collections.singletonList(couponId));
      posData.put("deducatedPrice", Collections.singletonList(deductedPrice));
      posData.put("couponName", Collections.singletonList(name));
    }


    hashOperations.put("CART", compositeId, posData);
  }


  @Override
  public Map<String, Map<String, List<Object>>> findAll() throws Exception {
    Map<String, Map<String, List<Object>>> result = new HashMap<>();
    Map<String, Map<String, List<Object>>> posData = hashOperations.entries("CART");
    for (Map.Entry<String, Map<String, List<Object>>> entry : posData.entrySet()) {
      result.put(entry.getKey(), entry.getValue());
    }
    return result;
  }

  @Override
  public Map<String, List<Object>>  findById(String id) {
    return (Map<String, List<Object>>) hashOperations.get("CART", id);
  }

  @Override
  public String findPhoneNumber(String compositeId) {
    Map<String, List<Object>> posData = (Map<String, List<Object>>) hashOperations.get("CART", compositeId);
    if (posData != null) {
      List<Object> phoneNumberList = posData.get("phoneNumber");
      if (phoneNumberList != null && !phoneNumberList.isEmpty()) {
        return (String) phoneNumberList.get(0);
      }
    }
    return null;
  }

  @Override
  public List<Map<String, Object>> findCartItems(String compositeId) {
    Map<String, List<Object>> posData = (Map<String, List<Object>>) hashOperations.get("CART", compositeId);
    if (posData != null) {
      List<Object> cartList = posData.get("cartList");
      List<Map<String, Object>> cartItemList = new ArrayList<>();

      if (cartList != null && !cartList.isEmpty()) {
        for (Object obj : cartList) {
          Map<String, Object> cartItem = (Map<String, Object>) obj;
          cartItemList.add(cartItem);
        }
      }

      return cartItemList;
    }

    return null;
  }


    public Long findUserId(String compositeId) {
    Map<String, List<Object>> posData = (Map<String, List<Object>>) hashOperations.get("CART", compositeId);
    if (posData != null) {
      List<Object> userIdList = posData.get("userId");
      if (userIdList != null && !userIdList.isEmpty()) {
        return (Long) userIdList.get(0);
      }
    }
    return null;
  }
  @Override
  public Integer findTotalPrice(String compositeId) {
    Map<String, List<Object>> posData = (Map<String, List<Object>>) hashOperations.get("CART", compositeId);
    if (posData != null) {
      List<Object> totalPriceList = posData.get("totalPrice");
      if (totalPriceList != null && !totalPriceList.isEmpty()) {
        return (Integer) totalPriceList.get(0);
      }
    }
    return null;
  }

  @Override
  public Integer findTotalOriginPrice(String compositeId) {
    Map<String, List<Object>> posData = (Map<String, List<Object>>) hashOperations.get("CART", compositeId);
    if (posData != null) {
      List<Object> totalOriginPriceList = posData.get("totalOriginPrice");
      if (totalOriginPriceList != null && !totalOriginPriceList.isEmpty()) {
        return (Integer) totalOriginPriceList.get(0);
      }
    }
    return null;
  }
  @Override
  public String findOrderName(String compositeId) {
    Map<String, List<Object>> posData = (Map<String, List<Object>>) hashOperations.get("CART", compositeId);
    if (posData != null) {
      List<Object> orderNameList = posData.get("orderName");
      if (orderNameList != null && !orderNameList.isEmpty()) {
        return (String) orderNameList.get(0);
      }
    }
    return null;
  }

  @Override
  public Integer findDeductedPrice(String compositeId) {
    Map<String, List<Object>> posData = (Map<String, List<Object>>) hashOperations.get("CART", compositeId);
    if (posData != null) {
      List<Object> couponIdList = posData.get("deducatedPrice");
      if (couponIdList != null && !couponIdList.isEmpty()) {
        return (Integer) couponIdList.get(0);
      }
    }
    return null;
  }

  @Override
  public Long findCouponId(String compositeId) {
    Map<String, List<Object>> posData = (Map<String, List<Object>>) hashOperations.get("CART", compositeId);
    if (posData != null) {
      List<Object> couponIdList = posData.get("couponId");
      if (couponIdList != null && !couponIdList.isEmpty()) {
        return (Long) couponIdList.get(0);
      }
    }
    return null;
  }

  @Override
  public void updatePoint(PointDTO pointDTO) {
    String compositeId = pointDTO.getPosId() + "-" + pointDTO.getStoreId();

    hashOperations.delete("CART", compositeId);
    savePoint(pointDTO);
  }

  @Override
  public void delete(String id) {
    hashOperations.delete("CART", id);
  }
  @Override
  public void deleteAll() {
    redisTemplate.delete("CART");
  }

  public void saveToken(String refreshToken, PosStoreCompositeId compositeId) {
    Long id = JwtUtil.getId(refreshToken);
    String compositeIdStr = compositeId.getStore_id() + "-" + compositeId.getPos_id();
    List idData = new ArrayList();
    idData.add(id);
    Map<String, List<Object>> tokenData = new HashMap<>();
    tokenData.put(refreshToken, idData);
    hashOperations.put("CART", compositeIdStr, tokenData);
  }
  public void deleteToken(String refreshToken) {
    // CART -> compositId -> ${refresh} 를 key로 가진 것만 삭제하는 로직
  }
}
