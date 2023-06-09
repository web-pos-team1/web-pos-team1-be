package com.ssg.webpos.service.hqController.csv;

import com.ssg.webpos.dto.hqSale.HqSaleOrderDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
@Service
@RequiredArgsConstructor
public class CsvService {
    public void exportToCsv(List<HqSaleOrderDTO> orderDTOList, String fileName) {
        // 파일 저장 경로 설정
        String filePath = "C:/Users/교육생56/Desktop/webpos/" + fileName; // 사용자가 파일 저장 장소 선택할 수 있게 코드 구현
        try (BufferedWriter writer = new BufferedWriter( new OutputStreamWriter( new FileOutputStream(filePath), StandardCharsets.UTF_8))) {
            writer.write('\ufeff');
            // CSV 헤더 작성
            writer.append("Serial Number,Store Name,Order Date,Order Status,Pay Method,Total Price,Point Use Price,Coupon Use Price,Final Total Price,Charge,Total Origin Price,Profit");
            writer.append("\n");

            // CSV 데이터 작성
            for (HqSaleOrderDTO orderDTO : orderDTOList) {
                String orderDate = String.valueOf(orderDTO.getOrderDate());
                String orderStatus = String.valueOf(orderDTO.getOrderStatus());
                String payMethod = String.valueOf(orderDTO.getPayMethod());
                writer.append(orderDTO.getSerialNumber());
                writer.append(",");
                writer.append(orderDTO.getStoreName());
                writer.append(",");
                writer.append(orderDate);
                writer.append(",");
                writer.append(orderStatus);
                writer.append(",");
                writer.append(payMethod);
                writer.append(",");
                writer.append(String.valueOf(orderDTO.getTotalPrice()));
                writer.append(",");
                writer.append(String.valueOf(orderDTO.getPointUsePrice()));
                writer.append(",");
                writer.append(String.valueOf(orderDTO.getCouponUsePrice()));
                writer.append(",");
                writer.append(String.valueOf(orderDTO.getFinalTotalPrice()));
                writer.append(",");
                writer.append(String.valueOf(orderDTO.getCharge()));
                writer.append(",");
                writer.append(String.valueOf(orderDTO.getTotalOriginPrice()));
                writer.append(",");
                writer.append(String.valueOf(orderDTO.getProfit()));
                writer.append("\n");

                System.out.println("Serial Number: " + orderDTO.getSerialNumber());
                System.out.println("Store Name: " + orderDTO.getStoreName());
                System.out.println("Order Date: " + orderDate);
            }



            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}