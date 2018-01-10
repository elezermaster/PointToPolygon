package io.emaster.pointtopolygon;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Layout;
import android.util.Log;
import android.util.SparseArray;
import android.view.Gravity;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import java.lang.Math.*;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Dash;
import com.google.android.gms.maps.model.Dot;
import com.google.android.gms.maps.model.Gap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PatternItem;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.maps.android.PolyUtil;
import com.google.maps.android.SphericalUtil;
import com.google.maps.android.data.kml.KmlContainer;
import com.google.maps.android.data.kml.KmlLayer;
import com.google.maps.android.data.kml.KmlPlacemark;
import com.google.maps.android.data.kml.KmlPolygon;
import com.google.maps.android.ui.IconGenerator;
import com.nbsp.materialfilepicker.MaterialFilePicker;
import com.nbsp.materialfilepicker.ui.FilePickerActivity;
import org.xmlpull.v1.XmlPullParserException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;


import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import io.emaster.pointtopolygon.*;

import static java.lang.Math.PI;
import static java.lang.Math.log;
import static java.lang.Math.sin;
import static java.lang.Math.tan;
import static java.lang.Math.toRadians;

/*
//TODO: להלן התרגיל לדוגמא:
מצורף קובץ מסוג KML (בשם Allowed area) ובו פוליגון באיזור תל אביב.
  יש לכתוב פתרון, שיציג על מפה את הפוליגון ויאפשר ללחוץ על המפה.
לאחר כל לחיצה, יש להציג את המיקום שנלחץ ולציין האם הוא בתוך הפוליגון או מחוץ אליו.
אם הלחיצה מחוץ לפוליגון, יש להציג את המרחק הקצר ביותר בין מקום הלחיצה לבין הפוליגון.

הערה:
שים לב, מדובר על מרחק לפוליגון (הצורה שנוצרת) ולא לנקודות שלו.
כך שאם יש לך פוליגון שאחת הצלעות שלו היא קו ישר בין חיפה לאילת והמיקום שלך הוא תל אביב (דוגמא בקובץ "Bad sample"). המרחק שלך לפוליגון יהיה כ-18 ק"מ (באיזור פתח תקווה) ולא כ-90 ק"מ לנקודה באיזור חיפה.

נא לציין את כמות הזמן שלקח לך לחקור את הנושא ואת כמות הזמן לכתיבת הפתרון.

 */

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback,
        GoogleMap.OnMapLongClickListener,
        GoogleMap.OnMarkerClickListener
        //,GoogleMap.OnMapClickListener
        //,GoogleMap.OnPolygonClickListener

       {

    private static final int COLOR_BLACK_ARGB = 0xff000000;
    private static final int COLOR_WHITE_ARGB = 0xffffffff;
    private static final int COLOR_GREEN_ARGB = 0xff388E3C;
    private static final int COLOR_PURPLE_ARGB = 0xff81C784;
    private static final int COLOR_ORANGE_ARGB = 0xffF57F17;
    private static final int COLOR_BLUE_ARGB = 0xffF9A825;
    private static final int COLOR_GREY_ARGB = 0xffBCBCBC;

    private static final int POLYGON_STROKE_WIDTH_PX = 8;
    private static final int PATTERN_DASH_LENGTH_PX = 20;
    private static final int PATTERN_GAP_LENGTH_PX = 20;
    private static final PatternItem DOT = new Dot();
    private static final PatternItem DASH = new Dash(PATTERN_DASH_LENGTH_PX);
    private static final PatternItem GAP = new Gap(PATTERN_GAP_LENGTH_PX);

    // Create a stroke pattern of a gap followed by a dash.
    private static final List<PatternItem> PATTERN_POLYGON_ALPHA = Arrays.asList(GAP, DASH);

    // Create a stroke pattern of a dot followed by a gap, a dash, and another gap.
    private static final List<PatternItem> PATTERN_POLYGON_BETA =
            Arrays.asList(DOT, GAP, DASH, GAP);

    private GoogleMap mMap;
    private ArrayList<LatLng> arrayPoints = new ArrayList<>();
    LatLng myPoint = null;
    ArrayList<LatLng> myPoints = null;
    PolylineOptions polylineOptions;
    private boolean checkClick = false;
    private boolean kmlIsOpened = false;
    FloatingActionButton fab;
    SupportMapFragment mapFragment;
    Polygon polygon;
    Polyline polyline;

    ClipboardManager clipboard;// = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
    public Integer bestPointDistance = 999999999;
    public LatLng bestPointLatLng = null;
    Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        context = getApplicationContext();
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.hide();
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (checkClick == false) {
                    Snackbar.make(view, "Tap on map to set point to calculate distance from polygon", Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();
                    fab.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(getApplicationContext(),R.color.colorGrey)));
                }else{

                }
                mapFragment.getMapAsync(new OnMapReadyCallback() {
                    @Override
                    public void onMapReady(GoogleMap googleMap) {
                        setPolygonPoints();

                        checkClick = true;
                    }
                });
            }
        });

        clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);

        mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        arrayPoints = new ArrayList<LatLng>();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // load kml file from local storage
        int id = item.getItemId();
        String path = "/sdcard/doc.kml";

        if (id == R.id.action_open_file) {

            new MaterialFilePicker()
                    .withActivity(this)
                    .withRequestCode(1)
                    .withFilter(Pattern.compile(".*\\.kml$")) // Filtering files and directories by file name using regexp
                    .withFilterDirectories(false) // Set directories filterable (false by default)
                    .withHiddenFiles(true) // Show hidden files and folders
                    .start();

            return true;
        }
        if(id == R.id.action_setting){
            //loadKMLfromClipboard();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
               super.onActivityResult(requestCode, resultCode, data);

               if (requestCode == 1 && resultCode == RESULT_OK) {
                   String filePath = data.getStringExtra(FilePickerActivity.RESULT_FILE_PATH);
                   FileInputStream kmlInputStream = null;
                    try {
                        kmlInputStream = new FileInputStream(filePath);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }

                    KmlLayer layer = null;
                    try {
                        layer = new KmlLayer(mMap, kmlInputStream, getApplicationContext());

                    } catch (XmlPullParserException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    try {
                        mMap.clear();
                        layer.addLayerToMap();
                        moveCameraToKml(layer);
                        checkClick = true;
                        Toast.makeText( getApplicationContext(),"now click on map to get distance to polygon", Toast.LENGTH_LONG).show();

                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (XmlPullParserException e) {
                        e.printStackTrace();
                    }
               }
    }

    private void moveCameraToKml(KmlLayer kmlLayer) {
               arrayPoints.clear();
               //Retrieve the first container in the KML layer
               KmlContainer container = kmlLayer.getContainers().iterator().next();
               //Retrieve a nested container within the first container
               //container = container.getContainers().iterator().next();
               //Retrieve the first placemark in the nested container
               KmlPlacemark placemark = container.getPlacemarks().iterator().next();
               //Retrieve a polygon object in a placemark
               KmlPolygon polygon = (KmlPolygon) placemark.getGeometry();
               //Create LatLngBounds of the outer coordinates of the polygon
               LatLngBounds.Builder builder = new LatLngBounds.Builder();
               for (LatLng latLng : polygon.getOuterBoundaryCoordinates()) {
                   builder.include(latLng);
                   arrayPoints.add(latLng);
                   Log.d("KML", latLng.latitude + " : "+ latLng.longitude);
               }

               setPolygonPoints();

               mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 1));
               kmlIsOpened = true;
               fab.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(getApplicationContext(),R.color.colorPrimary)));

    }

    //create polygon from array of points
    public void setPolygon(){
        PolygonOptions polygonOptions = new PolygonOptions();
        polygonOptions.addAll(arrayPoints);
        polygonOptions.geodesic(true);
        polygonOptions.clickable(false);
        polygon = mMap.addPolygon(polygonOptions);
        stylePolygon(polygon);
    }

    public void addMarker(LatLng myPoint ){
        mMap.addMarker(
                new MarkerOptions()
                        .position(myPoint)
                        .icon( BitmapDescriptorFactory.fromResource(R.drawable.ic_maps_point)));
    }


    //attach text to marker location
    private void addIcon(IconGenerator iconFactory, String text, LatLng position) {
        if(!text.equals("")) {
            TextView mmTextMapView = new TextView(this);
            //mmTextMapView.setTypeface(font);
            ViewGroup.LayoutParams layoutParams =
                    new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT);
            mmTextMapView.setLayoutParams(layoutParams);
            mmTextMapView.setGravity(Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL);
            mmTextMapView.setTextColor(getResources().getColor(R.color.colorAccent));
            mmTextMapView.setText(text);
            iconFactory.setContentView(mmTextMapView);

            MarkerOptions markerOptions = new MarkerOptions().
                    icon(BitmapDescriptorFactory.fromBitmap(
                            iconFactory.makeIcon(mmTextMapView.getText().toString()))).
                    position(position).
                    anchor(iconFactory.getAnchorU(), iconFactory.getAnchorV());
            mMap.addMarker(markerOptions);
        }
    }

    //clear map onMapLongClick
    @Override
    public void onMapLongClick(LatLng latLng) {
        mMap.clear();
        arrayPoints.clear();
        checkClick = false;
        kmlIsOpened = false;
        fab.hide();
        //fab.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(getApplicationContext(),R.color.colorAccent)));
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        Log.d("MAP", "Marker lat long=" + marker.getPosition());
        Log.d("MAP","First postion check" + arrayPoints.get(0));
        Log.d("MAP","**********All arrayPoints***********" + arrayPoints);
        if (arrayPoints.get(0).equals(marker.getPosition())){
            Log.d("MAP","********First Point choose************");

        }
        return false;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        //its important to disable listener on polygon
        //mMap.setOnPolygonClickListener(this);
        //mMap.setOnMapClickListener(this);
        mMap.setOnMapLongClickListener(this);
        mMap.setOnMarkerClickListener(this);


        // Add a marker in TelAviv and move the camera
        LatLng tlv = new LatLng(32.086924196045004,34.79001883417368); //Tel Aviv
        mMap.addMarker(new MarkerOptions().position(tlv).title("Tel Aviv"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(tlv));



        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                    myPoint = latLng;

                //if no polygon still created
                //put point by point to create polygon
                if (checkClick == false && kmlIsOpened == false){
                    mMap.clear();
                    addMarker(myPoint);
                    arrayPoints.add(latLng);
                    setPolygon();
                    if(arrayPoints.size()>=3){
                        fab.show();
                    }
                }
                //if kml file opened or
                //if polygon created by myself
                if(arrayPoints.size()>2 && checkClick == true){
                    mMap.clear();
                    setPolygon();
                    mMap.addMarker(
                            new MarkerOptions()
                                    .position(myPoint)
                                    .icon( BitmapDescriptorFactory.fromResource(R.drawable.ic_star_black_24px)));
                    //set closest point on polygon
                    getClosestPointToPoly( arrayPoints,  myPoint);
                    polyline = mMap.addPolyline(new PolylineOptions()
                            .add(myPoint, bestPointLatLng)
                            .width(5)
                            .color(Color.MAGENTA));
                    //add text icons
                    IconGenerator iconGenerator = new IconGenerator(getApplicationContext());
                    iconGenerator.setTextAppearance(R.style.iconGenText);
                    addIcon(iconGenerator, "" + bestPointDistance, bestPointLatLng);
                    addIcon(iconGenerator, getAddress(myPoint), myPoint);
                    //check if inside polygon
                    boolean contains = PolyUtil.containsLocation(myPoint, arrayPoints, false);
                    if(contains){
                        Toast.makeText(context, "INSIDE POLYGON" , Toast.LENGTH_LONG).show();
                    }
                }

            }

        });

    }


    public void setPolygonPoints(){
        if(arrayPoints.size() >= 3){
            checkClick = true;
            PolygonOptions polygonOptions = new PolygonOptions();
            polygonOptions.geodesic(true);
            polygonOptions.addAll(arrayPoints);
//            .strokeColor(Color.BLUE)
//            .strokeWidth(7)
//            .fillColor(Color.CYAN);
            polygon = mMap.addPolygon(polygonOptions);
            stylePolygon(polygon);
        }
        checkClick = false;
    }

    //set style to polygon
    private void stylePolygon(Polygon polygon) {
        String type = "";
        // Get the data object stored with the polygon.
        if (polygon.getTag() != null) {
            type = polygon.getTag().toString();
        }

        List<PatternItem> pattern = null;
        int strokeColor = COLOR_BLACK_ARGB;
        //int fillColor = COLOR_WHITE_ARGB;
        int fillColor = COLOR_BLUE_ARGB;

        switch (type) {
            // If no type is given, allow the API to use the default.
            case "alpha":
                // Apply a stroke pattern to render a dashed line, and define colors.
                pattern = PATTERN_POLYGON_ALPHA;
                strokeColor = COLOR_GREEN_ARGB;
                fillColor = COLOR_PURPLE_ARGB;
                break;
            case "beta":
                // Apply a stroke pattern to render a line of dots and dashes, and define colors.
                pattern = PATTERN_POLYGON_BETA;
                strokeColor = COLOR_ORANGE_ARGB;
                fillColor = COLOR_BLUE_ARGB;
                break;
        }

        polygon.setStrokePattern(pattern);
        polygon.setStrokeWidth(POLYGON_STROKE_WIDTH_PX);
        polygon.setStrokeColor(strokeColor);
        polygon.setFillColor(fillColor);
        //polygon.setClickable(true);
        polygon.setZIndex(-1);
        polygon.setVisible(true);
    }


    //calculate distance between two points in meter integer
    public Integer getDistanceInMeters(LatLng ll1, LatLng ll2){
        Location loc1 = new Location("");
        loc1.setLatitude(ll1.latitude);
        loc1.setLongitude(ll1.longitude);

        Location loc2 = new Location("");
        loc2.setLatitude(ll2.latitude);
        loc2.setLongitude(ll2.longitude);

        float distanceInMeters = loc1.distanceTo(loc2);
        return  (int)Math.round(distanceInMeters);
    }

    public LatLng getMid(LatLng start, LatLng dest){
        return SphericalUtil.interpolate(start, dest, 0.5);
    }

    //get name of location
    public String getAddress(LatLng point) {
        double lat = point.latitude;
        double lng = point.longitude;
        Geocoder geocoder = new Geocoder(getApplicationContext(), Locale.getDefault());
               try {
                   List<Address> addresses = geocoder.getFromLocation(lat, lng, 1);
                   Address obj = null;
                   String add = "";
                   if( addresses.size() ==0){
                        return add;
                   }else if(addresses.get(0) != null ){
                        obj = addresses.get(0);
                        add = obj.getAddressLine(0);
                   }

                   Log.v("IGA", "obj:" + obj);

                   //add = add + "\n" + obj.getCountryName();
                   //add = add + "\n" + obj.getCountryCode();
                   //add = add + "\n" + obj.getAdminArea();
                   //add = add + "\n" + obj.getPostalCode();
                   //add = add + "\n" + obj.getSubAdminArea();
                   //add = add + "\n" + obj.getLocality();
                   //add = add + "\n" + obj.getSubThoroughfare();


                   Log.v("IGA", "Address" + add);
                    Toast.makeText(this, "Address=>" + add,
                    Toast.LENGTH_SHORT).show();

                   return add;
               } catch (IOException e) {
                   // TODO Auto-generated catch block
                   e.printStackTrace();
                   Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
                   return "";
               }
    }

    //calculate point on polygon the closest to my touch
    public LatLng getClosestPointToPoly(ArrayList<LatLng> arrayPoints, LatLng myPoint) {
        bestPointDistance = 999999999;
        bestPointLatLng = new LatLng(0.0,0.0);

        //running on all polyline of polygon
        //to get closest point to each polyline
        for (int i = 0; i < arrayPoints.size() - 1; i++) {
             calculateBestDistance(arrayPoints.get(i) ,arrayPoints.get(i + 1), myPoint );
        }
        //dont forget the last line
         calculateBestDistance(arrayPoints.get(0) ,arrayPoints.get(arrayPoints.size()-1), myPoint );

        return bestPointLatLng;
    }

    //calculate closest point on polyline to my touched point
    //we split line each time to half
    //until line shrinks to very small around 10 meters
    //to find minimal distance to line
    public void calculateBestDistance(LatLng startPoint ,LatLng endPoint, LatLng myPoint ){

        LatLng  closestPoint;
        Integer startDist;
        Integer endDist;
        Integer closestDist = getDistanceInMeters(startPoint, endPoint);

        while (closestDist >15) {
            startDist = getDistanceInMeters(startPoint, myPoint);
            endDist = getDistanceInMeters(endPoint, myPoint);
            if (startDist > endDist) {
                startPoint = getMid(startPoint, endPoint);
                closestPoint = startPoint;
                savePoint(closestPoint);
            } else {
                endPoint = getMid(startPoint, endPoint);
                closestPoint = endPoint;
                savePoint(closestPoint);
            }
            closestDist = getDistanceInMeters(startPoint, endPoint);
        }


    }

    //save point as closest point
    public void savePoint(LatLng point){
        Integer closestDist =  (getDistanceInMeters(myPoint, point));

        if(bestPointLatLng !=null) {

            if (closestDist < bestPointDistance){
                bestPointDistance = closestDist;
                bestPointLatLng = point;
            }
        }else {
             bestPointDistance = closestDist;
             bestPointLatLng = point;
        }

    }



