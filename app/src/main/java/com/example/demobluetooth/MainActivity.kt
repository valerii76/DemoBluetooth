package com.example.demobluetooth

import android.content.Context
import android.database.DataSetObserver
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.media.MicrophoneInfo
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*


private class InputDeviceListAdapter(val context: Context): BaseAdapter() {

    val inflater: LayoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    var audioManager: AudioManager? = null
    var mics: List<MicrophoneInfo> = listOf()

    fun update() {
        audioManager = context.getSystemService(AppCompatActivity.AUDIO_SERVICE) as AudioManager?
        mics = audioManager?.microphones!!
        notifyDataSetChanged()
    }

    override fun getCount(): Int {
        return mics.size
    }

    override fun getItem(index: Int): Any {
        return mics[index]
    }

    override fun getItemId(index: Int): Long {
        return index.toLong()
    }

    override fun getView(index: Int, view: View?, parent: ViewGroup?): View {
        val rowView = inflater.inflate(R.layout.lst_item_input,  parent, false)
        val type = rowView.findViewById<TextView>(R.id.lstInputType)
        val id = rowView.findViewById<TextView>(R.id.lstInputId)
        val address = rowView.findViewById<TextView>(R.id.lstInputAddress)
        val description = rowView.findViewById<TextView>(R.id.lstInputDescription)

        val mic = getItem(index) as MicrophoneInfo
        type.text = mic.type.toString()
        id.text = mic.id.toString()
        address.text = mic.address
        description.text = mic.description

        return rowView
    }
}

private class OutputDeviceListAdapter(val context: Context): BaseAdapter() {
    val inflater: LayoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    var audioManager: AudioManager? = null
    var outputDevices: List<AudioDeviceInfo> = listOf()

    fun update() {
        audioManager = context.getSystemService(AppCompatActivity.AUDIO_SERVICE) as AudioManager?
        outputDevices = audioManager?.getDevices(AudioManager.GET_DEVICES_OUTPUTS)!!.toList()
        notifyDataSetChanged()
    }

    override fun getCount(): Int {
        return outputDevices.size
    }

    override fun getItem(index: Int): Any {
        return outputDevices[index]
    }

    override fun getItemId(index: Int): Long {
        return index.toLong()
    }

    override fun getView(index: Int, view: View?, parent: ViewGroup?): View {
        val rowView = inflater.inflate(R.layout.lst_item_output, parent, false)
        val type = rowView.findViewById<TextView>(R.id.lstOutputType)
        val id = rowView.findViewById<TextView>(R.id.lstOutputId)
        val address = rowView.findViewById<TextView>(R.id.lstOutputAddress)
        val product = rowView.findViewById<TextView>(R.id.lstOutputProductName)

        val output = getItem(index) as AudioDeviceInfo
        type.text = output.type.toString()
        id.text = output.id.toString()
        address.text = output.address
        product.text = output.productName

        return rowView
    }
}

class MainActivity : AppCompatActivity() {

    var lstInput: ListView? = null
    var lstOutput: ListView? = null
    private var inputAdapter: InputDeviceListAdapter? = null
    private var outputAdapter: OutputDeviceListAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        inputAdapter = InputDeviceListAdapter(this)
        outputAdapter = OutputDeviceListAdapter(this)
        lstInput = findViewById(R.id.lstInput)
        lstOutput = findViewById(R.id.lstOutput)

        val btnUpdate = findViewById<Button>(R.id.btnUpdateDevices)
        val btnStart = findViewById<Button>(R.id.btnStartRecord)
        val btnStop = findViewById<Button>(R.id.btnStopRecord)
        val btnPlay = findViewById<Button>(R.id.btnPlayRecord)

        lstInput?.adapter = inputAdapter
        lstOutput?.adapter = outputAdapter

        btnUpdate.setOnClickListener { v ->
            run {
                inputAdapter?.update()
                outputAdapter?.update()
            }
        }
    }
}