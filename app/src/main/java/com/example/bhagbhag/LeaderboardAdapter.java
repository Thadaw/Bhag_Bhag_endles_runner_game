package com.example.bhagbhag;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import java.util.List;

public class LeaderboardAdapter extends ArrayAdapter<LeaderboardEntryWithCoins> {

    public LeaderboardAdapter(Context context, int resource, List<LeaderboardEntryWithCoins> objects) {
        super(context, resource, objects);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            LayoutInflater inflater = LayoutInflater.from(getContext());
            convertView = inflater.inflate(R.layout.list_item_leaderboard, parent, false);
        }

        LeaderboardEntryWithCoins entry = getItem(position);
        if (entry != null) {
            TextView rankTextView = convertView.findViewById(R.id.rankTextView);
            TextView userNameTextView = convertView.findViewById(R.id.userNameTextView);
            TextView scoreTextView = convertView.findViewById(R.id.scoreTextView);
            TextView coinTextView = convertView.findViewById(R.id.coinTextView);


            rankTextView.setText((position + 1) + ".");

            userNameTextView.setText(entry.getUserName());

            scoreTextView.setText("Score: " + entry.getScore());

            coinTextView.setText("🪙 " + entry.getCoins());

            scoreTextView.setVisibility(View.VISIBLE);
            coinTextView.setVisibility(View.VISIBLE);
        }

        return convertView;
    }
}
