package com.example.andrs.presentation.meds;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.andrs.R;
import com.example.andrs.databinding.ItemMedicationBinding;
import com.example.andrs.domain.model.Medication;
import com.example.andrs.presentation.common.Formats;
import com.example.andrs.presentation.common.Labels;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

class MedicationAdapter extends RecyclerView.Adapter<MedicationAdapter.Holder> {
    private final List<Medication> items = new ArrayList<>();
    private final Consumer<Medication> onEdit;
    private final Consumer<Medication> onTake;

    MedicationAdapter(Consumer<Medication> onEdit, Consumer<Medication> onTake) {
        this.onEdit = onEdit;
        this.onTake = onTake;
    }

    @SuppressWarnings("NotifyDataSetChanged")
    void submit(List<Medication> list) {
        items.clear();
        items.addAll(list);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(ItemMedicationBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder h, int position) {
        Medication m = items.get(position);
        Context ctx = h.itemView.getContext();
        h.b.name.setText(m.getName());
        StringBuilder details = new StringBuilder(ctx.getString(Labels.category(m.getCategory())));
        if (m.getDosage() != null && !m.getDosage().isEmpty()) details.append(" · ").append(m.getDosage());
        if (!m.isActive()) details.append(" · ").append(ctx.getString(R.string.meds_inactive));
        h.b.details.setText(details);
        h.b.badgeAlters.setVisibility(m.altersCycle() ? View.VISIBLE : View.GONE);
        boolean reminder = m.isActive() && m.isReminderEnabled();
        h.b.reminder.setVisibility(reminder ? View.VISIBLE : View.GONE);
        h.b.reminder.setText(ctx.getString(R.string.meds_reminder_at,
                Formats.time(m.getReminderHour(), m.getReminderMinute())));
        h.itemView.setAlpha(m.isActive() ? 1f : 0.6f);
        h.itemView.setOnClickListener(v -> onEdit.accept(m));
        h.b.btnTake.setOnClickListener(v -> onTake.accept(m));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        final ItemMedicationBinding b;

        Holder(ItemMedicationBinding b) {
            super(b.getRoot());
            this.b = b;
        }
    }
}
