package com.kamiruku.pngoptimiser.fragments

import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.davemorrissey.labs.subscaleview.ImageSource
import com.kamiruku.pngoptimiser.R
import com.kamiruku.pngoptimiser.databinding.FragmentDisplayImagesBinding
import java.io.File

class DisplayImagesFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentDisplayImagesBinding.inflate(layoutInflater, container, false)

        val bundle = arguments
        bundle?.let {
            val imageUri = Uri.parse(bundle.getString("IMAGE_URI"))
            println(imageUri.toString())
            binding.fragmentImageViewer.setImage(ImageSource.uri(imageUri))
        }
        return binding.root
    }
}