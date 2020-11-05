package rs.aleph.android.example25.activities;

import android.Manifest;
import android.app.Dialog;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
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

    private boolean listShown = false;
    private boolean detailShown = false;

    private int productId = 0;

    private DatabaseHelper databaseHelper;
    //*Prvi korak - u sklopu zadatka 26 dodajemo cetiri nove metode. Pre nego sto krenemo da radimo, moramo u Manifestu iskazati permisije
    //koje ce nam dozvoliti da koristimo kameru, odnosno galeriju slika - READ_EXTERNAL_STORAGE i WRITE_EXTERNAL_STORAGE
    private ImageView preview;
    private String imagePath = null;
    private static final String TAG = "PERMISSIONS";
    private static final int SELECT_PICTURE = 1;

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

       //*Drugi korak Termin 26 - odredimo metodu reset
    private void reset(){
        imagePath = "";
        preview = null;
    }

    //*Treci korak - sa ovom metodom, koja je ujedno sablon, dajemo mogucnost korisniku da na dugme Choose otvori kameru,
    //odnosno galeriju slika te uradi odabir image file-a. Takodje moramo odrediti da li je Build verzija veca od 23!
    public  boolean isStoragePermissionGranted() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED &&
                    checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                            == PackageManager.PERMISSION_GRANTED) {
                Log.v(TAG,"Permission is granted");
                return true;
            } else {

                Log.v(TAG,"Permission is revoked");
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
                return false;
            }
        }
        else { //permission is automatically granted on sdk<23 upon installation
            Log.v(TAG,"Permission is granted");
            return true;
        }
    }
    //U okviru treceg koraka, uvek moramo ugraditi interface odnosno poziv na to da li je permisija bila uspesna
    //This interface is the contract for receiving the results for permission requests.
    //Callback for the result from requesting permissions.
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(grantResults[0]== PackageManager.PERMISSION_GRANTED
                && grantResults[1] == PackageManager.PERMISSION_GRANTED){
            Log.v(TAG,"Permission: "+permissions[0]+ "was "+grantResults[0]);
        }
    }
 // *Cetvrti korak Termin 26 - metoda selectPicture koja sa startActivityForResult pozivom usmerava ka drugoj aktivnosti
    //u ovom slucaju pristup kameri, slikama, itd
    private void selectPicture(){
        if (isStoragePermissionGranted()) {
            Intent i = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(i, SELECT_PICTURE);
        }
    }
//*Peti korak Termin 26, koji je ujedno i deo cetvrtog, jeste interface onActivityResult koji nam vraca vrednosti prvoj aktivnosto
    //sto je u ovom slucaju kamera i samo slikanje sa kamerom
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
//RESULT_OK oznacava da je korisnik potvrdio i izabrao sliku, te vrednost te data uvek mora bit vracena
        if (requestCode == SELECT_PICTURE && resultCode == RESULT_OK && null != data) {
            //Uri - jedinstveni identifikator slike
            Uri selectedImage = data.getData();
            //filePathColumn (koji se nalazi na Cursor-u) - putanja gde se slike tocno nalaze preko Media.DATA.
            String[] filePathColumn = { MediaStore.Images.Media.DATA };
//Cursor je dvodimenzionalni element, koji nam oznacava gde se sta nalazi te vraca kolekciju trazenih podataka sa strane vaseg upita
            //Cursor je interface
            if (selectedImage != null) {
                Cursor cursor = getContentResolver().query(selectedImage, filePathColumn, null, null, null);
               //Obvezno proveriti da li je cursor vracen
                if (cursor != null) {
                    cursor.moveToFirst();

                    int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                    //Sa getString mi cuvamo putanju na disku i posle toga uvek zatvorimo cursor, a sa
                    //imagePath mi cuvamo apsolutnu (krajnju) putanju na disku
                    imagePath = cursor.getString(columnIndex);
                    cursor.close();

                    // String picturePath contains the path of selected Image
                    //Posto je slika bitmapirana stvar, uvek koristi BitmapFactory.decodeFile
                    if (preview != null) {
                        preview.setImageBitmap(BitmapFactory.decodeFile(imagePath));
                    }

                    Toast.makeText(this, imagePath, Toast.LENGTH_SHORT).show();
                }
            }
        }
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
//    private void addItem() throws SQLException {
//        final Dialog dialog = new Dialog(this);
//        dialog.setContentView(R.layout.dialog_layout);

//        final Spinner imagesSpinner = (Spinner) dialog.findViewById(R.id.product_image);
//        List<String> imagesList = new ArrayList<String>();
//        imagesList.add("borat.jpg");
//        imagesList.add("borat1.jpg");
//        imagesList.add("borat2.jpg");
//        ArrayAdapter<String> imagesAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, imagesList);
//        imagesSpinner.setAdapter(imagesAdapter);
//        imagesSpinner.setSelection(0);


    private void addItem() throws SQLException {
        final Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_layout);

        Button choosebtn = (Button) dialog.findViewById(R.id.choose);
        choosebtn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                preview = (ImageView) dialog.findViewById(R.id.preview_image);
                selectPicture();
            }
        });


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
                   // String image = (String) imagesSpinner.getSelectedItem();


                    Product product = new Product();
                    product.setmName(name);
                    product.setDescription(desct);
                    product.setRating(price);
                    product.setImage(imagePath);
                    product.setCategory(category);

                    getDatabaseHelper().getProductDao().create(product);
                    refresh();
                    Toast.makeText(MainActivity.this, "Product inserted", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                    //reset(), kog smo odredili u prvom koraku Termin 26 moramo postaviti ovde. On oznaca da ce nam
                    //sve nase podatke resetovati i vratit natrag ka pocetnoj poziciji
                    reset();

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


