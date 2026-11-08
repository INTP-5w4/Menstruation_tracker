package com.example.andrs.presentation.calendar;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.andrs.R;
import com.example.andrs.databinding.ItemPeriodBinding;
import com.example.andrs.domain.model.Period;
import com.example.andrs.presentation.common.Formats;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

class PeriodHistoryAdapter extends RecyclerView.Adapter<PeriodHistoryAdapter.Holder> {
    private final List<PeriodRow> items = new ArrayList<>();
    private final Consumer<Period> onClick;

    PeriodHistoryAdapter(Consumer<Period> onClick) {
        this.onClick = onClick;
    }

    @SuppressWarnings("NotifyDataSetChanged")
    void submit(List<PeriodRow> rows) {
        items.clear();
        items.addAll(rows);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(ItemPeriodBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder h, int position) {
        PeriodRow r = items.get(position);
        Context ctx = h.itemView.getContext();
        Period p = r.period;
        String end = p.getEnd() == null ? ctx.getString(R.string.cal_history_ongoing) : Formats.mediumDate(p.getEnd());
        h.b.range.setText(Formats.mediumDate(p.getStart()) + " – " + end);
        StringBuilder details = new StringBuilder();
        if (r.durationDays > 0) {
            details.append(ctx.getString(R.string.cal_history_duration, Formats.days(ctx, r.durationDays)));
        }
        if (r.cycleLength > 0) {
            if (details.length() > 0) details.append(" · ");
            details.append(ctx.getString(R.string.cal_history_cycle, Formats.days(ctx, r.cycleLength)));
        }
        h.b.details.setText(details);
        h.itemView.setOnClickListener(v -> onClick.accept(p));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        final ItemPeriodBinding b;

        Holder(ItemPeriodBinding b) {
            super(b.getRoot());
            this.b = b;
        }
    }
}
