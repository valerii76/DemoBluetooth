package com.example.demobluetooth

import android.content.Context
import android.media.*
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import kotlin.concurrent.thread


private class InputDeviceListAdapter(val context: Context): BaseAdapter() {

    var selectedItem: Int = -1
    val inflater: LayoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    var audioManager: AudioManager? = null
    var mics: List<AudioDeviceInfo> = listOf()

    fun update() {
        audioManager = context.getSystemService(AppCompatActivity.AUDIO_SERVICE) as AudioManager?
        mics = audioManager?.getDevices(AudioManager.GET_DEVICES_INPUTS)!!.toList()
        selectedItem = -1
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

        val mic = getItem(index) as AudioDeviceInfo
        type.text = mic.type.toString()
        id.text = mic.id.toString()
        address.text = mic.address
        description.text = mic.productName

        return rowView
    }
}

private class OutputDeviceListAdapter(val context: Context): BaseAdapter() {

    var selectedItem: Int = -1
    val inflater: LayoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    var audioManager: AudioManager? = null
    var outputDevices: List<AudioDeviceInfo> = listOf()

    fun update() {
        audioManager = context.getSystemService(AppCompatActivity.AUDIO_SERVICE) as AudioManager?
        outputDevices = audioManager?.getDevices(AudioManager.GET_DEVICES_OUTPUTS)!!.toList()
        selectedItem = -1
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

    private val TAG: String = "BluetoothDemo"

    private lateinit var lstInput: ListView
    private lateinit var lstOutput: ListView
    private lateinit var inputAdapter: InputDeviceListAdapter
    private lateinit var outputAdapter: OutputDeviceListAdapter
    private lateinit var audioRecorder: AudioRecord
    private lateinit var audioPlayer: AudioTrack
    private var micRecord: ByteArrayOutputStream = ByteArrayOutputStream()

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

        lstInput.adapter = inputAdapter
        lstOutput.adapter = outputAdapter

        lstInput.setOnItemClickListener { _, view, index, _ -> run {
            view.isSelected = true
            inputAdapter.selectedItem = index
        }}
        lstOutput.setOnItemClickListener {_, view, index, _ -> run {
            view.isSelected = true
            outputAdapter.selectedItem = index
        }}

        btnUpdate.setOnClickListener { run {
            inputAdapter.update()
            outputAdapter.update()
        } }

        btnStart.setOnClickListener { run {
            // Set preferred device
            if (inputAdapter.selectedItem != -1) {
                val device = inputAdapter.getItem(inputAdapter.selectedItem) as AudioDeviceInfo
                audioRecorder.preferredDevice = device
            }

            micRecord.reset()
            audioRecorder.startRecording()
            thread (start = true) {
                var totalSize = 0
                val bufferSize = 1024 * 16
                val buffer = ByteArray(bufferSize)
                while (audioRecorder.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                    val readCount = audioRecorder.read(buffer,0, bufferSize)
                    totalSize += readCount
                    micRecord.write(buffer, 0, readCount)
                    Log.d(TAG, "readCount: $readCount, totalSize: $totalSize")
                }
            }
        } }

        btnStop.setOnClickListener { run {
            audioRecorder.stop()
        } }

        btnPlay.setOnClickListener { run {
            // Set preferred device
            if (outputAdapter.selectedItem != -1) {
                val device = outputAdapter.getItem(outputAdapter.selectedItem) as AudioDeviceInfo
                audioPlayer.preferredDevice = device
            }

            val inputBuffer = micRecord.toByteArray()
            val data = ByteArrayInputStream(inputBuffer)
            audioPlayer.play()

            var readSize = 0
            val size = 1024*16
            val buffer = ByteArray(size)
            while (readSize != -1) {
                readSize = data.read(buffer)
                if (readSize != -1)
                    audioPlayer.write(buffer, 0, readSize)
            }

            audioPlayer.stop()
            Toast.makeText(this@MainActivity, "Play stopped", Toast.LENGTH_LONG).show()
        } }

        inputAdapter.update()
        outputAdapter.update()

        // Create AudioRecorder
        val inputSampleRate = 8000
        audioRecorder = AudioRecord.Builder()
            .setAudioSource(MediaRecorder.AudioSource.MIC)
            .setAudioFormat(AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(inputSampleRate)
                .setChannelMask(AudioFormat.CHANNEL_IN_STEREO)
                .build())
            .setBufferSizeInBytes(4 * AudioRecord.getMinBufferSize(inputSampleRate,AudioFormat.CHANNEL_IN_STEREO, AudioFormat.ENCODING_PCM_16BIT))
            .build()

        // Create AudioTrack (audio player)
        val outputSampleRate = 8000
        audioPlayer = AudioTrack.Builder()
            .setAudioFormat(AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(outputSampleRate)
                .setChannelMask(AudioFormat.CHANNEL_IN_STEREO)
                .build())
            .setBufferSizeInBytes(4 * AudioRecord.getMinBufferSize(outputSampleRate,AudioFormat.CHANNEL_IN_STEREO, AudioFormat.ENCODING_PCM_16BIT))
            .build()
    }
}