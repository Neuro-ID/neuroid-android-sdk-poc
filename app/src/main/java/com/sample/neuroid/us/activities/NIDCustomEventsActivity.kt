package com.sample.neuroid.us.activities

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.neuroid.tracker.NeuroIDImpl
import com.sample.neuroid.us.MyApplicationDemo
import com.sample.neuroid.us.R
import com.sample.neuroid.us.databinding.NidActivityCustomEventsBinding

class NIDCustomEventsActivity : AppCompatActivity() {
    private lateinit var binding: NidActivityCustomEventsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTitle(R.string.nid_custom_events_title_activity)
        binding = DataBindingUtil.setContentView(this, R.layout.nid_activity_custom_events)
        NeuroIDImpl.getInstance()?.let {
            if (it.isStopped()) {
                it.start()
            }
            it.setScreenName("NID CUSTOM EVENT PAGE")
        }

        binding.apply {
            buttonSendCustomEvent.setOnClickListener {
                showToast()
            }
            buttonSendFormSubmit.setOnClickListener {
                NeuroIDImpl.getInstance()?.formSubmit()
                showToast()
            }
            buttonSendFormSuccess.setOnClickListener {
                NeuroIDImpl.getInstance()?.formSubmitSuccess()
                showToast()
            }
            buttonSendFormFailure.setOnClickListener {
                NeuroIDImpl.getInstance()?.formSubmitFailure()
                showToast()
            }
        }
    }

    private fun showToast() {
        Toast.makeText(
            this@NIDCustomEventsActivity,
            R.string.nid_custom_events_saved_event_activity,
            Toast.LENGTH_LONG
        ).show()
    }
}