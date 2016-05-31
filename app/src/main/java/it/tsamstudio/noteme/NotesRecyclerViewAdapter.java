package it.tsamstudio.noteme;

import android.content.Intent;
import android.graphics.Color;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import it.tsamstudio.noteme.utils.NoteMeApp;


public class NotesRecyclerViewAdapter extends RecyclerView.Adapter<NotesRecyclerViewAdapter.DataObjectHolder> {

    private static final String TAG = "NotesRecyclerViewAdaptr";

    private ArrayList<Nota> mDataset;
    private static MyClickListener myClickListener;

    public static class DataObjectHolder extends RecyclerView.ViewHolder
            implements View.OnClickListener,
            View.OnCreateContextMenuListener,
            MenuItem.OnMenuItemClickListener,
            View.OnLongClickListener {

        private static final int ID_MENUITEM_SHARE = 13341234;

        Intent shareIntent;

        TextView title;
        TextView content;
        TextView tag;
        TextView creationDate;
        TextView expirationDate;
        ImageView micImg;
        ImageView imgBackground;
        CardView card;

        public DataObjectHolder(View itemView) {
            super(itemView);
            card = (CardView) itemView.findViewById(R.id.card_view);
            title = (TextView) itemView.findViewById(R.id.noteTitle);
            content = (TextView) itemView.findViewById(R.id.noteContent);
            tag = (TextView) itemView.findViewById(R.id.tag);
            creationDate = (TextView) itemView.findViewById(R.id.creationDate);
            expirationDate = (TextView) itemView.findViewById(R.id.txtExpirationDate);
            micImg = (ImageView) itemView.findViewById(R.id.micImgView);
            imgBackground = (ImageView) itemView.findViewById(R.id.imgBackground);
            itemView.setOnClickListener(this);
            itemView.setOnCreateContextMenuListener(this);
            itemView.setOnLongClickListener(this);
        }

        @Override
        public void onClick(View v) {
            myClickListener.onItemClick(getAdapterPosition(), v);
        }

        @Override
        public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
            MenuItem myActionItem = menu.add(0, ID_MENUITEM_SHARE, 0, R.string.share);
            myActionItem.setIcon(R.drawable.ic_share);
            myActionItem.setOnMenuItemClickListener(this);
        }

        @Override
        public boolean onMenuItemClick(MenuItem item) {
            switch (item.getItemId()) {
                case ID_MENUITEM_SHARE:
                    if (shareIntent != null) {
                        NoteMeApp.getInstance().getApplicationContext().startActivity(shareIntent);
                    }
                    break;
                default:
                    break;
            }
            shareIntent = null;
            return false;
        }

        @Override
        public boolean onLongClick(View v) {
            Log.d(TAG, "onLongClick: " + title.getText());
            shareIntent = new Intent();
            shareIntent.setAction(Intent.ACTION_SEND);
            String text = String.format("%s\r\n%s", title.getText(), content.getText());
            shareIntent.putExtra(Intent.EXTRA_TEXT, text);
            shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            shareIntent.setType("text/plain");
            return false;
        }
    }


    public void setOnItemClickListener(MyClickListener myClickListener) {
        this.myClickListener = myClickListener;
    }

    public NotesRecyclerViewAdapter(ArrayList<Nota> myDataset) {
        mDataset = myDataset;
    }

    @Override
    public DataObjectHolder onCreateViewHolder(ViewGroup parent,
                                               int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.note_cardview, parent, false);

        DataObjectHolder dataObjectHolder = new DataObjectHolder(view);
        return dataObjectHolder;
    }

    @Override
    public void onBindViewHolder(final DataObjectHolder holder, final int position) {
        holder.title.setText(mDataset.get(position).getTitle().trim());
        String content = mDataset.get(position).getText().trim();
        if (content.trim().length() > 50)
            content = content.substring(0, 50) + "...";
        holder.content.setText(content);
        Date d = mDataset.get(position).getLastModifiedDate();
        SimpleDateFormat sd = new SimpleDateFormat("dd/MM/yyyy HH:mm", NoteMeApp.getInstance().getLocale());

        String date = sd.format(d);
        holder.creationDate.setText(date);
        holder.imgBackground.setImageDrawable(null);
        holder.imgBackground.setAlpha(0.25f);
        holder.card.post(new Runnable() {
            @Override
            public void run() {
                holder.card.setCardBackgroundColor(Color.WHITE);
            }
        });

        if (mDataset.get(position).getColor() != 0) {
            holder.card.post(new Runnable() {
                @Override
                public void run() {
                    holder.card.setCardBackgroundColor(mDataset.get(position).getColor());
                }
            });
        } else {
            holder.card.post(new Runnable() {
                @Override
                public void run() {
                    holder.card.setCardBackgroundColor(Color.WHITE);
                }
            });
        }

        if (mDataset.get(position).getExpireDate() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM", NoteMeApp.getInstance().getLocale());
            holder.expirationDate.setVisibility(View.VISIBLE);
            holder.expirationDate.setText(NoteMeApp.getInstance().getString(R.string.scade)
                    + " " + sdf.format(mDataset.get(position).getExpireDate()));
        } else {
            holder.expirationDate.setVisibility(View.GONE);
        }

        if (mDataset.get(position).getTag() != null &&
                mDataset.get(position).getTag().trim().length() > 0) {
            holder.tag.setVisibility(View.VISIBLE);
            holder.tag.setText(mDataset.get(position).getTag().toUpperCase());
        } else {
            holder.tag.setVisibility(View.GONE);
        }

        if (mDataset.get(position).getAudio() != null) {
            holder.micImg.setVisibility(View.VISIBLE);
            holder.micImg.setImageResource(R.drawable.ic_mic_card);
        } else {
            holder.micImg.setVisibility(View.GONE);
        }

        if (mDataset.get(position).getImage() != null) {
            Log.d("card con sfondo", mDataset.get(position).getImage());
            holder.imgBackground.setVisibility(View.VISIBLE);
//            final Drawable drawable = Drawable.createFromPath(mDataset.get(position).getImage());
            holder.imgBackground.post(new Runnable() {
                @Override
                public void run() {
                    holder.card.post(new Runnable() {
                        @Override
                        public void run() {
                            Log.d(TAG, "run: " + holder.card.getWidth() + " " + holder.card.getHeight());
                            Picasso.with(NoteMeApp.getInstance().getApplicationContext())
                                    .load("file://" + mDataset.get(position).getImage())
                                    .resize(holder.card.getWidth(), holder.card.getHeight())
                                    .centerCrop()
                                    .into(holder.imgBackground, new Callback() {
                                        @Override
                                        public void onSuccess() {
                                            Log.d(TAG, "onSuccess: picasso loaded");
                                        }

                                        @Override
                                        public void onError() {
                                            Log.e(TAG, "onError: picasso failed");
                                        }
                                    });
                        }
                    });

//                    holder.imgBackground.setImageDrawable(drawable);
                }
            });
        } else {
            holder.imgBackground.setVisibility(View.GONE);
        }
    }

    public void addItem(Nota dataObj, int index) {
        mDataset.add(index, dataObj);
        notifyItemInserted(index);
    }

    public void updateItem(Nota dataObj, int index) {
        mDataset.set(index, dataObj);
        notifyItemChanged(index);
    }

    public void deleteItem(int index) {
        mDataset.remove(index);
        notifyItemRemoved(index);
    }

    @Override
    public int getItemCount() {
        return mDataset.size();
    }

    public interface MyClickListener {
        public void onItemClick(int position, View v);
    }
}
