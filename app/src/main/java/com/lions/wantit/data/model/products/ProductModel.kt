package com.lions.wantit.data.model.products

import com.google.firebase.firestore.Exclude

data class ProductModel(
    @get:Exclude var id_Product: String? = null,
    var productName: String? = null,
    var model: String? = null,
    var category: String? = null,
    var productCode: String? = null,
    var description: String? = null,
    var quantity: Int? = 0,
    var color: String? = null,
    var size: String? = null,
    var productImage: String? = null,
    var priceBase: Double? = 0.0,
    var priceSale: Double? = 0.0,
    var pricePromo: Double? = 0.0){

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ProductModel

        if (id_Product != other.id_Product) return false

        return true
    }

    override fun hashCode(): Int {
        return id_Product?.hashCode() ?: 0
    }
}
