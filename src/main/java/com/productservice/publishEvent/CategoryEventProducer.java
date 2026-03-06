package com.productservice.publishEvent;
import com.productservice.commondtos.CategoryEvent;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Component;
@Component
public class CategoryEventProducer {
	private final StreamBridge streamBridge;

	public CategoryEventProducer(StreamBridge streamBridge) {
		this.streamBridge = streamBridge;
	}

	public void sendCategoryEvent(CategoryEvent event) {
		streamBridge.send("category-out-0", event);

	}
}
