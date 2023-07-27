package com.kamiruku.pngoptimiser

import android.content.Context
import android.view.Display
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import com.kamiruku.pngoptimiser.fragments.DisplayImagesFragment

class ImagePagerAdapter(context: Context, fragmentManager: FragmentManager, totalTabs: Int) : FragmentPagerAdapter(fragmentManager) {
    private val myContext: Context
    private val totalTabs: Int
    private val tabTitles: Array<String> = arrayOf("Before", "After")

    init {
        myContext = context
        this.totalTabs = totalTabs
    }

    override fun getCount(): Int {
        return tabTitles.size
    }

    override fun getItem(position: Int): Fragment {
        return when (position) {
            0 -> DisplayImagesFragment()
            1 -> DisplayImagesFragment()
            else -> DisplayImagesFragment()
        }
    }

    override fun getPageTitle(position: Int): CharSequence {
        return tabTitles[position]
    }
}