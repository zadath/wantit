package com.lions.wantit.ui.principal

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.WindowInsets
import android.view.WindowManager
import android.widget.Toast
import com.firebase.ui.auth.AuthUI
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.lions.wantit.R
import com.lions.wantit.databinding.ActivityPrincipalBinding
import com.lions.wantit.ui.MainActivity
import com.lions.wantit.ui.order.OrderActivity
import com.lions.wantit.ui.product.Product

class Principal : AppCompatActivity() {
    private lateinit var binding: ActivityPrincipalBinding
    //private lateinit var firebaseAnalytics: FirebaseAnalytics

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPrincipalBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // firebaseAnalytics = Firebase.analytics
        @Suppress("DEPRECATION")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.hide(WindowInsets.Type.statusBars())
        } else {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
            )
        }

        binding.buttonProduct.setOnClickListener() {
            val intent = Intent(this, Product::class.java)
            startActivity(intent)
            finish()
        }

    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_sign_out -> {
                AuthUI.getInstance().signOut(this)
                    .addOnSuccessListener {
                        //Toast.makeText(this, "Sesión cerrada", Toast.LENGTH_SHORT).show()
                        // analytics:
                        /* firebaseAnalytics.logEvent(FirebaseAnalytics.Event.LOGIN){
                             param(FirebaseAnalytics.Param.SUCCESS, 100)  // 100 = signout successfully
                             param(FirebaseAnalytics.Param.METHOD, "sign_out")
                         }*/

                        val intent = Intent(this, MainActivity::class.java)
                        startActivity(intent)
                        finish()
                    }
                    .addOnCompleteListener {
                        if (it.isSuccessful) {
                            //val intent = Intent(this, MainActivity::class.java)
                            //startActivity(intent)
                            //binding.llProgress.visibility = View.VISIBLE
                            //binding.efab.hide()
                        } else {
                            Toast.makeText(this, "No se pudo cerrar la sesión!", Toast.LENGTH_LONG)
                                .show()

                            /*firebaseAnalytics.logEvent(FirebaseAnalytics.Event.LOGIN){
                                param(FirebaseAnalytics.Param.SUCCESS, 201)  // 201 = error signout
                                param(FirebaseAnalytics.Param.METHOD, "sign_out")
                            }*/
                        }
                    }
            }

            R.id.action_order_history -> startActivity(Intent(this, OrderActivity::class.java))
        }
        return super.onOptionsItemSelected(item)
    }
}