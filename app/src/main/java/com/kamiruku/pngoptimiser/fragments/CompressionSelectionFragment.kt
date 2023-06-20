package com.kamiruku.pngoptimiser.fragments

import android.content.res.ColorStateList
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.google.android.material.shape.CornerFamily
import com.kamiruku.pngoptimiser.R
import com.kamiruku.pngoptimiser.activities.ViewModel
import com.kamiruku.pngoptimiser.databinding.FragmentCompressionSelectionBinding


class CompressionSelectionFragment : Fragment() {
    private lateinit var binding: FragmentCompressionSelectionBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding =
            FragmentCompressionSelectionBinding.inflate(
                layoutInflater, container, false)
        val view = binding.root

        val backgroundPaddingSize = 1.toPixels()
        //Rounded corners for background
        binding.shapeableImageView.shapeAppearanceModel =
            binding.shapeableImageView.shapeAppearanceModel
                .toBuilder()
                .setTopLeftCorner(CornerFamily.ROUNDED, 5f)
                .setTopRightCorner(CornerFamily.ROUNDED, 5f)
                .build()
        //Sets padding for background
        binding.shapeableImageView.setPadding(
            backgroundPaddingSize,
            backgroundPaddingSize,
            backgroundPaddingSize,
            backgroundPaddingSize
        )
        //Sets colour of padding
        binding.shapeableImageView.strokeColor =
            ColorStateList.valueOf(requireContext().getColor(R.color.white))
        //Sets progress of continuous seekbar to max - only on main activity oncreate
        binding.seekBarQualityContinuous.progress = binding.seekBarQualityContinuous.max
        //Sets attribute of discrete seekbar - max is amount of luban options - 1
        binding.seekBarQualityDiscrete.apply {
            max = 1
            progress = binding.seekBarQualityDiscrete.max
            visibility = View.INVISIBLE
        }

        val compressionMethods: Array<String> = resources.getStringArray(R.array.compressionMethods)
        //Array adapter showing compression method entries
        val adapterCompressionMethods: ArrayAdapter<String> =
            ArrayAdapter<String>(requireContext(), android.R.layout.simple_list_item_1, compressionMethods)
        //Then sets adapter
        binding.spinnerCompressionMethod.adapter = adapterCompressionMethods

        //Sets up viewModel to communicate with parent activity
        val viewModel: ViewModel by activityViewModels()

        //Listener on spinner to respond to any selection
        binding.spinnerCompressionMethod.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parentView: AdapterView<*>?) { }

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                //Communicates to view model different compression method has been selected
                viewModel.changeCompression(binding.spinnerCompressionMethod.selectedItem.toString())

                when (binding.spinnerCompressionMethod.selectedItem.toString()) {
                    "Luban" -> {
                        //Luban uses a discrete seekbar
                        enableQualityToggle(true)
                        binding.seekBarQualityDiscrete.visibility = View.VISIBLE
                        binding.seekBarQualityContinuous.visibility = View.INVISIBLE

                        binding.editTextQuality.setText(binding.seekBarQualityDiscrete.progress.toString())
                    }
                    "Original" -> {
                        //No point in showing quality since it won't do anything
                        enableQualityToggle(false)
                    }
                    else -> {
                        //Every other compression method uses a continuious seekbar
                        enableQualityToggle(true)
                        binding.seekBarQualityDiscrete.visibility = View.INVISIBLE
                        binding.seekBarQualityContinuous.visibility = View.VISIBLE

                        binding.editTextQuality.setText(binding.seekBarQualityContinuous.progress.toString())
                    }
                }
            }
        }

        //Listener on seekbar to check progress
        val seekBarListener = object: OnSeekBarChangeListener {
            override fun onStopTrackingTouch(seekBar: SeekBar) { }
            override fun onStartTrackingTouch(seekBar: SeekBar) { }
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (binding.spinnerCompressionMethod.selectedItem.toString() == "Luban") {
                    //Progress from seekbar will always be 1 less
                    binding.editTextQuality.setText((progress + 1).toString())
                    viewModel.changeQuality(progress)
                } else {
                    //Communicates to view model if quality changes
                    binding.editTextQuality.setText(progress.toString())
                    viewModel.changeQuality(progress)
                }
            }
        }

        //Sets seekBar listener to both seekbars
        binding.seekBarQualityContinuous.setOnSeekBarChangeListener(seekBarListener)
        binding.seekBarQualityDiscrete.setOnSeekBarChangeListener(seekBarListener)

        //Listener on edit text to check user input
        binding.editTextQuality.addTextChangedListener(object: TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) { }
            override fun afterTextChanged(s: Editable?) { }
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                //Calling toInt on empty string will throw error so just use toIntOrNull and assign 0 to null values
                val progress = binding.editTextQuality.text.toString().toIntOrNull()

                //Whenever there is an empty string, quality will be at least
                //Whenever quality is greater than max, quality will be max
                //This applies to both seek bars / edit text
                if (binding.spinnerCompressionMethod.selectedItem.toString() == "Luban") {
                    when (progress) {
                        null -> binding.seekBarQualityDiscrete.progress = 0
                        in 0..1 ->
                            binding.seekBarQualityDiscrete.progress =
                                binding.editTextQuality.text.toString().toInt() - 1
                        else -> binding.seekBarQualityDiscrete.progress = binding.seekBarQualityDiscrete.max
                    }
                } else {
                    when (progress) {
                        null -> binding.seekBarQualityContinuous.progress = 0
                        in 0..100 ->
                            binding.seekBarQualityContinuous.progress =
                                binding.editTextQuality.text.toString().toInt()
                        else -> binding.seekBarQualityContinuous.progress = binding.seekBarQualityContinuous.max
                    }

                }
                
                viewModel.changeQuality(progress ?: 0)
                //Puts cursor at *end* of edit text
                binding.editTextQuality.setSelection(binding.editTextQuality.text.length)
            }

        })
        return view
    }

    private fun enableQualityToggle(enabled: Boolean) {
        binding.apply {
            seekBarQualityDiscrete.isEnabled = enabled
            seekBarQualityContinuous.isEnabled = enabled

            editTextQuality.isEnabled = enabled
        }
    }
    private fun Int.toPixels(): Int {
        val scale = requireContext().resources?.displayMetrics?.density ?: 0f
        //Converts from dp/sp to pixels
        return (this * scale + 0.5).toInt()
    }
}