package com.example.test

import android.content.ContentValues.TAG
import android.os.Bundle
import com.example.test.StateFragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import android.content.Intent
import android.nfc.Tag
import android.util.Log
import android.widget.Button
import com.example.test.R
import com.example.test.HomeFragment
import com.example.test.FamilyFragment
import com.google.firebase.firestore.FirebaseFirestore
import org.w3c.dom.Text
import java.io.BufferedReader
import java.io.InputStreamReader
import kotlin.math.log

/**
 * A simple [Fragment] subclass.
 * Use the [HomeFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class HomeFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var mParam1: String? = null
    private var mParam2: String? = null
    private var csvList = mutableListOf<List<Float>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (arguments != null) {
            mParam1 = arguments!!.getString(ARG_PARAM1)
            mParam2 = arguments!!.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val email = requireActivity().intent.getStringExtra("email")
        var name = view.findViewById<TextView>(R.id.textView)
        var mail = view.findViewById<TextView>(R.id.textView2)
        var phone = view.findViewById<TextView>(R.id.textView3)
        var address = view.findViewById<TextView>(R.id.textView4)
        var sub_btn = view.findViewById<Button>(R.id.ecg_sub)
        val db = FirebaseFirestore.getInstance()
        val ref = db.collection("USER")
        val query = ref.whereEqualTo("userEmail", email)
        query.get().addOnSuccessListener { documents ->
            for (document in documents) {
                val userName = document.data["userName"]
                val userPhone = document.data["userPhone"]
                val userAddress = document.data["userAddress"]
                // 在此處處理您所需的文檔數據
                name.text = "Name:" + userName.toString()
                mail.text = "Mail:" + email.toString()
                phone.text = "Phone:" + userPhone.toString()
                address.text = "Address:" + userAddress.toString()
            }
        }.addOnFailureListener { exception ->
            // 在此處處理查詢失敗的情況
            Log.d(TAG, "找不到此使用者")
        }

        //按下上傳按鈕
        sub_btn.setOnClickListener() {
            //處理CSV
            val inputStream = resources.openRawResource(R.raw.mitdb_360_test_ans)
            val reader = BufferedReader(InputStreamReader(inputStream))
            //val csvList = mutableListOf<List<Float>>()

            reader.use {
                var line = it.readLine() // 跳過首行標題
                while (line != null) {
                    val row = line.split(",") // 以逗號為分隔符號切割每行
                    val floatList = row.map { it.toFloat() }
                    csvList.add(floatList)
                    line = it.readLine()
                }
            }
            for (i in 0..199 ){
                val data = hashMapOf(
                    "ecgData" to csvList[i]
                )
                db.collection("USER")
                    .whereEqualTo("userEmail", email)
                    .get()
                    .addOnSuccessListener { querySnapshot ->
                        for (document in querySnapshot.documents) {
                            val documentId = document.id
                            // 在這裡處理獲取到的文件 ID
                            Log.d("Firestore", "對應的 ID 為：$documentId")
                            db.collection("USER")
                                .document(documentId)
                                .collection("Ecg_Data")
                                .add(data)
                        }
                    }
                    .addOnFailureListener { e ->
                        // 查詢失敗
                        Log.e("Firestore", "查詢文件失敗：$e")
                    }
            }
        }
    }

    companion object {
        // TODO: Rename parameter arguments, choose names that match
        // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
        private const val ARG_PARAM1 = "param1"
        private const val ARG_PARAM2 = "param2"

        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment HomeFragment.
         */
        // TODO: Rename and change types and number of parameters
        fun newInstance(param1: String?, param2: String?): HomeFragment {
            val fragment = HomeFragment()
            val args = Bundle()
            args.putString(ARG_PARAM1, param1)
            args.putString(ARG_PARAM2, param2)
            fragment.arguments = args
            return fragment
        }
    }
}