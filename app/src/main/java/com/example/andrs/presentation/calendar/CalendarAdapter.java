package com.example.andrs.presentation.calendar;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.andrs.R;
import com.example.andrs.databinding.ItemCalendarDayBinding;
import com.google.android.material.color.MaterialColors;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

class CalendarAdapter extends RecyclerView.Adapter<CalendarAdapter.Holder> {
    private final List<CalendarDay> items = new ArrayList<>();
    private final Consumer<LocalDate> onClick;

    CalendarAdapter(Consumer<LocalDate> onClick) {
        this.onClick = onClick;
    }

    @SuppressWarnings("NotifyDataSetChanged")
    void submit(List<CalendarDay> days) {
        items.clear();
        items.addAll(days);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(ItemCalendarDayBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder h, int position) {
        CalendarDay d = items.get(position);
        Context ctx = h.itemView.getContext();
        if (d.date == null) {
            h.b.day.setText("");
            h.b.day.setBackground(null);
            h.b.dot.setVisibility(View.INVISIBLE);
            h.itemView.setOnClickListener(null);
            h.itemView.setClickable(false);
            return;
        }
        h.b.day.setText(String.valueOf(d.date.getDayOfMonth()));
        h.b.day.setTypeface(null, d.today ? Typeface.BOLD : Typeface.NORMAL);

        int onSurface = MaterialColors.getColor(h.itemView, com.google.android.material.R.attr.colorOnSurface);
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.OVAL);
        int textColor = onSurface;
        boolean hasBg = true;
        switch (d.type) {
            case PERIOD:
                bg.setColor(ContextCompat.getColor(ctx, R.color.phase_menstrual));
                textColor = ContextCompat.getColor(ctx, R.color.white);
                break;
            case PREDICTED_PERIOD:
                bg.setColor(ContextCompat.getColor(ctx, R.color.day_predicted_period));
                bg.setStroke(dp(ctx, 1.5f), ContextCompat.getColor(ctx, R.color.phase_menstrual), dp(ctx, 3), dp(ctx, 2));
                textColor = ContextCompat.getColor(ctx, R.color.day_on_predicted);
                break;
            case OVULATION:
                bg.setColor(ContextCompat.getColor(ctx, R.color.phase_ovulation));
                textColor = ContextCompat.getColor(ctx, R.color.white);
                break;
            case FERTILE:
                bg.setColor(ContextCompat.getColor(ctx, R.color.day_fertile));
                textColor = ContextCompat.getColor(ctx, R.color.day_on_fertile);
                break;
            default:
                hasBg = false;
        }
        if (d.today) {
            if (!hasBg) bg.setColor(0);
            bg.setStroke(dp(ctx, 2), MaterialColors.getColor(h.itemView, androidx.appcompat.R.attr.colorPrimary));
            hasBg = true;
        }
        h.b.day.setBackground(hasBg ? bg : null);
        h.b.day.setTextColor(textColor);

        if (d.hasLog || d.hasIntimacy) {
            GradientDrawable dot = new GradientDrawable();
            dot.setShape(GradientDrawable.OVAL);
            dot.setColor(d.hasIntimacy
                    ? MaterialColors.getColor(h.itemView, androidx.appcompat.R.attr.colorPrimary)
                    : MaterialColors.getColor(h.itemView, com.google.android.material.R.attr.colorOnSurfaceVariant));
            h.b.dot.setBackground(dot);
            h.b.dot.setVisibility(View.VISIBLE);
        } else {
            h.b.dot.setVisibility(View.INVISIBLE);
        }
        h.itemView.setOnClickListener(v -> onClick.accept(d.date));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private static int dp(Context c, float v) {
        return Math.round(v * c.getResources().getDisplayMetrics().density);
    }

    static class Holder extends RecyclerView.ViewHolder {
        final ItemCalendarDayBinding b;

        Holder(ItemCalendarDayBinding b) {
            super(b.getRoot());
            this.b = b;
        }
    }
}
