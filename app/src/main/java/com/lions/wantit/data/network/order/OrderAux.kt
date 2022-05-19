package com.lions.wantit.data.network.order

import com.lions.wantit.data.model.order.Order

interface OrderAux {
    fun getOrderSelected(): Order
}