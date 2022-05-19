package com.lions.wantit.ui.chat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.lions.wantit.R
import com.lions.wantit.data.model.chat.ChatAdapter
import com.lions.wantit.data.model.chat.Message
import com.lions.wantit.data.model.chat.OnChatListener
import com.lions.wantit.data.model.order.Order
import com.lions.wantit.data.network.order.OrderAux
import com.lions.wantit.databinding.FragmentChatBinding

class ChatFragment : Fragment(), OnChatListener {

    private var binding: FragmentChatBinding? = null
    private lateinit var adapter: ChatAdapter
    private var order: Order? = null


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentChatBinding.inflate(inflater, container, false)
        binding?.let {
            return it.root
        }
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        getOrder()
        setupRecyclerView()
        setupButtons()
    }


    private fun getOrder() {
        order = (activity as? OrderAux)?.getOrderSelected()
        order?.let {
            setupActionBar()
            setupRealtimeDatabase()
        }
    }

    private fun setupRealtimeDatabase() {
        order?.let {
            val database = Firebase.database
            val chatRef = database.getReference("chats").child(it.id)
            val childListener = object : ChildEventListener {
                override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                    val message = snapshot.getValue(Message::class.java)
                    message?.let { message ->
                        snapshot.key?.let {
                            message.id = it
                        }
                        FirebaseAuth.getInstance().currentUser?.let { user ->
                            message.myUid = user.uid
                        }
                        adapter.add(message)
                        binding?.recyclerView?.scrollToPosition(adapter.itemCount - 1)
                    }
                }

                override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                    val message = snapshot.getValue(Message::class.java)
                    message?.let { message ->
                        snapshot.key?.let {
                            message.id = it
                        }
                        FirebaseAuth.getInstance().currentUser?.let { user ->
                            message.myUid = user.uid
                        }
                        adapter.update(message)
                    }
                }

                override fun onChildRemoved(snapshot: DataSnapshot) {
                    val message = snapshot.getValue(Message::class.java)
                    message?.let { message ->
                        snapshot.key?.let {
                            message.id = it
                        }
                        FirebaseAuth.getInstance().currentUser?.let { user ->
                            message.myUid = user.uid
                        }
                        adapter.delete(message)
                    }
                }

                override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {
                    // No se necesitará
                }

                override fun onCancelled(error: DatabaseError) {
                    binding?.let {
                        Snackbar.make(it.root, "Error al cargar mensajes", Snackbar.LENGTH_LONG)
                            .show()
                    }
                }

            }
            chatRef.addChildEventListener(childListener)
        }
    }


    private fun setupRecyclerView() {
        adapter = ChatAdapter(mutableListOf(), this)
        binding?.let {
            it.recyclerView.apply {
                layoutManager = LinearLayoutManager(context).also {
                    it.stackFromEnd = true
                }
                adapter = this@ChatFragment.adapter
            }
        }

        /*(1..20).forEach{
            adapter.add(Message(it.toString(), if(it%4 == 0) "Hi how r u ? Hi how r u ? Hi how r u ?" else "Hi how r u ?",
                if(it%3 == 0) "Tu" else "Yo", "Yo"))
        }*/

    }

    private fun setupButtons() {
        binding?.let { binding ->
            binding.ibSend.setOnClickListener {
                sendMessage()
            }
        }
    }

    private fun sendMessage() {
        binding?.let { binding ->
            order?.let {
                val database = Firebase.database
                val chatRef =
                    database.getReference("chats").child(it.id) // se creará un chat por pedido
                val user = FirebaseAuth.getInstance().currentUser
                user?.let {
                    val temporal = binding.etMessage.text.toString()
                    //val message = Message(message = binding.etMessage.text.toString().trim(), sender = it.uid)
                    val message = Message(message = temporal, sender = it.uid)

                    //println("Mensaje--------------   " + temporal)
                    binding.ibSend.isEnabled = false

                    chatRef.push().setValue(message)
                        .addOnSuccessListener {
                            binding.etMessage.setText("")
                        }
                        .addOnCompleteListener {
                            binding.ibSend.isEnabled = true
                        }
                }
            }
        }
    }

    private fun setupActionBar() {
        (activity as? AppCompatActivity)?.let {
            it.supportActionBar?.setDisplayHomeAsUpEnabled(true)
            it.supportActionBar?.title = getString(R.string.chat_title)
            setHasOptionsMenu(true)

        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            activity?.onBackPressed()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onDestroy() {
        (activity as? AppCompatActivity)?.let {
            it.supportActionBar?.setDisplayHomeAsUpEnabled(false)
            it.supportActionBar?.title = getString(R.string.order_title)
            setHasOptionsMenu(false)
        }
        super.onDestroy()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    override fun deleteMessage(message: Message) {
        order?.let {
            val database = Firebase.database
            val messageRef = database.getReference("chats").child(it.id).child(message.id)
            messageRef.removeValue { error, ref ->
                binding?.let {
                    if (error != null) {
                        Snackbar.make(it.root, "Error al eliminar mensajes", Snackbar.LENGTH_LONG)
                            .show()
                    } else {
                        Snackbar.make(it.root, "Mensaje eliminado", Snackbar.LENGTH_LONG).show()
                    }
                }
            }

        }
    }
}