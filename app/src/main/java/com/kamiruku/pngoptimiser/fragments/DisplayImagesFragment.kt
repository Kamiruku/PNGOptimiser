package com.kamiruku.pngoptimiser.fragments

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import com.davemorrissey.labs.subscaleview.ImageSource
import com.kamiruku.pngoptimiser.activities.ViewModel
import com.kamiruku.pngoptimiser.databinding.FragmentDisplayImagesBinding
import java.io.File

class DisplayImagesFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentDisplayImagesBinding.inflate(layoutInflater, container, false)

        var beforeUpdate = ""
        var afterUpdate = ""

        val bundle = arguments
        bundle?.let {
            val type = bundle.getString("KEY_ID")
            val viewModel: ViewModel by activityViewModels()

            if (type == "before") {
                viewModel.beforePath.observe(viewLifecycleOwner) {
                    if (viewModel.beforePath.isInitialized && viewModel.beforePath.value != beforeUpdate) {
                        val uri = Uri.parse(viewModel.beforePath.value)
                        binding.fragmentImageViewer.setImage(ImageSource.uri(uri))
                        beforeUpdate = viewModel.beforePath.value ?: ""
                    }
                }
            } else if (type == "after") {
                viewModel.afterPath.observe(viewLifecycleOwner) {
                    val file = File(viewModel.afterPath.value ?: "")
                    val uri = Uri.fromFile(file)
                    binding.fragmentImageViewer.setImage(ImageSource.uri(uri))
                    afterUpdate = viewModel.afterPath.value ?: ""
                }
            }
        }


        return binding.root
    }

    override fun onResume() {

        super.onResume()
    }
}