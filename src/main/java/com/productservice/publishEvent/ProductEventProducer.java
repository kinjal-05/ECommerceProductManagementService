package com.productservice.publishEvent;
import com.productservice.commondtos.ProductEvent;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Component;
@Component
public class ProductEventProducer {

	private final StreamBridge streamBridge;

	public ProductEventProducer(StreamBridge streamBridge) {
		this.streamBridge = streamBridge;
	}

	public void sendProductEvent(ProductEvent event) {

		streamBridge.send("product-out-0", event);

	}

}