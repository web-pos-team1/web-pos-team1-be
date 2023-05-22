package com.ssg.webpos.dto.settlement;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Getter
@Setter
public class RequestSettlementMonthByStoreIdDTO {
    private String year;
    private Long storeId;
}
