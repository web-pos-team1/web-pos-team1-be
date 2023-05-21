package com.ssg.webpos.service;

import com.ssg.webpos.repository.cart.CartRedisImplRepository;
import com.ssg.webpos.repository.cart.CartRepository;
import com.ssg.webpos.repository.order.OrderRepository;
import com.ssg.webpos.repository.pos.PosRepository;
import com.ssg.webpos.repository.product.ProductRepository;
import com.ssg.webpos.repository.store.StoreRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@Transactional
class CartRedisServiceTest {

  @Autowired
  OrderRepository orderRepository;
  @Autowired
  ProductRepository productRepository;
  @Autowired
  CartRepository cartRepository;

  @Autowired
  StoreRepository storeRepository;
  @Autowired
  PosRepository posRepository;

  @Autowired
  CartRedisService cartRedisService;

  @Autowired
  CartRedisImplRepository cartRedisRepository;


  @Test
  void addOrderTest() throws Exception {

  }
}
