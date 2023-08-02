package com.kamiruku.pngoptimiser

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.kamiruku.pngoptimiser.fragments.DisplayImagesFragment

class ViewStateAdapter(fragmentManager: FragmentManager, private val arrayFragments: Array<DisplayImagesFragment>, lifecycle: Lifecycle):
    FragmentStateAdapter(fragmentManager, lifecycle) {

    override fun createFragment(position: Int): Fragment {
        // Hardcoded in this order, you'll want to use lists and make sure the titles match
        val args = Bundle()

        return if (position == 0) {
            args.putString("KEY_ID", "before")
            arrayFragments[position].arguments = args
            arrayFragments[position]
        } else {
            args.putString("KEY_ID", "after")
            arrayFragments[position].arguments = args
            arrayFragments[position]
        }
    }

    override fun getItemCount(): Int {
        return arrayFragments.size
    }
}