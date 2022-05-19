package com.lions.wantit.data.model.products

interface OnProductListener {
    fun onClick(product: ProductModel)
    fun onLongClick(product: ProductModel)
}