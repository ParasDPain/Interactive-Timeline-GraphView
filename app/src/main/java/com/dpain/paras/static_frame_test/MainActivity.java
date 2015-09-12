package com.dpain.paras.static_frame_test;

/*
TODO Handle orientation change
TODO Add a static label displaying the threshold value
 */
import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.helper.DateAsXAxisLabelFormatter;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.DataPointInterface;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.jjoe64.graphview.series.OnDataPointTapListener;
import com.jjoe64.graphview.series.PointsGraphSeries;
import com.jjoe64.graphview.series.Series;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class MainActivity extends Activity implements NewValueFragment.NoticeDialogListener{

    // Constants
    private static final int CUSTOM_BLUE = Color.argb(200, 2, 80, 180);
    private static final int CUSTOM_LIGHTBLUE = Color.argb(100, 0, 200, 225);
    private static final int CUSTOM_RED = Color.argb(255, 230, 80, 50);
    private static final int CUSTOM_GRAY = Color.argb(100, 0, 0, 0);

    // GraphView Components
    private static GraphView graph;
    private static LineGraphSeries<DataPoint> mainStream;
    private static PointsGraphSeries<DataPoint> pointStream;
    private static PointsGraphSeries<DataPoint> overThreshStream;
    private static LineGraphSeries<DataPoint> thresholdStream;

    // Thresh Control
    private static final int thresh = 100;

    // DataPoints
    static ArrayList<DataPoint> mainDataLib = new ArrayList<>();
    static ArrayList<Date> dateIndex = new ArrayList<>();
    static Date fromDateX, toDateX;
    static DateFormat formatter = DateFormat.getDateInstance();
    static Calendar calendar = Calendar.getInstance();

    // Button
    private Button fromPicker;
    private Button toPicker;
    private static boolean isFromButton = true;

    @Override
    public final void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        fromPicker = (Button) findViewById(R.id.btn_from_date);
        toPicker = (Button) findViewById(R.id.btn_to_date);

        calendar.set(2015, Calendar.JANUARY, 1);
        Date startDate = calendar.getTime();
        fromDateX = startDate;

        int size = 30;
        Random rand = new Random();

        // Patch to avoid illegal data appends after orientation reset
        mainDataLib.clear();
        dateIndex.clear();

        for(int i = 0;i < size; i++){
            mainDataLib.add(new DataPoint(startDate,rand.nextInt(120)));
            dateIndex.add(startDate);
            calendar.add(Calendar.DATE, 1);
            startDate = calendar.getTime();
        }

        calendar.set(2015, Calendar.JANUARY, 1);
        calendar.add(Calendar.DATE, size - 1);
        toDateX = calendar.getTime();

        GraphInit();
        GraphSeriesInit();
        UpdateGraph(fromDateX, toDateX);
    }

    // Initialize GraphView Components
    private void GraphInit() {
        LinearLayout graphContainer = (LinearLayout) findViewById(R.id.graph_container);

        graph = new GraphView(this);

        graph.getViewport().setXAxisBoundsManual(true);
        graph.getViewport().setYAxisBoundsManual(true);
        graph.getViewport().setMinX(0);
        graph.getViewport().setMinY(0);

        // graph.getViewport().setScalable(true);
        // graph.setHorizontalScrollBarEnabled(true);

        graph.getGridLabelRenderer().setGridColor(CUSTOM_RED);
        graph.getGridLabelRenderer().setHorizontalLabelsColor(CUSTOM_GRAY);
        graph.getGridLabelRenderer().setVerticalLabelsColor(CUSTOM_GRAY);
        graph.getGridLabelRenderer().setNumHorizontalLabels(3);

        graph.getGridLabelRenderer().setLabelFormatter(new DateAsXAxisLabelFormatter(this));

        graphContainer.addView(graph);
    }

    // Called when time-frame bounds are changed - X Axis
    public void UpdateGraph(Date fromDateX, Date toDateX) {
        // Reset streams
        mainStream.resetData(new DataPoint[]{new DataPoint(0,0)});
        pointStream.resetData(new DataPoint[]{new DataPoint(0, 0)});
        overThreshStream.resetData(new DataPoint[]{new DataPoint(0, 0)});
        thresholdStream.resetData(new DataPoint[]{new DataPoint(0, 0)});

        // Calculate number of days
        TimeUnit tu = TimeUnit.DAYS;
        int daysInInt = (int) tu.convert(toDateX.getTime() - fromDateX.getTime(), TimeUnit.MILLISECONDS);

        // find index to find needed values via for-loop
        int fromX = dateIndex.indexOf(fromDateX);
        int toX = dateIndex.indexOf(toDateX);

        // series should have some amount of days
        if(daysInInt > 0)
        {
            // Update streams
            for(int i = 0; i <= daysInInt; i++){
                // Try-Catch to append empty values for null values
                try {
                    mainStream.appendData(mainDataLib.get(fromX + i), true, daysInInt);
                    pointStream.appendData(mainDataLib.get(fromX + i), true, daysInInt);
                    thresholdStream.appendData(new DataPoint((fromX + i), thresh), true, daysInInt);

                    if(mainDataLib.get(fromX + i).getY() >= thresh){
                        overThreshStream.appendData(mainDataLib.get(fromX + i), true, daysInInt);
                    }
                } catch (Exception e) {
                    // Do nothing
                }
            }
        }
        else
        {
            Toast t = Toast.makeText(this, "No data found!",Toast.LENGTH_LONG);
            t.show();
        }
        // Add extra data point to cover the whole graph
        thresholdStream.appendData(new DataPoint(toDateX, thresh), true, toX);

        // Update button text values and graph size
        fromPicker.setText(formatter.format(fromDateX));
        toPicker.setText(formatter.format(toDateX));

        // +-1 to make limit values inclusive
        graph.getViewport().setMinX(fromDateX.getTime() - 1);
        graph.getViewport().setMaxX(toDateX.getTime() + 1);

        graph.getViewport().setMinY(0);
        // Highest value + 1/4th the size of the canvas
        if (thresh >= mainStream.getHighestValueY()){
            graph.getViewport().setMaxY(thresh + (thresh / 4));
        } else{
            graph.getViewport().setMaxY(mainStream.getHighestValueY() + (mainStream.getHighestValueY() / 4));
        }
    }

    // Init DataSeries
    private void GraphSeriesInit() {
        graph.removeAllSeries();

        mainStream = new LineGraphSeries<>(new DataPoint[]{new DataPoint(0, 0)});
        mainStream.setThickness(2);
        mainStream.setColor(CUSTOM_LIGHTBLUE);
        mainStream.setDrawBackground(true);
        mainStream.setBackgroundColor(CUSTOM_LIGHTBLUE);

        pointStream = new PointsGraphSeries<>(new DataPoint[]{new DataPoint(0, 0)});
        pointStream.setSize(10);
        pointStream.setColor(CUSTOM_BLUE);
        pointStream.setOnDataPointTapListener(new OnDataPointTapListener() {
            @Override
            public void onTap(Series series, DataPointInterface dataPoint) {
                String message = "Value " + dataPoint.getY() + " on " + formatter.format(dataPoint.getX());
                Toast.makeText(getBaseContext(), message, Toast.LENGTH_SHORT).show();
            }
        });

        overThreshStream = new PointsGraphSeries<>(new DataPoint[]{new DataPoint(0,0)});
        overThreshStream.setColor(CUSTOM_RED);
        overThreshStream.setSize(10);

        thresholdStream = new LineGraphSeries<>(new DataPoint[]{new DataPoint(0,0)});
        Paint paint = new Paint();
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(CUSTOM_RED);
        paint.setStrokeWidth(5);
        paint.setPathEffect(new DashPathEffect(new float[]{8, 5}, 0));
        thresholdStream.setCustomPaint(paint);

        graph.addSeries(thresholdStream);
        graph.addSeries(mainStream);
        graph.addSeries(pointStream);
        graph.addSeries(overThreshStream);
    }

    public void AddValDialog(View view) {
        DialogFragment newFragment = new NewValueFragment();
        newFragment.show(getFragmentManager(), "newValue");
    }

    public void LoadNextDataSet(View view) {
        calendar.set(2015, Calendar.JANUARY, 1);
        Date startDate = calendar.getTime();
        fromDateX = startDate;

        int size = 30;

        // Patch to avoid illegal data appends after orientation reset
        mainDataLib.clear();
        dateIndex.clear();

        for(int i = 0;i < size; i++){
            mainDataLib.add(new DataPoint(startDate,i*10));
            dateIndex.add(startDate);
            calendar.add(Calendar.DATE, 1);
            startDate = calendar.getTime();
        }

        calendar.set(2015, Calendar.JANUARY, 1);
        calendar.add(Calendar.DATE, size - 1);
        toDateX = calendar.getTime();

        UpdateGraph(fromDateX, toDateX);
    }

    // New Value Dialog button handlers
    @Override
    public void onDialogPositiveClick(DialogFragment dialog, Date time, int value) {
        mainDataLib.add(new DataPoint(time,value));
        dateIndex.add(time);
        toDateX = time;
        UpdateGraph(fromDateX, toDateX);

        dialog.getDialog().dismiss();
    }
    @Override
    public void onDialogNegativeClick(DialogFragment dialog) {
        dialog.getDialog().cancel();
    }

    // DatePicker Control
    public void DatePickerDialog(View v) {
        isFromButton = (v.getId() == findViewById(R.id.btn_from_date).getId());
        DialogFragment newFragment = new DatePickerFragment();
        newFragment.show(getFragmentManager(), "datePicker");
    }
    public static class DatePickerFragment extends android.app.DialogFragment
            implements DatePickerDialog.OnDateSetListener {

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            // Use the current from/to as the default date in the picker
            if(isFromButton)
            {
                calendar.setTime(fromDateX);
            }
            else
            {
                calendar.setTime(toDateX);
            }

            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH);
            int day = calendar.get(Calendar.DAY_OF_MONTH);


            // Create a new instance of DatePickerDialog and return it
            return new DatePickerDialog(getActivity(), this, year, month, day);
        }

        public void onDateSet(DatePicker view, int year, int month, int day) {
            // Convert to 'Date' type and update the graph
            calendar.set(year, month, day);
            if(isFromButton)
            {
                fromDateX = calendar.getTime();
            }
            else
            {
                toDateX = calendar.getTime();
            }
            ((MainActivity)getActivity()).UpdateGraph(fromDateX, toDateX);
        }
    }
}



