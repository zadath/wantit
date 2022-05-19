package com.lions.wantit.data.model.order

interface OnOrderListener {
    fun onStartChat(order: Order)
    fun onStatusChange(order: Order)
}