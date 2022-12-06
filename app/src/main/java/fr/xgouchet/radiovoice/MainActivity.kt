package fr.xgouchet.radiovoice

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast

class MainActivity : AppCompatActivity() {


    lateinit var buttonStart : View
    lateinit var buttonStop : View
    lateinit var textStatus : TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        buttonStart = findViewById(R.id.btn_start)
        buttonStop = findViewById(R.id.btn_stop)
        textStatus = findViewById(R.id.status)

        buttonStart.setOnClickListener {
            Toast.makeText(this, "Hello Start", Toast.LENGTH_LONG).show()
            val serviceIntent = RadioVoiceService.buildStartIntent(this)
            startService(serviceIntent)
        }

        buttonStop.setOnClickListener {
            val serviceIntent = RadioVoiceService.buildStopIntent(this)
            startService(serviceIntent)
        }
    }
}