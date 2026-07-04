package com.madhurgram.productservice.customer.service;

import com.madhurgram.productservice.customer.dto.AddressDTO;
import com.madhurgram.productservice.customer.dto.CustomerDTO;

public interface CustomerService {
    // फोन नंबर डालते ही प्रोफाइल मिलेगी, अगर पहली बार आया है तो ऑटो-रजिस्टर (On-the-fly) होगा
    CustomerDTO getCustomerProfile(String phoneNumber);
    
    // कस्टमर की प्रोफाइल में नया एड्रेस जोड़ने का मेथड
    CustomerDTO addAddressToProfile(String phoneNumber, AddressDTO addressDTO);

}