//     private void loadKMLfromClipboard() {
//         // to load a KML dataset from a local stream, it's like this:
//         Uri uriLinktoKML;
//         ClipData.Item itemClip = clipboard.getPrimaryClip().getItemAt(0);
//         pasteData =(String) itemClip.getText();
//         if (pasteData != null) {
//             try {
//                 downloadFileAsync(pasteData);
//             } catch (Exception e) {
//                 e.printStackTrace();
//             }
//         } else {
//             Uri pasteUri = itemClip.getUri();
//             // If the URI contains something, try to get text from it
//             if (pasteUri != null) {
//                 try {
//                     downloadFileAsync(pasteUri.toString());
//                 } catch (Exception e) {
//                     e.printStackTrace();
//                 }
//
//             } else {
//
//             }
//         }
//
//         FileInputStream kmlInputStream = null;
//         String fileName = "/sdcard/doc.kml";
//         String pathName = Environment.getExternalStorageDirectory()+"/"+fileName;
//         File file = new File(pathName);
//         try {
//             kmlInputStream = new FileInputStream(file);
//
//         } catch (FileNotFoundException e) {
//             e.printStackTrace();
//         }
//
//
//         KmlLayer layer = null;
//         try {
//             layer = new KmlLayer(mMap, kmlInputStream, getApplicationContext());
//         } catch (XmlPullParserException e) {
//             e.printStackTrace();
//         } catch (IOException e) {
//             e.printStackTrace();
//         }
//         try {
//             layer.addLayerToMap();
//         } catch (IOException e) {
//             e.printStackTrace();
//         } catch (XmlPullParserException e) {
//             e.printStackTrace();
//         }
//    }
//
//    public void downloadFileAsync(final String downloadUrl) throws Exception {
//               OkHttpClient client = new OkHttpClient();
//               Request request = new Request.Builder().url(downloadUrl).build();
//               client.newCall(request).enqueue(new Callback() {
//                   public void onFailure(Call call, IOException e) {
//                       e.printStackTrace();
//                   }
//
//                   public void onResponse(Call call, Response response) throws IOException {
//                       if (!response.isSuccessful()) {
//                           throw new IOException("Failed to download file: " + response);
//                       }
//                       FileOutputStream fos = new FileOutputStream("/sdcard/doc.kml");
//                       fos.write(response.body().bytes());
//                       fos.close();
//                   }
//               });
//    }


//           public FileOutputStream getStreamFileAsync(final String downloadUrl) throws Exception {
//               OkHttpClient client = new OkHttpClient();
//               Request request = new Request.Builder().url(downloadUrl).build();
//               final FileOutputStream[] fos = new FileOutputStream[1];
//               client.newCall(request).enqueue(new Callback() {
//                   public void onFailure(Call call, IOException e) {
//                       e.printStackTrace();
//                   }
//
//                   public void onResponse(Call call, Response response) throws IOException {
//                       if (!response.isSuccessful()) {
//                           throw new IOException("Failed to download file: " + response);
//                       }
//                       fos[0] = new FileOutputStream("/sdcard/doc.kml");
//                       fos[0].write(response.body().bytes());
//                       fos[0].close();
//
//                   }
//               });
//               return fos[0];
//           }




}
