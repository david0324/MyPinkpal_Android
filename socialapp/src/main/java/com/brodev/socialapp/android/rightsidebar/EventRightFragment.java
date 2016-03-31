package com.mypinkpal.app.Rightsidebar;

import java.util.ArrayList;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONObject;

import com.brodev.socialapp.config.Config;
import com.brodev.socialapp.entity.Menu;
import com.brodev.socialapp.entity.User;

import com.brodev.socialapp.fragment.EventFragment;
import com.brodev.socialapp.http.NetworkUntil;
import com.brodev.socialapp.view.DashboardActivity;

import com.brodev.socialapp.android.PhraseManager;
import com.mypinkpal.app.R;

import android.content.Context;

import android.os.AsyncTask;
import android.os.Bundle;

import android.support.v4.app.Fragment;
import android.support.v4.app.ListFragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class EventRightFragment extends ListFragment {

	// init Network Until
	private NetworkUntil networkUntil = new NetworkUntil();
	private SidebarAdapter sa = null;
	private String categoryId, categoryName;
	private User user;

	// phrase manager
	private PhraseManager phraseManager;
    private RelativeLayout noInternetLayout;

	public static EventRightFragment newInstance() {
		return new EventRightFragment();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		if (getArguments() != null) {
			categoryId = getArguments().getString("categoryId");
			categoryName = getArguments().getString("categoryName");
		}
        View view = inflater.inflate(R.layout.side_bar, container, false);
        noInternetLayout = (RelativeLayout) view.findViewById(R.id.no_internet_layout);
        noInternetLayout.setVisibility(View.GONE);
        return view;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		categoryId = null;
		super.onCreate(savedInstanceState);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		user = (User) getView().getContext().getApplicationContext();
		phraseManager = new PhraseManager(getActivity().getApplicationContext());

		try {
			new SideBarTask().execute();
		} catch (Exception ex) {
			ex.printStackTrace();
		}

	}

	@Override
	public void onListItemClick(ListView listview, View view, int position, long id) {
		Fragment newContent = new EventFragment();

		Fragment newContentRight = new EventRightFragment();

		Menu menu = (Menu) getListAdapter().getItem(position);

		int mode = Config.LEFT_RIGHT_SLIDING;
		Bundle bundle = new Bundle();
		bundle.putString("categoryId", menu.getLink());
		bundle.putString("name", menu.getPhrase());
		newContent.setArguments(bundle);
		
		Bundle category = new Bundle();
		category.putString("categoryId", menu.getLink());
		category.putString("categoryName", menu.getPhrase());
		newContentRight.setArguments(category);
		switchFragment(newContent, newContentRight, mode);

	}

	private class SideBarTask extends AsyncTask<String, Void, String> {
		String resultstring = null;
		JSONObject mainJson = null;

		@Override
		protected String doInBackground(String... params) {
			try {
				// create side bar adapter
				sa = new SidebarAdapter(getActivity());

				// Use BasicNameValuePair to create GET data
				List<NameValuePair> pairs = new ArrayList<NameValuePair>();
				pairs.add(new BasicNameValuePair("token", user.getTokenkey()));
				pairs.add(new BasicNameValuePair("method", "accountapi.getEventCategories"));
				pairs.add(new BasicNameValuePair("user_id", user.getUserId()));
				if (categoryId != null) {
					pairs.add(new BasicNameValuePair("category_id", String.valueOf(categoryId)));	
				}
				
				// url request
				String URL = Config.makeUrl(user.getCoreUrl(), null, false);
				// request GET method to server
				resultstring = networkUntil.makeHttpRequest(URL, "GET", pairs);

			} catch (Exception ex) {
			}
			return resultstring;
		}

		@Override
		protected void onPreExecute() {
			// TODO Auto-generated method stub
			super.onPreExecute();
		}

		protected void onPostExecute(String result) {
			try {
				mainJson = new JSONObject(result);
				JSONArray outputJson = mainJson.getJSONArray("output");
				JSONObject genreJson = null;

				// get blog category
				if (categoryId == null) {
					sa.addHeader(phraseManager.getPhrase(getActivity().getApplicationContext(), "event.categories"));
				} else {
					sa.addItem(categoryName, "category", categoryId, "", "", 0, true);
				}
				
				for (int i = 0; i < outputJson.length(); i++) {
					genreJson = outputJson.getJSONObject(i);
					Menu menu = new Menu();
					menu.setPhrase(genreJson.getString("name"));						
					menu.setUrl("url");
					menu.setLink(genreJson.getString("category_id"));
					sa.addItem(menu);
				}

				if (sa != null) {
					setListAdapter(sa);
				}
			} catch (Exception ex) {
				ex.printStackTrace();
			}

			super.onPostExecute(result);
		}
	}

	/**
	 * Change fragment on dash board activity
	 * 
	 * @param fragment
	 */
	public void switchFragment(Fragment fragment, Fragment fragRight, int mode) {
		if (getActivity() == null)
			return;
		if (getActivity() instanceof DashboardActivity) {
			DashboardActivity fca = (DashboardActivity) getActivity();
			// set mode for sliding menu
			fca.setModeSliding(mode);
			fca.switchContent(fragment);
			if (fragRight != null) {
				fca.switchContentForRight(fragRight);
			}
		}
	}

	public class SidebarAdapter extends ArrayAdapter<Menu> {

		public SidebarAdapter(Context context) {
			super(context, 0);
		}

		public void addHeader(String title) {
			add(new Menu(title, true));
		}

		public void addItem(String title, String url, String link, String icon,
				String isActive, int counter, boolean isHeader) {
			add(new Menu(title, url, link, icon, isActive, counter, isHeader));
		}

		public void addItem(Menu menu) {
			add(menu);
		}

		@Override
		public int getViewTypeCount() {
			return 3;
		}

		@Override
		public int getItemViewType(int position) {
			if (getItem(position).isUser())
				return 2;
			return getItem(position).isHeader() ? 0 : 1;
		}

		@Override
		public boolean isEnabled(int position) {
			return !getItem(position).isHeader();
		}

		public View getView(int position, View convertView, ViewGroup parent) {

			Menu item = getItem(position);
			ViewHolder holder = null;
			View view = convertView;

			if (view == null) {
				int layout = R.layout.sidebar_row;
				// if is header
				if (item.isHeader()) {
					layout = R.layout.sidebar_header;
				} else if (item.isUser()) {
					layout = R.layout.sidebar_user_row;
				}

				view = LayoutInflater.from(getContext()).inflate(layout, null);

				TextView header = (TextView) view.findViewById(R.id.menurow_title);
				ImageView icon = (ImageView) view.findViewById(R.id.menurow_icon);
				TextView title = (TextView) view.findViewById(R.id.menurow_counter);
				
				view.setTag(new ViewHolder(header, icon, title));
			}

			if (holder == null && view != null) {
				Object tag = view.getTag();
				if (tag instanceof ViewHolder) {
					holder = (ViewHolder) tag;
				}
			}
			if (item != null && holder != null) {
				if (holder.textHolder != null) {
					if ((item.getPhrase() != null)) {
						holder.textHolder.setText(item.getPhrase());
					} else {
						holder.textHolder.setText("" + item.getIdTitle());
					}
				}

				if (holder.textCounterHolder != null) {
					if (item.getCounter() > 0) {
						holder.textCounterHolder.setVisibility(View.VISIBLE);
						holder.textCounterHolder.setText("" + item.getCounter());
					} else {
						holder.textCounterHolder.setVisibility(View.GONE);
					}
				}

				if (holder.imageHolder != null) {
					holder.imageHolder.setVisibility(View.GONE);
				}
			}
			
			return view;

		}
	}
	
	public static class ViewHolder {
		public final TextView textHolder;
		public final ImageView imageHolder;
		public final TextView textCounterHolder;

		public ViewHolder(TextView header, ImageView icon, TextView title) {
			this.textHolder = header;
			this.imageHolder = icon;
			this.textCounterHolder = title;
		}
	}

}
