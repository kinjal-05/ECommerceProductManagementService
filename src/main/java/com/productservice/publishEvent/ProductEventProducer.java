package com.productservice.publishEvent;

import com.productservice.commondtos.ProductEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProductEventProducer {

	private final StreamBridge streamBridge;

	public void sendProductEvent(ProductEvent event) {

		streamBridge.send("product-out-0", event);

	}

}