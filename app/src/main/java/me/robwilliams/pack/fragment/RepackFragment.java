package me.robwilliams.pack.fragment;

public class RepackFragment extends AbstractPackingFragment {
    @Override
    protected int getCurrentPageStatus() {
        return STATUS_REPACKED;
    }
}
