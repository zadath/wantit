package com.lions.wantit.ui.product

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import com.lions.wantit.R
import com.lions.wantit.data.model.products.OnProductListener
import com.lions.wantit.data.model.products.ProductAdapter
import com.lions.wantit.data.model.products.ProductModel
import com.lions.wantit.data.network.products.MainAux
import com.lions.wantit.databinding.ActivityProductBinding
import java.util.*

class Product : AppCompatActivity(), OnProductListener, MainAux {

    private lateinit var binding: ActivityProductBinding
    private lateinit var adapter: ProductAdapter
    private lateinit var firestoreListener: ListenerRegistration
    private var productSelected: ProductModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding =ActivityProductBinding.inflate(layoutInflater)
        setContentView(binding.root)

        configRecyclerView()
        //configFirestore()
        //configFirestoreRealtime()   //Se ejecutará solo onResume()
        configButtons()
        binding.efab.show()
    }

    override fun onResume(){
        super.onResume()
        configFirestoreRealtime()
    }

    override fun onPause() {
        super.onPause()
        firestoreListener.remove()
    }

    private fun configRecyclerView() {
        adapter = ProductAdapter(mutableListOf(), this)
        binding.recyclerView.apply{
            layoutManager = LinearLayoutManager(this@Product, LinearLayoutManager.VERTICAL, false)
            adapter = this@Product.adapter

        }

        /*(1..20).forEach{
            val producto = ProductModel(it.toString(), "Product $it", "modelo = $it", "codigo", "description $it",
            5, "verde", "23", "", 45, 23, 12)
            adapter.add(producto)
        }*/
    }

    private fun configFirestore(){
        val db = FirebaseFirestore.getInstance()

        db.collection("products")
            .orderBy("productName", Query.Direction.ASCENDING)
            .get()
            .addOnSuccessListener { snapshots ->
                for(document in snapshots){
                    val product = document.toObject(ProductModel::class.java)
                    product.id_Product = document.id
                    adapter.add(product)
                }
            }
            .addOnFailureListener{
                Toast.makeText(this, "Error al consultar la Base de Datos", Toast.LENGTH_SHORT).show()
            }
    }

    private fun configFirestoreRealtime(){
        val db = FirebaseFirestore.getInstance()
        // val productRef = db.collection("products")   //ORIGINAL que funciona
        val productRef = db.collection("products").orderBy("productName", Query.Direction.ASCENDING)

        firestoreListener = productRef.addSnapshotListener{snapshots, error ->
            if(error != null){
                Toast.makeText(this, "Error al consulta la Base de Datos", Toast.LENGTH_LONG).show()
                return@addSnapshotListener
            }

            for(snapshot in snapshots!!.documentChanges){
                val product = snapshot.document.toObject(ProductModel::class.java)
                product.id_Product = snapshot.document.id
                when(snapshot.type){
                    DocumentChange.Type.ADDED -> adapter.add(product)
                    DocumentChange.Type.MODIFIED -> adapter.update(product)
                    DocumentChange.Type.REMOVED -> adapter.delete((product)
                    )
                }
            }

        }

    }

    private fun configButtons(){
        binding.efab.setOnClickListener{
            productSelected = null
            AddDialogFragment().show(supportFragmentManager, AddDialogFragment::class.java.simpleName)
        }
    }
    override fun onClick(product: ProductModel) {
        productSelected = product
        AddDialogFragment().show(supportFragmentManager, AddDialogFragment::class.java.simpleName)
    }

    /*override fun onLongClick(product: ProductModel) {   ///Método que si funciona para el borrado cuando existe una imagen en STORAGE
        //evitar borrado equivocado
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.product_dialog_delete_title)
            .setMessage(R.string.product_dialog_delete_msg)
            .setPositiveButton(R.string.product_dialog_delete_confirm) { _, _ ->  //dejamos vacío son parámetros anonimos
                val db = FirebaseFirestore.getInstance()
                val productRef = db.collection("products")
                product.id_Product?.let { id ->
                    product.productImage?.let{url ->
                        // extraer referencia con base a la URL:
                        val photoRef = FirebaseStorage.getInstance().getReferenceFromUrl(url)

                        // obtener referencia del producto en Firebase STORAGE:
                        //FirebaseStorage.getInstance().reference.child(Constants.PATH_PRODUCT_IMAGES).child(id) // la omitimos para usar la refUrl
                        photoRef
                            .delete()
                            .addOnSuccessListener {
                                productRef.document(id)   //si se borró en STORAGE entonces borrar en Firestore
                                    .delete()
                                    .addOnFailureListener {
                                        Toast.makeText(this, "Error al eliminar registro en Firestore", Toast.LENGTH_LONG).show()
                                    }
                            }
                            .addOnFailureListener {
                                Toast.makeText(this, "Error al eliminar foto en Storage", Toast.LENGTH_LONG).show()
                            }
                    }
                }
            }
            .setNegativeButton(R.string.dialog_cancel, null)
            .show()
    }*/

    override fun onLongClick(product: ProductModel) {
        //evitar borrado equivocado
        MaterialAlertDialogBuilder(this)
                .setTitle(R.string.product_dialog_delete_title)
                .setMessage(R.string.product_dialog_delete_msg)
                .setPositiveButton(R.string.product_dialog_delete_confirm) { _, _ ->  //dejamos vacío son parámetros anonimos
                    val db = FirebaseFirestore.getInstance()
                    val productRef = db.collection("products")
                    product.id_Product?.let { id ->
                        if (product.productImage?.isEmpty() == true){
                            productRef.document(id)
                                    .delete()
                                    .addOnFailureListener {
                                        Toast.makeText(this, "Error al eliminar registro en Firestore", Toast.LENGTH_LONG).show()
                                    }
                        } else {
                            product.productImage?.let{url ->
                                // extraer referencia con base a la URL:
                                val photoRef = FirebaseStorage.getInstance().getReferenceFromUrl(url)

                                // obtener referencia del producto en Firebase STORAGE:
                                //FirebaseStorage.getInstance().reference.child(Constants.PATH_PRODUCT_IMAGES).child(id) // la omitimos para usar la refUrl
                                photoRef
                                        .delete()
                                        .addOnSuccessListener {
                                            productRef.document(id)   //si se borró en STORAGE entonces borrar en Firestore
                                                    .delete()
                                                    .addOnFailureListener {
                                                        Toast.makeText(this, "Error al eliminar registro en Firestore", Toast.LENGTH_LONG).show()
                                                    }
                                        }
                                        .addOnFailureListener {
                                            Toast.makeText(this, "Error al eliminar foto en Storage", Toast.LENGTH_LONG).show()
                                        }
                            }
                        }

                    }
                }
                .setNegativeButton(R.string.dialog_cancel, null)
                .show()
    }
    override fun getProductSelected(): ProductModel? = productSelected

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_search, menu)

        val search = menu?.findItem(R.id.menu_search)
        val searchView = search?.actionView as? SearchView
        searchView?.isSubmitButtonEnabled = true
    searchView?.setOnQueryTextListener(object : SearchView.OnQueryTextListener{
        override fun onQueryTextSubmit(query: String?): Boolean {


            return true
        }

        override fun onQueryTextChange(newText: String?): Boolean {
            TODO("Not yet implemented")
        }
    })
        return true
    }

   /* override fun onQueryTextSubmit(query: String?): Boolean {
        val productList = listOf(adapter)
        if(productList.contains(query)){
            /* if(listOf(adapter).contains(query)){

             }*/
            Log.i("Consulta Recycler!!!", "Se enconntró el valor ="+ query)
        }
        Log.i("Consulta Recycler!!!", "Se enconntró el valor ="+ query)
        return true
    }*/

    /*override fun onQueryTextChange(query: String?): Boolean {
        val productList = listOf(adapter)
        if(productList.contains(query)){
           /* if(listOf(adapter).contains(query)){

            }*/
            Log.i("Consulta Recycler!!!", "Se enconntró el valor ="+ query)
        }
        Log.i("Consulta Recycler!!!", "Se enconntró el valor ="+ query)

        return true
    }*/


}