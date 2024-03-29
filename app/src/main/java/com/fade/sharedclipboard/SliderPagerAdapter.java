package com.fade.sharedclipboard;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

public class SliderPagerAdapter extends FragmentPagerAdapter {
  SliderPagerAdapter(@NonNull FragmentManager fm, int behavior) {
    super(fm, behavior);
  }

  @NonNull @Override
  public Fragment getItem(int position) {
    return SliderItemFragment.newInstance(position);
  }

  @Override
  public int getCount() {
    return 4;
  }
}