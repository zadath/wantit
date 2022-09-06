package com.lions.wantit.ui.product

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.zxing.integration.android.IntentIntegrator
import com.lions.wantit.Constants
import com.lions.wantit.R
import com.lions.wantit.data.model.products.EventPost
import com.lions.wantit.data.model.products.ProductModel
import com.lions.wantit.data.network.products.MainAux
import com.lions.wantit.databinding.FragmentDialogAddBinding
import java.io.ByteArrayOutputStream

class AddDialogFragment : DialogFragment(), DialogInterface.OnShowListener, AdapterView.OnItemClickListener {

    private var binding: FragmentDialogAddBinding? = null
    private var positiveButton: Button? = null
    private var negativeButton: Button? = null

    //private lateinit var contexto: Context
    private var product: ProductModel? = null
    private var photoSelectedUri: Uri? = null
    private var categoria: String? = null
    private var flag: Boolean = false

    private val resultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                photoSelectedUri = it.data?.data

                //binding?.imgProductPreview?.setImageURI(photoSelectedUri)
                binding?.let {
                    Glide.with(this)
                        .load(photoSelectedUri)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .centerCrop()
                        .into(it.imgProductPreview)
                }


            }
        }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        activity?.let { activity ->
            binding = FragmentDialogAddBinding.inflate(LayoutInflater.from(context))

            binding?.let {
                val builder = AlertDialog.Builder(activity)
                    .setTitle("Productos")
                    .setPositiveButton("Guardar", null)
                    .setNegativeButton("Cancelar", null)
                    .setView(it.root)
                   //.setIcon(R.drawable.ic_access_time)

                val dialog = builder.create()
                dialog.setOnShowListener(this)
                return dialog
            }
        }
        return super.onCreateDialog(savedInstanceState)
    }

    override fun onShow(dialogInterface: DialogInterface?) {
        initProduct()
        configButtons()
        initCategory()   // 20mayo

        val dialog = dialog as? AlertDialog
        dialog?.let {
            positiveButton = it.getButton(Dialog.BUTTON_POSITIVE)
            negativeButton = it.getButton(Dialog.BUTTON_NEGATIVE)

            product?.let { positiveButton?.setText("Actualizar") }

            positiveButton?.setOnClickListener {
                binding?.let {
                    //enableUI(false)

                    validarCampos()
                    if (flag) {  // si pasan todas las validaciones entonces ejecutará lo siguiente:
                        enableUI(false)
                        //uploadImage(product?.id_Product) { eventPost -> //este sube imagen sin reducirlas
                        uploadReduceImage(product?.id_Product, product?.productImage) { eventPost ->  //este sube imagen reduciendo calidad
                            if (eventPost.isSuccess) {
                                if (product == null) {

                                    val product = ProductModel(
                                            productName = it.etProducName.text.toString().trim(),
                                            model = it.etModel.text.toString().trim(),
                                            productCode = it.etProductCode.text.toString().trim(),
                                            description = it.etDescription.text.toString().trim(),
                                            quantity = it.etQuantity.text.toString().toInt(),
                                            priceBase = it.etPriceBase.text.toString().toDouble(),
                                            productImage = eventPost.photoUrl,
                                            category = categoria
                                    )
                                    save(product, eventPost.documentId!!)
                                } else {
                                    product?.apply {
                                        productName = it.etProducName.text.toString().trim()
                                        model = it.etModel.text.toString().trim()
                                        productCode = it.etProductCode.text.toString().trim()
                                        description = it.etDescription.text.toString().trim()
                                        quantity = it.etQuantity.text.toString().toInt()
                                        priceBase = it.etPriceBase.text.toString().toDouble()
                                        productImage = eventPost.photoUrl
                                        category = categoria  //20mayo

                                        update(this)
                                    }
                                }
                            }
                        }
                    } else {
                        Toast.makeText(activity, "Llenar campos indicados", Toast.LENGTH_LONG).show()
                    }
                }
            }
            negativeButton?.setOnClickListener {
                dismiss()
            }
        }
    }

    private fun validarCampos(): Boolean {
        flag = false
        var flagCounter = 0

        binding?.let {
            if (it.etProducName.text.toString().isEmpty()) {
                it.etProducName.error = "Campo requerido"
                flagCounter += 1
            }

            /*if (it.autoCompleteTextView.text.toString().isEmpty()) {
                it.autoCompleteTextView.error = "Elige categoría"
                flagCounter += 1
            }*/

            if (it.etPriceBase.text.toString().isEmpty()) {
                it.etPriceBase.error = "Captura precio"
                flagCounter += 1
            }

            if (it.etQuantity.text.toString().isEmpty()) {
                it.etQuantity.error = "Ingresa Cantidad"
                flagCounter += 1
            }

        }
        if (flagCounter == 0){
            flag = true
            Log.i("Validaciones Campos", "Contador final igual a: " + flagCounter)
        }
        return flag
    }




    private fun initProduct() {
        product = (activity as? MainAux)?.getProductSelected()
        product?.let { product ->
            binding?.let {
                dialog?.setTitle("Actualizar Producto")
                it.etProducName.setText(product.productName)
                it.etModel.setText(product.model)
                it.etProductCode.setText(product.productCode)
                it.etDescription.setText(product.description)
                it.etQuantity.setText(product.quantity.toString())
                it.etPriceBase.setText(product.priceBase.toString())
                it.autoCompleteTextView.setText(product.category.toString())  // prueba para cargar datos


                Glide.with(this)
                    .load(product.productImage)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .optionalFitCenter()
                    .centerCrop()
                    .fitCenter()
                    .into(it.imgProductPreview)

            }
        }

    }

    private fun initCategory(){
        // 20mayo2022
        //contexto = context?.applicationContext ?: contexto
        val listaItems = listOf("Producto", "Servicio")
        //val categorias = resources.getStringArray(R.array.categorias)
        //val adaptador = ArrayAdapter(contexto, R.layout.list_item, categorias)
        val adaptador = ArrayAdapter(requireContext(), R.layout.list_item, listaItems)
        with(binding!!.autoCompleteTextView){
            setAdapter(adaptador)
            onItemClickListener = this@AddDialogFragment
        } // 20mayo2022

    }

    override fun onItemClick(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        //val item = parent?.getItemAtPosition(position).toString()
        categoria = parent?.getItemAtPosition(position).toString()
        //Toast.makeText(contexto, item, Toast.LENGTH_LONG).show()
    }

    private fun configButtons() {
        binding?.let {
            it.ibProduct.setOnClickListener {
                openGallery()
            }

            it.btnReadCodes.setOnClickListener {
                initScanner()
            }
        }
    }

    private fun initScanner() {
        val integrator = IntentIntegrator.forSupportFragment(this).setDesiredBarcodeFormats(
            IntentIntegrator.ALL_CODE_TYPES
        )
        //IntentIntegrator.forSupportFragment(this).setDesiredBarcodeFormats(IntentIntegrator.ALL_CODE_TYPES)
        integrator.setPrompt("Lectura de códigos")
        integrator.setTorchEnabled(false)
        integrator.setBeepEnabled(true)
        integrator.initiateScan()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)

        if (result != null) {
            if (result.contents == null) {
                Toast.makeText(context, "No realizaste lectura", Toast.LENGTH_LONG).show()
                println("Cancelaste la lectura del código                  !!!")
            } else {
                Toast.makeText(
                    context,
                    "El valor escaneado es ${result.contents}",
                    Toast.LENGTH_LONG
                ).show()
                println("La lectura del código de barras arrojó:  ${result.contents}")
                binding?.etProductCode?.setText(result.contents)
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        resultLauncher.launch(intent)
    }

    // FUNCIÓN PARA SUBIR IMAGEN sin reducción en STORAGE
    private fun uploadImage(productId: String?, callback: (EventPost) -> Unit) {
        val eventPost = EventPost()
        eventPost.documentId = productId ?: FirebaseFirestore.getInstance()
            .collection("products")
            .document().id

        val storageRef = FirebaseStorage.getInstance().reference.child("product_images")

        photoSelectedUri?.let { uri ->
            binding?.let { binding ->
                binding.progressBar.visibility = View.VISIBLE
                val photoRef = storageRef.child(eventPost.documentId!!)
                photoRef.putFile(uri)
                    .addOnProgressListener {
                        val progress = (100 * it.bytesTransferred / it.totalByteCount).toInt()
                        it.run {
                            binding.progressBar.progress = progress
                            binding.tvProgress.text = String.format("%s%%", progress)
                        }
                    }
                    .addOnSuccessListener {
                        it.storage.downloadUrl.addOnSuccessListener { downloadUrl ->
                            //Log.i("URL", downloadUrl.toString())
                            eventPost.isSuccess = true
                            eventPost.photoUrl = downloadUrl.toString()
                            callback(eventPost)
                        }
                    }
                    .addOnFailureListener {
                        Toast.makeText(activity, "Error al subir imagen", Toast.LENGTH_LONG).show()
                        enableUI(true)

                        eventPost.isSuccess = false
                        callback(eventPost)
                    }
            }
        }
    }

    // FUNCION PARA subir imagen reduciendola así ahorramos espacio
    private fun uploadReduceImage(
        productId: String?,
        imageUrl: String?,
        callback: (EventPost) -> Unit
    ) {
        val eventPost = EventPost()
        imageUrl?.let { eventPost.photoUrl = it }
        eventPost.documentId = productId ?: FirebaseFirestore.getInstance()
            .collection("products")
            .document().id

        FirebaseAuth.getInstance().currentUser?.let { user ->
            val imageRef = FirebaseStorage.getInstance().reference.child(user.uid)
                .child(Constants.PATH_PRODUCT_IMAGES)
            val photoRef = imageRef.child(eventPost.documentId!!)
            //photoSelectedUri?.let { uri ->
            if (photoSelectedUri == null) {
                eventPost.isSuccess = true
                callback(eventPost)
            } else {
                binding?.let { binding ->
                    getBitmapFromUri(photoSelectedUri!!)?.let { bitmap ->
                        binding.progressBar.visibility = View.VISIBLE

                        val baos = ByteArrayOutputStream()
                        // para reducir un poco la calidad de la imagen:
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, baos)

                        photoRef.putBytes(baos.toByteArray())
                            .addOnProgressListener {
                                val progress =
                                    (100 * it.bytesTransferred / it.totalByteCount).toInt()
                                it.run {
                                    binding.progressBar.progress = progress
                                    binding.tvProgress.text = String.format("%s%%", progress)
                                }
                            }
                            .addOnSuccessListener {
                                it.storage.downloadUrl.addOnSuccessListener { downloadUrl ->
                                    //Log.i("URL", downloadUrl.toString())
                                    eventPost.isSuccess = true
                                    eventPost.photoUrl = downloadUrl.toString()
                                    callback(eventPost)
                                }
                            }
                            .addOnFailureListener {
                                Toast.makeText(activity, "Error al subir imagen", Toast.LENGTH_LONG)
                                    .show()
                                enableUI(true)

                                eventPost.isSuccess = false
                                callback(eventPost)
                            }
                    }

                }
            }
        }
    }

    private fun getBitmapFromUri(uri: Uri): Bitmap? {  //regresará un Bitmap que podría ser nulo
        // revisamos que activity no sea null
        activity?.let {
            val bitmap =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {  // depende de la versión de android se ejecutará la solución:
                    val source = ImageDecoder.createSource(it.contentResolver, uri)
                    ImageDecoder.decodeBitmap(source)
                } else {
                    MediaStore.Images.Media.getBitmap(it.contentResolver, uri)
                }

            //return bitmap  la omitimos para implementar la función de reducción de imagen:
            return getResizedImage(bitmap, 520)
        }
        return null
    }

    private fun getResizedImage(image: Bitmap, maxSize: Int): Bitmap {
        var width = image.width
        var height = image.height

        if (width <= maxSize && height <= maxSize) return image  //se compara que la imagen recibida sea mas pequeña del tamaño permitido

        // si el código siguiente se ejecuta es porque la validación anterior arrojó que la imagen recibida es mayor que el tamaña maximo permitido
        val bitmapRatio = width.toFloat() / height.toFloat()
        if (bitmapRatio > 1) {
            width = maxSize
            height = (width / bitmapRatio).toInt()
        } else {
            height = maxSize
            width = (height / bitmapRatio).toInt()
        }
        // se retorna y se creará la escala del bitmap:
        return Bitmap.createScaledBitmap(image, width, height, true)
    }

    private fun save(product: ProductModel, documentId: String) {
        val db = FirebaseFirestore.getInstance()
        db.collection(Constants.COLL_PRODUCTS)
            .document(documentId)
            .set(product)
            //.add(product)
            .addOnSuccessListener {
                Toast.makeText(activity, "Producto agregado ", Toast.LENGTH_LONG).show()
            }
            .addOnFailureListener {
                Toast.makeText(activity, "No se pudo agregar el producto", Toast.LENGTH_LONG).show()
            }
            .addOnCompleteListener {
                enableUI(true)
                binding?.progressBar?.visibility = View.INVISIBLE
                dismiss()
            }
    }

    private fun update(product: ProductModel) {
        val db = FirebaseFirestore.getInstance()

        product.id_Product?.let { id ->
            db.collection("products")
                .document(id)
                .set(product)
                .addOnSuccessListener {
                    Toast.makeText(activity, "Producto actualizado ", Toast.LENGTH_LONG).show()
                }
                .addOnFailureListener {
                    Toast.makeText(activity, "No se pudo actualizar el producto", Toast.LENGTH_LONG)
                        .show()
                }
                .addOnCompleteListener {
                    enableUI(true)
                    binding?.progressBar?.visibility = View.INVISIBLE
                    dismiss()
                }
        }
    }

    private fun enableUI(enable: Boolean) {
        positiveButton?.isEnabled = enable
        negativeButton?.isEnabled = enable

        binding?.let {
            with(it) {
                etProducName.isEnabled = enable
                etModel.isEnabled = enable
                etProductCode.isEnabled = enable
                etDescription.isEnabled = enable
                etQuantity.isEnabled = enable
                etPriceBase.isEnabled = enable
                progressBar.visibility = if (enable) View.INVISIBLE else View.VISIBLE
                tvProgress.visibility = if (enable) View.INVISIBLE else View.VISIBLE
            }
        }

    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }


}