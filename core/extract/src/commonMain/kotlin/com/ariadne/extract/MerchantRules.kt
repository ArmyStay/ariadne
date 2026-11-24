package com.ariadne.extract

import com.ariadne.model.Category

/**
 * Local, deterministic hints from receipt text/merchant to capture metadata.
 * Suggestions are best-effort and always user-overridable.
 */
public object MerchantRules {
    private val keywordCategories: List<Pair<String, Category>> = listOf(
        "chai" to Category.Food,
        "tea" to Category.Food,
        "coffee" to Category.Food,
        "cafe" to Category.Food,
        "restaurant" to Category.Food,
        "bistro" to Category.Food,
        "bakery" to Category.Food,
        "diner" to Category.Food,
        "pizza" to Category.Food,
        "burger" to Category.Food,
        "noodle" to Category.Food,
        "sushi" to Category.Food,
        "grocery" to Category.Food,
        "supermarket" to Category.Food,
        "market" to Category.Food,
        "mart" to Category.Food,
        "bar" to Category.Food,
        "pub" to Category.Food,
        "taxi" to Category.Transport,
        "cab" to Category.Transport,
        "uber" to Category.Transport,
        "lyft" to Category.Transport,
        "grab" to Category.Transport,
        "bolt" to Category.Transport,
        "metro" to Category.Transport,
        "subway" to Category.Transport,
        "train" to Category.Transport,
        "rail" to Category.Transport,
        "bus" to Category.Transport,
        "ferry" to Category.Transport,
        "airline" to Category.Transport,
        "airport" to Category.Transport,
        "fuel" to Category.Transport,
        "petrol" to Category.Transport,
        "gas" to Category.Transport,
        "parking" to Category.Transport,
        "toll" to Category.Transport,
        "hotel" to Category.Lodging,
        "hostel" to Category.Lodging,
        "inn" to Category.Lodging,
        "resort" to Category.Lodging,
        "motel" to Category.Lodging,
        "stay" to Category.Lodging,
        "airbnb" to Category.Lodging,
        "booking" to Category.Lodging,
        "shoes" to Category.Shopping,
        "shoe" to Category.Shopping,
        "clothing" to Category.Shopping,
        "apparel" to Category.Shopping,
        "fashion" to Category.Shopping,
        "boutique" to Category.Shopping,
        "store" to Category.Shopping,
        "shop" to Category.Shopping,
        "mall" to Category.Shopping,
        "pharmacy" to Category.Shopping,
        "chemist" to Category.Shopping,
        "drug" to Category.Shopping,
        "book" to Category.Shopping,
        "souvenir" to Category.Shopping,
        "gift" to Category.Shopping,
        "museum" to Category.Entertainment,
        "cinema" to Category.Entertainment,
        "movie" to Category.Entertainment,
        "theatre" to Category.Entertainment,
        "theater" to Category.Entertainment,
        "concert" to Category.Entertainment,
        "ticket" to Category.Entertainment,
        "tour" to Category.Entertainment,
        "park" to Category.Entertainment,
        "spa" to Category.Entertainment,
    )

    public fun suggestCategory(merchant: String?): Category? {
        val haystack = merchant?.lowercase()?.trim().orEmpty()
        if (haystack.isEmpty()) return null
        return keywordCategories.firstOrNull { (keyword, _) -> haystack.contains(keyword) }?.second
    }
}