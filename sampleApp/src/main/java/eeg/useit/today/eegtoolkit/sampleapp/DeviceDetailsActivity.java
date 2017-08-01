package eeg.useit.today.eegtoolkit.sampleapp;

import android.databinding.DataBindingUtil;
import android.databinding.Observable;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.choosemuse.libmuse.Eeg;
import com.choosemuse.libmuse.Muse;
import com.choosemuse.libmuse.MuseListener;
import com.choosemuse.libmuse.MuseManagerAndroid;

import eeg.useit.today.eegtoolkit.common.FrequencyBands.Band;
import eeg.useit.today.eegtoolkit.common.FrequencyBands.ValueType;
import eeg.useit.today.eegtoolkit.sampleapp.databinding.ActivityDeviceDetailsBinding;
import eeg.useit.today.eegtoolkit.view.graph.GraphGLView;
import eeg.useit.today.eegtoolkit.view.graph.GraphSurfaceView;
import eeg.useit.today.eegtoolkit.vm.StreamingDeviceViewModel;
import eeg.useit.today.eegtoolkit.vm.TimeSeries;

/**
 * Example activity that displays live details for a connected device.
 * Includes isGood connection status, and scrolling line graphs using surface and GL views.
 */
public class DeviceDetailsActivity extends AppCompatActivity {
  public static int DURATION_SEC = 5;

  /** The live device VM backing this view. */
  private StreamingDeviceViewModel deviceVM = new StreamingDeviceViewModel();

  /** Holder for the scrolling graph based off a SurfaceView. */
  private GraphSurfaceView surfaceView;

  /** Holder for the scrolling graph based off a GLView. */
  private GraphGLView glView;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    // Initialize Muse first up.
    MuseManagerAndroid.getInstance().setContext(this);

    // Bind viewmodel to the view.
    ActivityDeviceDetailsBinding binding =
        DataBindingUtil.setContentView(this, R.layout.activity_device_details);
    binding.setDeviceVM(deviceVM);
    binding.setConnectionVM(deviceVM.createSensorConnection());
    binding.setThetaVM(deviceVM.createFrequencyLiveValue(Band.THETA, ValueType.SCORE));
    binding.setDeltaVM(deviceVM.createFrequencyLiveValue(Band.DELTA, ValueType.SCORE));
    binding.setAlphaVM(deviceVM.createFrequencyLiveValue(Band.ALPHA, ValueType.SCORE));
    binding.setBetaVM( deviceVM.createFrequencyLiveValue(Band.BETA,  ValueType.SCORE));

    // Attach the live data to the graph views.
    TimeSeries rawSeries3 = deviceVM.createRawTimeSeries(Eeg.EEG3, DURATION_SEC);
    this.surfaceView = (GraphSurfaceView) findViewById(R.id.graphSurface);
    this.surfaceView.setTimeSeries(rawSeries3);
    this.glView = (GraphGLView) findViewById(R.id.graphGL);
    this.glView.setTimeSeries(rawSeries3);

    // Bind action bar, seems like this can't be done in the layout :(
    deviceVM.addOnPropertyChangedCallback(new Observable.OnPropertyChangedCallback() {
      @Override
      public void onPropertyChanged(Observable sender, int propertyId) {
        DeviceDetailsActivity.this.getSupportActionBar().setTitle(
            String.format("%s: %s", deviceVM.getName(), deviceVM.getConnectionState())
        );
      }
    });

    // And attach the desired muse to the VM once connected.
    final String macAddress = getIntent().getExtras().getString("mac");
    if (macAddress != null) {
      MuseManagerAndroid.getInstance().startListening();
      MuseManagerAndroid.getInstance().setMuseListener(new MuseListener() {
        @Override
        public void museListChanged() {
          for (Muse muse : MuseManagerAndroid.getInstance().getMuses()) {
            if (macAddress.equals(muse.getMacAddress())) {
              DeviceDetailsActivity.this.deviceVM.setMuse(muse);
              MuseManagerAndroid.getInstance().stopListening();
              break;
            }
          }
        }
      });
    }

    // Finally, make sure to refresh the graphs once the timeseries changes.
    rawSeries3.addOnPropertyChangedCallback(new Observable.OnPropertyChangedCallback() {
      @Override public void onPropertyChanged(Observable sender, int propertyId) {
        updateGraphs();
      }
    });
  }

  // Updates the graphs by invalidating them, causing redraw with the new data.
  private void updateGraphs() {
    surfaceView.postInvalidate();
    glView.postInvalidate();
  }
}
