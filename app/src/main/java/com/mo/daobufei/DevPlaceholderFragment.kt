package com.mo.daobufei

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment

class DevPlaceholderFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val textView = TextView(requireContext()).apply {
            text = "敬请期待"
            textSize = 20f
            gravity = android.view.Gravity.CENTER
        }
        return textView
    }
}