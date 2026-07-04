package com.madhurgram.productservice.customer.dto;

import lombok.*;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerDTO {
    private Long id;
    private String phoneNumber;
    private String fullName;
    private String email;
    private List<AddressDTO> addresses;
}