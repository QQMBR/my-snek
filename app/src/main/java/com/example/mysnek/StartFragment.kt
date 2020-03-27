package com.example.mysnek

import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import kotlinx.android.synthetic.main.fragment_start.*

/**
 * A simple [Fragment] subclass.
 * Use the [StartFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class StartFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_start, container, false)

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        textView.movementMethod = ScrollingMovementMethod()
        
        button.setOnClickListener {
            val action = StartFragmentDirections.actionStartFragmentToGameFragment()

            findNavController().navigate(action)
        }

        button2.setOnClickListener {
            val action = StartFragmentDirections.actionStartFragmentToSettingsFragment()

            findNavController().navigate(action)
        }
    }

    companion object {
        /**
         * @return A new instance of fragment StartFragment.
         */
        @JvmStatic
        fun newInstance() = StartFragment()
    }
}
