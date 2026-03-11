package com.productservice.publishEvent;

import com.productservice.commondtos.CategoryEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CategoryEventProducer {

	private final StreamBridge streamBridge;

	public void sendCategoryEvent(CategoryEvent event) {
		streamBridge.send("category-out-0", event);

	}
}
