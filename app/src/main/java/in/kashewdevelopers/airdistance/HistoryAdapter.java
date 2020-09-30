package in.kashewdevelopers.airdistance;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;

import in.kashewdevelopers.airdistance.databinding.HistoryListItemBinding;

public class HistoryAdapter extends CursorAdapter {

    HistoryAdapter(Context context, Cursor cursor, int flags) {
        super(context, cursor, flags);
    }


    @Override
    public View newView(Context context, Cursor cursor, ViewGroup viewGroup) {
        HistoryListItemBinding binding = HistoryListItemBinding.inflate(LayoutInflater.from(context), viewGroup, false);
        View view = binding.getRoot();
        view.setTag(binding);
        return view;
    }


    @Override
    public void bindView(View view, final Context context, Cursor cursor) {
        HistoryListItemBinding binding = (HistoryListItemBinding) view.getTag();

        // get data from cursor
        String sourceName = cursor.getString(cursor.getColumnIndex(HistoryDbHelper.SRC_NAME));
        String destinationName = cursor.getString(cursor.getColumnIndex(HistoryDbHelper.DST_NAME));
        String sourceLatLng = cursor.getString(cursor.getColumnIndex(HistoryDbHelper.SRC_LL));
        String destinationLatLng = cursor.getString(cursor.getColumnIndex(HistoryDbHelper.DST_LL));
        String distance = cursor.getString(cursor.getColumnIndex(HistoryDbHelper.DISTANCE));
        final String hashCode = cursor.getString(cursor.getColumnIndex(HistoryDbHelper.HASH));

        binding.sourceName.setText(sourceName);
        binding.destinationName.setText(destinationName);
        binding.sourceLatLng.setText(sourceLatLng);
        binding.destinationLatLng.setText(destinationLatLng);
        binding.distance.setText(distance);

        // show history on map
        binding.parent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (onHistoryClickListener != null) {
                    onHistoryClickListener.onHistoryClickListener((HistoryListItemBinding) view.getTag());
                }
            }
        });

        // delete entry in history
        binding.deleteIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (onDeleteClickListener != null) {
                    onDeleteClickListener.onDeleteClickListener(hashCode);
                }
            }
        });
    }


    // set onClick interfaces
    void setOnHistoryClickListener(OnHistoryClickListener onHistoryClickListener) {
        this.onHistoryClickListener = onHistoryClickListener;
    }

    void setOnDeleteClickListener(OnDeleteClickListener onDeleteClickListener) {
        this.onDeleteClickListener = onDeleteClickListener;
    }


    // onClick interface variables
    private OnHistoryClickListener onHistoryClickListener;
    private OnDeleteClickListener onDeleteClickListener;


    // interfaces used to implement onClick behaviours
    interface OnHistoryClickListener {
        void onHistoryClickListener(HistoryListItemBinding listItemBinding);
    }

    interface OnDeleteClickListener {
        void onDeleteClickListener(String hashCode);
    }

}
