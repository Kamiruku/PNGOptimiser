package com.kamiruku.pngoptimiser.fragments

import android.media.Image
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import com.davemorrissey.labs.subscaleview.ImageSource
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import com.kamiruku.pngoptimiser.R
import com.kamiruku.pngoptimiser.activities.ViewModel
import com.kamiruku.pngoptimiser.databinding.FragmentDisplayImagesBinding
import java.io.File

class DisplayImagesFragment : Fragment() {
    private lateinit var binding: FragmentDisplayImagesBinding
    private var beforeUpdate: String? = null
    private var afterUpdate: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentDisplayImagesBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onResume() {
        val bundle = arguments
        bundle?.let {
            val type = bundle.getString("KEY_ID")
            val viewModel: ViewModel by activityViewModels()

            if (type == "before") {
                if (viewModel.beforePath.isInitialized || viewModel.beforePath.value != null) {
                    if (viewModel.beforePath.value != beforeUpdate) {
                        val uri = Uri.parse(viewModel.beforePath.value)
                        binding.fragmentImageViewer.setImage(ImageSource.uri(uri))
                        beforeUpdate = viewModel.beforePath.value
                    }
                }
            }
            else if (type == "after") {
                if (viewModel.afterPath.value != afterUpdate) {
                    val file = File(viewModel.afterPath.value ?: "")
                    val uri = Uri.fromFile(file)
                    binding.fragmentImageViewer.setImage(ImageSource.uri(uri))
                    afterUpdate = viewModel.afterPath.value
                }
            }
        }

        super.onResume()
    }
}