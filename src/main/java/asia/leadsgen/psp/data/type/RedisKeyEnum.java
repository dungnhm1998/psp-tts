package asia.leadsgen.psp.data.type;

public enum RedisKeyEnum {
	
	SHIPPING_OWNER("redis.default.shipping.owner"),
	BASES_WEIGHT("redis.default.base.weight"),
	BASES_COLORS_MAP("redis.map.bases.colors"),
	CATEGORY_NAMES_MAP("redis.map.category.names"),
	BASES_FEED_MAP("redis.map.bases.feed"),
	BASES_MAP("redis.map.dropship.bases"),
	SIZES_AND_AVAILABLE_BASE_MAP("redis.map.sizes.and.available.bases"),
	TASK_PROCESS_FETCH_ORDER_WOO("redis.map.task.fetch.order.woo"),
	TASK_PROCESS_IMPORT_ORDER_CSV("redis.map.task.import.order.csv"),
	TASK_PROCESS_FETCH_ORDER_SHOPIFY("redis.map.task.fetch.order.shopify"),
	TASK_PROCESS_FETCH_PRODUCT_SHOPIFY("redis.map.task.fetch.product.shopify"),
	TASK_PROCESS_ADD_PRODUCT_SHOPIFY("redis.map.task.add.product.shopify"),
	TASK_PROCESS_ADD_VARIANT_SHOPIFY("redis.map.task.add.variant.shopify"),
	TASK_PROCESS_FETCH_ORDER_SHOPIFY_APP("redis.map.task.fetch.shopifyapp.order"),
	BASE_GROUPS_MAP("redis.map.dropship.basegroups"),
	BASE_GROUPS_MAP_ORDER("redis.map.dropship.basegroups.order"),
	BASE_NEW_PRODUCT("redis.map.dropship.base.new"),
	BASE_SIZE_MAP("redis.map.base.size"),
	;
	
	private String value;

	private RedisKeyEnum(String value) {
		this.value = value;
	}

	public String getValue() {
		return value;
	}
	
}
