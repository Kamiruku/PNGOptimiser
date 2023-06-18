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
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat.getColor
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.google.android.material.shape.CornerFamily
import com.kamiruku.pngoptimiser.R
import com.kamiruku.pngoptimiser.activities.ViewModel
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

        binding.shapeableImageView.strokeColor =
            ColorStateList.valueOf(context?.getColor(R.color.white) ?: 0)
        binding.seekBarQualityContinuous.progress = 100

        binding.seekBarQualityDiscrete.progress = 3
        binding.seekBarQualityDiscrete.visibility = View.GONE

        val compressionMethods: Array<String> = resources.getStringArray(R.array.compressionMethods)
        val adapterCompressionMethods: ArrayAdapter<String> =
            ArrayAdapter<String>(requireContext(), android.R.layout.simple_list_item_1, compressionMethods)
        binding.spinnerCompressionMethod.adapter = adapterCompressionMethods

        val viewModel: ViewModel by activityViewModels()

        val set = ConstraintSet()
        set.clone(binding.ConstraintLayout)

        binding.spinnerCompressionMethod.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parentView: AdapterView<*>?) { }

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                viewModel.changeCompression(binding.spinnerCompressionMethod.selectedItem.toString())

                if (binding.spinnerCompressionMethod.selectedItem.toString() == "Luban") {
                    set.connect(
                        binding.editTextQuality.id, ConstraintSet.START,
                        binding.seekBarQualityDiscrete.id, ConstraintSet.END, 10.toPixels()
                    )
                    set.applyTo(binding.ConstraintLayout)
                    binding.seekBarQualityDiscrete.visibility = View.VISIBLE
                    binding.seekBarQualityContinuous.visibility = View.GONE

                    binding.editTextQuality.setText(binding.seekBarQualityDiscrete.progress.toString())
                } else {
                    set.connect(
                        binding.editTextQuality.id, ConstraintSet.START,
                        binding.seekBarQualityContinuous.id, ConstraintSet.END, 10.toPixels()
                    )
                    set.applyTo(binding.ConstraintLayout)
                    binding.seekBarQualityDiscrete.visibility = View.GONE
                    binding.seekBarQualityContinuous.visibility = View.VISIBLE

                    binding.editTextQuality.setText(binding.seekBarQualityContinuous.progress.toString())
                }
            }
        }

        val seekBarListener = object: OnSeekBarChangeListener {
            override fun onStopTrackingTouch(seekBar: SeekBar) { }
            override fun onStartTrackingTouch(seekBar: SeekBar) { }
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                binding.editTextQuality.setText(progress.toString())
                viewModel.changeQuality(progress)
            }
        }

        binding.seekBarQualityContinuous.setOnSeekBarChangeListener(seekBarListener)
        binding.seekBarQualityDiscrete.setOnSeekBarChangeListener(seekBarListener)

        binding.editTextQuality.addTextChangedListener(object: TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) { }
            override fun afterTextChanged(s: Editable?) { }
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val beforeProgress = binding.editTextQuality.text.toString().toIntOrNull()
                when (beforeProgress) {
                    null -> binding.seekBarQualityDiscrete.progress = 0
                    in 0..100 ->
                        binding.seekBarQualityDiscrete.progress =
                            binding.editTextQuality.text.toString().toInt()
                    else -> binding.seekBarQualityDiscrete.progress = 100
                }

                viewModel.changeQuality(beforeProgress ?: 0)
                //Puts cursor at *end* of edit text
                binding.editTextQuality.setSelection(binding.editTextQuality.text.length)
            }

        })
        return view
    }

    private fun Int.toPixels(): Int {
        val scale = context?.resources?.displayMetrics?.density ?: 0f
        //Converts from dp/sp to pixels
        return (this * scale + 0.5).toInt()
    }
}