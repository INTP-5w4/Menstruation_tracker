package com.example.andrs.presentation.meds;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.andrs.R;
import com.example.andrs.databinding.ItemIntakeBinding;
import com.example.andrs.domain.model.MedicationIntake;
import com.example.andrs.presentation.common.Formats;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

class IntakeAdapter extends RecyclerView.Adapter<IntakeAdapter.Holder> {
    private final List<MedicationIntake> items = new ArrayList<>();
    private final Consumer<MedicationIntake> onDelete;

    IntakeAdapter(Consumer<MedicationIntake> onDelete) {
        this.onDelete = onDelete;
    }

    @SuppressWarnings("NotifyDataSetChanged")
    void submit(List<MedicationIntake> list) {
        items.clear();
        items.addAll(list);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(ItemIntakeBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder h, int position) {
        MedicationIntake i = items.get(position);
        Context ctx = h.itemView.getContext();
        h.b.name.setText(i.getMedicationName());
        String time = Formats.dateTime(i.getTakenAt());
        if (i.altersCycle()) time += " · " + ctx.getString(R.string.intake_alters);
        h.b.time.setText(time);
        h.b.btnDelete.setOnClickListener(v -> onDelete.accept(i));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        final ItemIntakeBinding b;

        Holder(ItemIntakeBinding b) {
            super(b.getRoot());
            this.b = b;
        }
    }
}
