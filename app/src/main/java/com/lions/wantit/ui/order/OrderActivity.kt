package com.lions.wantit.ui.order

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.lions.wantit.Constants
import com.lions.wantit.R
import com.lions.wantit.data.model.order.OnOrderListener
import com.lions.wantit.data.model.order.Order
import com.lions.wantit.data.model.order.OrderAdapter
import com.lions.wantit.data.network.fcm.NotificationRS
import com.lions.wantit.data.network.order.OrderAux
import com.lions.wantit.databinding.ActivityOrderBinding
import com.lions.wantit.ui.chat.ChatFragment

class OrderActivity : AppCompatActivity(), OnOrderListener, OrderAux {

    private lateinit var binding: ActivityOrderBinding
    private lateinit var adapter: OrderAdapter
    private lateinit var orderSelected: Order
    private val aValues: Array<String> by lazy {

        resources.getStringArray(R.array.status_value)
    }

    private val aKeys: Array<Int> by lazy {
        resources.getIntArray(R.array.status_key).toTypedArray()
    }


    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        binding = ActivityOrderBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupFirestore()
    }

    private fun setupRecyclerView() {
        adapter = OrderAdapter(mutableListOf(), this)
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@OrderActivity)
            adapter = this@OrderActivity.adapter
        }
    }

    private fun setupFirestore() {
        val db = FirebaseFirestore.getInstance()
        db.collection(Constants.COLL_REQUEST)
            .orderBy(Constants.PROP_DATE, Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener {
                for (document in it) {
                    val order = document.toObject(Order::class.java)
                    order.id = document.id
                    adapter.add(order)
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al consultar datos en DB", Toast.LENGTH_SHORT).show()

            }
    }

    private fun notifyClient(order: Order) {
        val db = FirebaseFirestore.getInstance()

        db.collection(Constants.COLL_USERS)
            .document(order.clientId)
            .collection(Constants.COLL_TOKENS)
            .get()
            .addOnSuccessListener {
                var tokensStr = ""
                for (document in it) {
                    val tokenMap = document.data
                    tokensStr += "${tokenMap.getValue(Constants.PROP_TOKEN)}, " // se van concatenando separados por coma
                }
                if (tokensStr.length > 0) {
                    tokensStr = tokensStr.dropLast(1)
                    //con dropLast se quita la última coma

                    var names = ""
                    order.products.forEach {
                        names += "${it.value.name}, "  // por cada producto se extrae el nombre
                    }
                    names = names.dropLast(2)
                    // Extraer el valor del estado actual
                    val index = aKeys.indexOf(order.status)

                    val notificationRS = NotificationRS()
                    notificationRS.sendNotification(
                        "Tu pedido ha sido ${aValues[index]}",
                        names, tokensStr
                    )
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al consultar datos", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onStartChat(order: Order) {
        orderSelected = order

        val fragment = ChatFragment()

        supportFragmentManager
            .beginTransaction()
            .add(R.id.containerMain, fragment)
            .addToBackStack(null)
            .commit()
    }

    override fun onStatusChange(order: Order) {
        val db = FirebaseFirestore.getInstance()
        db.collection(("requests"))
            .document(order.id)
            .update("status", order.status)
            .addOnSuccessListener {
                Toast.makeText(this, "Orden actualizada", Toast.LENGTH_SHORT).show()
                notifyClient(order)
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al actualizar la orden", Toast.LENGTH_SHORT).show()

            }

    }

    override fun getOrderSelected(): Order = orderSelected
}