package com.example.test

import android.Manifest
import android.app.Activity.RESULT_OK
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Parcelable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import java.util.*


/**
 * A simple [Fragment] subclass.
 * Use the [RecordFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class RecordFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var mParam1: String? = null
    private var mParam2: String? = null
    private lateinit var mChartView: ChartView
    private var str: String? = null
    val stringArray = arrayOf(
        arrayOf(107, 108, 108, 108, 109, 111, 110, 111, 112, 113, 114, 115, 115, 116, 116, 116, 117, 117, 117, 116, 116, 117, 116, 115, 115, 115, 115, 113, 114, 114, 114, 113, 112, 112, 113, 111, 111, 111, 111, 110, 110, 109, 110, 109, 109, 109, 109, 108, 108, 108, 108, 108, 108, 108, 108, 108, 108, 108, 109, 109, 109, 108, 109, 109),
        arrayOf(109, 109, 110, 110, 110, 109, 109, 110, 110, 109, 109, 110, 110, 109, 109, 109, 110, 109, 109, 109, 109, 109, 108, 108, 109, 108, 107, 108, 108, 108, 107, 108, 109, 110, 110, 110, 111, 112, 111, 111, 112, 112, 111, 111, 111, 112, 111, 111, 111, 111, 111, 110, 109, 109, 107, 105, 103, 101, 99, 99, 101, 105, 111, 114),
        arrayOf(117, 119, 120, 119, 118, 117, 116, 114, 113, 111, 112, 110, 109, 109, 109, 108, 108, 107, 108, 108, 108, 107, 108, 108, 108, 107, 107, 108, 107, 106, 107, 107, 107, 107, 107, 107, 107, 106, 106, 106, 106, 106, 105, 106, 107, 106, 106, 106, 107, 107, 106, 107, 108, 109, 110, 110, 111, 111, 112, 112, 114, 115, 115, 114),
        arrayOf(116, 116, 116, 116, 116, 117, 116, 115, 115, 116, 115, 114, 114, 114, 114, 113, 112, 112, 112, 112, 111, 111, 111, 110, 110, 109, 110, 110, 109, 109, 109, 109, 108, 109, 109, 108, 108, 108, 109, 108, 108, 108, 108, 109, 108, 108, 108, 109, 109, 108, 109, 109, 109, 108, 109, 110, 110, 109, 109, 109, 110, 109, 109, 109),
        arrayOf(110, 110, 109, 110, 110, 109, 109, 109, 109, 109, 108, 109, 109, 109, 109, 109, 109, 109, 108, 107, 108, 108, 107, 107, 108, 109, 109, 109, 110, 111, 111, 111, 111, 112, 112, 112, 111, 112, 112, 111, 111, 111, 111, 110, 110, 110, 110, 108, 107, 106, 104, 101, 99, 99, 102, 106, 110, 115, 118, 120, 120, 119, 118, 117),
        arrayOf(115, 114, 113, 112, 110, 109, 109, 109, 108, 107, 107, 108, 108, 107, 107, 107, 108, 107, 107, 107, 108, 108, 107, 108, 108, 107, 107, 107, 107, 107, 107, 106, 107, 107, 106, 106, 107, 107, 106, 106, 106, 107, 106, 106, 108, 108, 108, 109, 109, 111, 111, 111, 112, 113, 114, 114, 115, 116, 116, 116, 116, 117, 117, 116),
        arrayOf(116, 117, 116, 116, 115, 115, 114, 114, 113, 113, 113, 113, 112, 112, 111, 111, 110, 110, 110, 110, 109, 108, 108, 109, 109, 109, 109, 109, 108, 108, 108, 109, 108, 108, 108, 109, 109, 108, 108, 108, 109, 108, 108, 109, 110, 109, 108, 109, 109, 109, 109, 109, 110, 110, 110, 109, 110, 110, 109, 109, 110, 110, 109, 109),
        arrayOf(109, 110, 109, 108, 108, 109, 108, 107, 108, 108, 107, 108, 108, 110, 110, 110, 111, 111, 112, 111, 111, 111, 111, 111, 111, 111, 112, 111, 110, 111, 111, 110, 110, 110, 110, 108, 106, 103, 101, 99, 99, 102, 108, 113, 117, 120, 122, 122, 120, 118, 116, 115, 113, 111, 110, 110, 109, 108, 108, 108, 108, 107, 107, 108),
        arrayOf(108, 107, 107, 107, 107, 107, 106, 107, 107, 107, 106, 107, 107, 107, 106, 106, 107, 106, 106, 106, 106, 106, 106, 105, 106, 106, 105, 106, 106, 107, 107, 107, 108, 109, 109, 110, 110, 111, 112, 112, 113, 114, 115, 115, 115, 117, 117, 117, 117, 117, 118, 117, 117, 117, 117, 116, 115, 115, 115, 114, 114, 113, 113, 113)
    )

    val byteArray = stringArray.flatMap { row ->
        row.map { value -> value.toInt().toByte() }
    }.toByteArray()

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
        return inflater.inflate(R.layout.fragment_record, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mChartView = view.findViewById(R.id.chartView)
        mChartView.Wave_Draw(byteArray)
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
         * @return A new instance of fragment RecordFragment.
         */
        // TODO: Rename and change types and number of parameters
        fun newInstance(param1: String?, param2: String?): RecordFragment {
            val fragment = RecordFragment()
            val args = Bundle()
            args.putString(ARG_PARAM1, param1)
            args.putString(ARG_PARAM2, param2)
            fragment.arguments = args
            return fragment
        }
    }
}