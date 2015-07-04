package me.robwilliams.pack.fragment;

public class PackFragment extends AbstractPackingFragment {
    @Override
    protected int getCurrentPageStatus() {
        return STATUS_PACKED;
    }
}
