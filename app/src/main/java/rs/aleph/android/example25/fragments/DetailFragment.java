package rs.aleph.android.example25.fragments;

import android.app.Fragment;
import android.app.NotificationManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.List;

import rs.aleph.android.example25.R;
import rs.aleph.android.example25.activities.MainActivity;
import rs.aleph.android.example25.db.model.Product;
import rs.aleph.android.example25.model.Category;
//import rs.aleph.android.example25.provider.CategoryProvider;
//import rs.aleph.android.example25.provider.ProductProvider;

public class DetailFragment extends Fragment implements AdapterView.OnItemSelectedListener {

    private static int NOTIFICATION_ID = 1;

    private Product product = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

//        try {
//            if (product == null) { product = ((MainActivity)getActivity()).getDatabaseHelper().getProductDao().queryForId(0); }
//
//        } catch (SQLException e) {
//            e.printStackTrace();
//        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (savedInstanceState != null) {
            product = new Product();
            product.setmId(savedInstanceState.getInt("id"));
            product.setmName(savedInstanceState.getString("name"));
            product.setDescription(savedInstanceState.getString("description"));
            product.setRating(savedInstanceState.getFloat("rating"));
            product.setImage(savedInstanceState.getString("image"));
        }
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);

        if (savedInstanceState != null) {
            savedInstanceState.putInt("id", product.getmId());
            savedInstanceState.putString("name", product.getmName());
            savedInstanceState.putString("description", product.getDescription());
            savedInstanceState.putFloat("rating", product.getRating());
            savedInstanceState.putString("image", product.getImage());
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.v("DetailFragment", "onCreateView()");

        setHasOptionsMenu(true);

        View view = inflater.inflate(R.layout.detail_fragment, container, false);

        TextView name = (TextView) view.findViewById(R.id.name);
        name.setText(product.getmName());

        TextView description = (TextView) view.findViewById(R.id.description);
        description.setText(product.getDescription());

        RatingBar ratingBar = (RatingBar) view.findViewById(R.id.rating);
        ratingBar.setRating(product.getRating());

        ImageView imageView = (ImageView) view.findViewById(R.id.image);
        InputStream is = null;
        try {
            is = getActivity().getAssets().open(product.getImage());
            Drawable drawable = Drawable.createFromStream(is, null);
            imageView.setImageDrawable(drawable);
        } catch (IOException e) {
            e.printStackTrace();
        }
//Ovde krecemo sa kreiranjem Spinnera za kategorije, pa tek onda idemo u Main-u kako bismo pozvali ovu metodu
        Spinner spinner = (Spinner) view.findViewById(R.id.category);


        try {
            List<rs.aleph.android.example25.db.model.Category> list = ((MainActivity) getActivity()).getDatabaseHelper().getCategoryDao().queryForAll();
            ArrayAdapter<rs.aleph.android.example25.db.model.Category> dataAdapter = new ArrayAdapter<>(
                    getActivity(),
                    android.R.layout.simple_spinner_item,
                    list);
            spinner.setAdapter(dataAdapter);


            for (int i=0;i<list.size();i++){
                if (list.get(i).getId() == product.getCategory().getId()){
                    spinner.setSelection(i);
                    break;
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return view;
    }
    //Uvek dodamo setProduct, kako bi nam vratio i istovremeno sacuvali izabrani produkt
    public void setProduct(Product product) {
        this.product = product;
    }


    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
        // You can retrieve the selected item using
        //product.setCategory(CategoryProvider.getCategoryById((int)id));
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        //product.setCategory(null);
    }

    /**
     * Kada dodajemo novi element u toolbar potrebno je da obrisemo prethodne elmente
     * zato pozivamo menu.clear() i dodajemo nove toolbar elemente
     * */
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.clear();
        inflater.inflate(R.menu.detail_fragment_menu, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

//    public void updateProduct(Product product) {
//        this.product = product;
//
//        EditText name = (EditText) getActivity().findViewById(R.id.name);
//        name.setText(product.getmName());
//
//        EditText description = (EditText) getActivity().findViewById(R.id.description);
//        description.setText(product.getDescription());
//
//        RatingBar ratingBar = (RatingBar) getActivity().findViewById(R.id.rating);
//        ratingBar.setRating(product.getRating());
//
//        ImageView imageView = (ImageView) getActivity().findViewById(R.id.image);
//        InputStream is = null;
//        try {
//            is = getActivity().getAssets().open(product.getImage());
//            Drawable drawable = Drawable.createFromStream(is, null);
//            imageView.setImageDrawable(drawable);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }

    //Ova metoda sluzi za izmenu i cuvanje podataka koje smo vec uneli klikom na Add dugme.
    //Svaki put kada izvrsimo i sacuvamo akciju Add, mozemo otvoriti nas sacuvan fajl i ga nadknadno izmeniti doUpdate

    private void doUpdateElement(){
        if (product != null){
            EditText name = (EditText) getActivity().findViewById(R.id.name);
            product.setmName(name.getText().toString());

            EditText description = (EditText) getActivity().findViewById(R.id.description);
            product.setDescription(description.getText().toString());

            RatingBar ratingBar = (RatingBar) getActivity().findViewById(R.id.rating);
            product.setRating(ratingBar.getRating());

            Spinner category = (Spinner) getActivity().findViewById(R.id.category);
            rs.aleph.android.example25.db.model.Category c = (rs.aleph.android.example25.db.model.Category)category.getSelectedItem();
            product.setCategory(c);

            try {
                ((MainActivity) getActivity()).getDatabaseHelper().getProductDao().update(product);
            } catch (SQLException e) {
                e.printStackTrace();
            }
            getActivity().onBackPressed();
        }
    }

    private void doRemoveElement(){
        if (product != null) {
            try {
                ((MainActivity) getActivity()).getDatabaseHelper().getProductDao().delete(product);
            } catch (SQLException e) {
                e.printStackTrace();
            }
            getActivity().onBackPressed();
        }
    }

    /**
     * Na fragment dodajemo element za brisanje elementa i za izmenu podataka
     * */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.remove:
                doRemoveElement();
                break;
            case R.id.update:
                doUpdateElement();
                break;
        }

        return super.onOptionsItemSelected(item);
    }
}
