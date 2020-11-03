package rs.aleph.android.example25.activities;

import android.app.Dialog;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.Toast;

import com.j256.ormlite.android.apptools.OpenHelperManager;
import com.j256.ormlite.dao.Dao;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import rs.aleph.android.example25.R;
import rs.aleph.android.example25.adapters.DrawerListAdapter;
import rs.aleph.android.example25.db.DatabaseHelper;
import rs.aleph.android.example25.db.model.Category;
import rs.aleph.android.example25.db.model.Product;
import rs.aleph.android.example25.dialogs.AboutDialog;
import rs.aleph.android.example25.fragments.DetailFragment;
import rs.aleph.android.example25.fragments.ListFragment;
import rs.aleph.android.example25.fragments.ListFragment.OnProductSelectedListener;
import rs.aleph.android.example25.model.NavigationItem;

public class MainActivity extends AppCompatActivity implements OnProductSelectedListener {

    /* The click listner for ListView in the navigation drawer */
    private class DrawerItemClickListener implements ListView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            selectItemFromDrawer(position);
        }
    }

    private DrawerLayout drawerLayout;
    private ListView drawerList;
    private ActionBarDrawerToggle drawerToggle;
    private RelativeLayout drawerPane;
    private CharSequence drawerTitle;
    private CharSequence title;

    private ArrayList<NavigationItem> navigationItems = new ArrayList<NavigationItem>();

    private AlertDialog dialog;

    private boolean landscapeMode = false;
    private boolean listShown = false;
    private boolean detailShown = false;

    private int productId = 0;

    private DatabaseHelper databaseHelper;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main);

        // Draws navigation items
        navigationItems.add(new NavigationItem(getString(R.string.drawer_home), getString(R.string.drawer_home_long), R.drawable.ic_action_product));
        navigationItems.add(new NavigationItem(getString(R.string.drawer_settings), getString(R.string.drawer_Settings_long), R.drawable.ic_action_settings));
        navigationItems.add(new NavigationItem(getString(R.string.drawer_about), getString(R.string.drawer_about_long), R.drawable.ic_action_about));

        title = drawerTitle = getTitle();
        drawerLayout = (DrawerLayout) findViewById(R.id.drawerLayout);
        drawerList = (ListView) findViewById(R.id.navList);

        // Populate the Navigtion Drawer with options
        drawerPane = (RelativeLayout) findViewById(R.id.drawerPane);
        DrawerListAdapter adapter = new DrawerListAdapter(this, navigationItems);

        // set a custom shadow that overlays the main content when the drawer opens
        drawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);
        drawerList.setOnItemClickListener(new DrawerItemClickListener());
        drawerList.setAdapter(adapter);

        // Enable ActionBar app icon to behave as action to toggle nav drawer
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        final android.support.v7.app.ActionBar actionBar = getSupportActionBar();

        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeAsUpIndicator(R.drawable.ic_drawer);
            actionBar.setHomeButtonEnabled(true);
            actionBar.show();
        }

        drawerToggle = new ActionBarDrawerToggle(
                this,                  /* host Activity */
                drawerLayout,         /* DrawerLayout object */
                toolbar,  /* nav drawer image to replace 'Up' caret */
                R.string.drawer_open,  /* "open drawer" description for accessibility */
                R.string.drawer_close  /* "close drawer" description for accessibility */
        ) {
            public void onDrawerClosed(View view) {
                getSupportActionBar().setTitle(title);
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }

            public void onDrawerOpened(View drawerView) {
                getSupportActionBar().setTitle(drawerTitle);
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }
        };

        if (savedInstanceState == null) {
            FragmentTransaction ft = getFragmentManager().beginTransaction();
            ListFragment listFragment = new ListFragment();
            ft.add(R.id.displayList, listFragment, "List_Fragment");
            ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
            ft.commit();
            selectItemFromDrawer(0);
        }

        if (findViewById(R.id.displayDetail) != null) {
            landscapeMode = true;
            getFragmentManager().popBackStack();

            DetailFragment detailFragment = (DetailFragment) getFragmentManager().findFragmentById(R.id.displayDetail);
            if (detailFragment == null) {
                FragmentTransaction ft = getFragmentManager().beginTransaction();
                detailFragment = new DetailFragment();
                ft.replace(R.id.displayDetail, detailFragment, "Detail_Fragment1");
                ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
                ft.commit();
                detailShown = true;
            }
        }

        listShown = true;
        detailShown = false;
        productId = 0;

        addInitCateogry();
    }

    //Primenom metode addInitCategory voditi racuna da prilikom promene imena naziva kategorije, morate promeniti ime baza
    //   private static final String DATABASE_NAME = "mojadrugabaza.db" (na primer) i debugovati app
    //kako bi vam iznova iscitao promene naziva kategorije. Bitno je imati na umu da je ovo sve spojeno sa klasom
    //DatabaseHelper i bez promene imena baze nase dole navedene promene ne bi bile vidljive
    private void addInitCateogry(){
        try {
            if (getDatabaseHelper().getCategoryDao().queryForAll().size() == 0){
                Category Sglumci = new Category();
                Sglumci.setName("Strani glumci");

                Category Dglumci = new Category();
                Dglumci.setName("Domaci glumci");

                Category Nkategorija = new Category();
                Nkategorija.setName("Neznana kategorija");

                getDatabaseHelper().getCategoryDao().create(Sglumci);
                getDatabaseHelper().getCategoryDao().create(Dglumci);
                getDatabaseHelper().getCategoryDao().create(Nkategorija);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    //da bi dodali podatak u bazu, potrebno je da napravimo objekat klase addItem i njene metode
    //koji reprezentuje tabelu i popunimo podacima
    private void addItem() throws SQLException {
        final Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_layout);

        final Spinner imagesSpinner = (Spinner) dialog.findViewById(R.id.product_image);
        List<String> imagesList = new ArrayList<String>();
        imagesList.add("borat.jpg");
        imagesList.add("borat1.jpg");
        imagesList.add("borat2.jpg");
        ArrayAdapter<String> imagesAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, imagesList);
        imagesSpinner.setAdapter(imagesAdapter);
        imagesSpinner.setSelection(0);

// Final metoda categorySpinner je za razliku od prethodne povezanna sa klasom DatabaseHelper i preko nje pozivamo da nam sacuva
        //izmene na dialog spinner-u. U slucaju da ne pozovemo getDatabaseHelper().getCategoryDao().queryForAll() nasa aplikacija
        // bi pukla, odnosno ne bi sacuvala nikakva dodavanja.
        final Spinner productsSpinner = (Spinner) dialog.findViewById(R.id.product_category);
        List<Category> list = getDatabaseHelper().getCategoryDao().queryForAll();
        ArrayAdapter<Category> dataAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, list);
        productsSpinner.setAdapter(dataAdapter);
        productsSpinner.setSelection(0);

        final EditText productName = (EditText) dialog.findViewById(R.id.product_name);
        final EditText productDescr = (EditText) dialog.findViewById(R.id.product_description);
        final EditText productRating = (EditText) dialog.findViewById(R.id.product_rating);

        Button ok = (Button) dialog.findViewById(R.id.ok);
        ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {

                    String name = productName.getText().toString();
                    String desct = productDescr.getText().toString();


                    //Voditi racuna kada radimo sa brojevima. Ono sto unesemo mora biti broj
                    //da bi formater uspeo ispravno da formatira broj. Dakle ono sto unesemo
                    //bice u teksutalnom obliku, i mora biti moguce pretrovirit u broj.
                    //Ako nije moguce pretvoriti u broj dobicemo NumberFormatException
                    //Zato je dobro za input gde ocekujemo da stavimo broj, stavimo u xml-u
                    //da ce tu biti samo unet broj npr android:inputType="number|numberDecimal"
                    float price = Float.parseFloat(productRating.getText().toString());

                    Category category = (Category) productsSpinner.getSelectedItem();
                    String image = (String) imagesSpinner.getSelectedItem();


                    Product product = new Product();
                    product.setmName(name);
                    product.setDescription(desct);
                    product.setRating(price);
                    product.setImage(image);
                    product.setCategory(category);

                    getDatabaseHelper().getProductDao().create(product);
                    refresh();
                    Toast.makeText(MainActivity.this, "Product inserted", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();

                }catch (NumberFormatException e){
                    Toast.makeText(MainActivity.this, "Rating mora biti broj", Toast.LENGTH_SHORT).show();
                }catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        });

        Button cancel = (Button) dialog.findViewById(R.id.cancel);
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        dialog.show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_item_detail, menu);
        return super.onCreateOptionsMenu(menu);
    }

    // onCreateOptionsMenu i onOptionsItemSelected su vezane za nas Action Bar - u ovom slucaju dodavanje i refresh

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_refresh:
                refresh();
                break;
            case R.id.action_add:
                try {
                    addItem();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    // refresh() prikazuje novi sadrzaj.Povucemo nov sadrzaj iz baze i popunimo listu
    private void refresh() {
        ListView listview = (ListView) findViewById(R.id.products);

        if (listview != null){
            ArrayAdapter<Product> adapter = (ArrayAdapter<Product>) listview.getAdapter();

            if(adapter!= null)
            {
                try {
                    adapter.clear();
                    List<Product> list = getDatabaseHelper().getProductDao().queryForAll();

                    adapter.addAll(list);

                    adapter.notifyDataSetChanged();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void setTitle(CharSequence title) {
        getSupportActionBar().setTitle(title);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        drawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Pass any configuration change to the drawer toggls
        drawerToggle.onConfigurationChanged(newConfig);
    }

    private void selectItemFromDrawer(int position) {
        if (position == 0){

        } else if (position == 1){
            Intent settings = new Intent(MainActivity.this,SettingsActivity.class);
            startActivity(settings);
        } else if (position == 2){
            if (dialog == null){
                dialog = new AboutDialog(MainActivity.this).prepareDialog();
            } else {
                if (dialog.isShowing()) {
                    dialog.dismiss();
                }
            }

            dialog.show();
        }

       drawerList.setItemChecked(position, true);
       setTitle(navigationItems.get(position).getTitle());
       drawerLayout.closeDrawer(drawerPane);
    }

    // Metode onProductSelected i onBackPressed uvek idu zajedno. Prva oznacava neku promenu i cuvanje, a druga da kada izvrsimo
    //prethodnu akciju mozemo da se vratimo na "pocetni" prozor aplikacije.
    @Override
    public void onProductSelected(int id) {

        productId = id;

        try {
            Product product = getDatabaseHelper().getProductDao().queryForId(id);
                    DetailFragment detailFragment = new DetailFragment();
                    detailFragment.setProduct(product);
                    FragmentTransaction ft = getFragmentManager().beginTransaction();
                    ft.replace(R.id.displayList, detailFragment, "Detail_Fragment2");
                    ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
                    ft.addToBackStack("Detail_Fragment2");
                    ft.commit();
            //Ako bi stavili za listShown true, svaki put kada bismo stisnuli na konzolu "sacuvaj", nas app bi sacuvao tu
            //informaciju, ali bi odmah doslo do crasha. Zato stavljamo na false, a detailShown na true!
                    listShown = false;
                    detailShown = true;
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onBackPressed() {

        if (listShown == true) {
            finish();
        } else if (detailShown == true) {
            getFragmentManager().popBackStack();
            ListFragment listFragment = new ListFragment();
            FragmentTransaction ft = getFragmentManager().beginTransaction();
            ft.replace(R.id.displayList, listFragment, "List_Fragment");
            ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
            ft.commit();
            listShown = true;
            detailShown = true;
        }

    }

    //Metoda koja komunicira sa bazom podataka
    public DatabaseHelper getDatabaseHelper() {
        if (databaseHelper == null) {
            databaseHelper = OpenHelperManager.getHelper(this, DatabaseHelper.class);
        }
        return databaseHelper;
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();

        // nakon rada sa bazo podataka potrebno je obavezno
        //osloboditi resurse!
        if (databaseHelper != null) {
            OpenHelperManager.releaseHelper();
            databaseHelper = null;
        }
    }
}


