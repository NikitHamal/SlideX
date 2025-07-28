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
        // Pre-create fragments to ensure they're available
        slidesFragment = new SlidesFragment();
        chatFragment = new ChatFragment();
        codeFragment = new CodeFragment();
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                return slidesFragment;
            case 1:
                return chatFragment;
            case 2:
                return codeFragment;
            default:
                return slidesFragment;
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
