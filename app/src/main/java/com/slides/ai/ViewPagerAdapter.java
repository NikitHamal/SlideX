package com.slides.ai;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class ViewPagerAdapter extends FragmentStateAdapter {

    private SlidesFragment slidesFragment;
    private ChatFragment chatFragment;
    private CodeFragment codeFragment;

    public ViewPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                if (slidesFragment == null) {
                    slidesFragment = new SlidesFragment();
                }
                return slidesFragment;
            case 1:
                if (chatFragment == null) {
                    chatFragment = new ChatFragment();
                }
                return chatFragment;
            case 2:
                if (codeFragment == null) {
                    codeFragment = new CodeFragment();
                }
                return codeFragment;
            default:
                return new SlidesFragment();
        }
    }

    @Override
    public int getItemCount() {
        return 3;
    }

    public SlidesFragment getSlidesFragment() {
        return slidesFragment;
    }

    public CodeFragment getCodeFragment() {
        return codeFragment;
    }

    public ChatFragment getChatFragment() {
        return chatFragment;
    }
}
