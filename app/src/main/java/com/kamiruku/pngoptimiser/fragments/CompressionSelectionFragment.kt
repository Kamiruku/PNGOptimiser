package com.kamiruku.pngoptimiser.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import com.google.android.material.shape.CornerFamily
import com.kamiruku.pngoptimiser.R
import com.kamiruku.pngoptimiser.databinding.FragmentCompressionSelectionBinding

class CompressionSelectionFragment : Fragment() {
    private lateinit var binding: FragmentCompressionSelectionBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding =
            FragmentCompressionSelectionBinding.inflate(
                layoutInflater, container, false)
        val view = binding.root

        binding.shapeableImageView.shapeAppearanceModel =
            binding.shapeableImageView.shapeAppearanceModel
                .toBuilder()
                .setTopLeftCorner(CornerFamily.ROUNDED, 5f)
                .setTopRightCorner(CornerFamily.ROUNDED, 5f)
                .build()
        binding.shapeableImageView.setPadding(
            1.toPixels(),
            1.toPixels(),
            1.toPixels(),
            1.toPixels()
        )

        val compressionMethods: Array<String> = resources.getStringArray(R.array.compressionMethods)
        val adapterCompressionMethods: ArrayAdapter<String> =
            ArrayAdapter<String>(requireContext(), android.R.layout.simple_list_item_1, compressionMethods)
        binding.spinnerCompressionMethod.adapter = adapterCompressionMethods

        return view
    }

    private fun Int.toPixels(): Int {
        val scale = context?.resources?.displayMetrics?.density ?: 0f
        //Converts from dp/sp to pixels
        return (this * scale + 0.5).toInt()
    }
}