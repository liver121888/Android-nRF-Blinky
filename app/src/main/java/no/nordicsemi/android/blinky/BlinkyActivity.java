/*
 * Copyright (c) 2018, Nordic Semiconductor
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors may be used to endorse or promote products derived from this
 * software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package no.nordicsemi.android.blinky;

import static com.github.mikephil.charting.charts.Chart.PAINT_INFO;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.androidplot.xy.CatmullRomInterpolator;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.PanZoom;
import com.androidplot.xy.SimpleXYSeries;
import com.androidplot.xy.XYGraphWidget;
import com.androidplot.xy.XYSeries;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.utils.Utils;
import com.google.android.material.appbar.MaterialToolbar;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import java.text.FieldPosition;
import java.text.Format;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import no.nordicsemi.android.ble.livedata.state.ConnectionState;
import no.nordicsemi.android.ble.observer.ConnectionObserver;
import no.nordicsemi.android.blinky.adapter.DiscoveredBluetoothDevice;
import no.nordicsemi.android.blinky.databinding.ActivityBlinkyBinding;
import no.nordicsemi.android.blinky.profile.data.Command;
import no.nordicsemi.android.blinky.viewmodels.BlinkyViewModel;

public class BlinkyActivity extends AppCompatActivity {
	public static final String EXTRA_DEVICE = "no.nordicsemi.android.blinky.EXTRA_DEVICE";

	private BlinkyViewModel viewModel;
	private ActivityBlinkyBinding binding;
	private List<Short> shortList = new ArrayList<>();


	@RequiresApi(api = Build.VERSION_CODES.O)
	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		OnClick onClick = new OnClick();
		super.onCreate(savedInstanceState);
		binding = ActivityBlinkyBinding.inflate(getLayoutInflater());
		setContentView(binding.getRoot());

		final Intent intent = getIntent();
		final DiscoveredBluetoothDevice device = intent.getParcelableExtra(EXTRA_DEVICE);
		final String deviceName = device.getName();
		final String deviceAddress = device.getAddress();

		final MaterialToolbar toolbar = binding.toolbar;
		toolbar.setTitle(deviceName != null ? deviceName : getString(R.string.unknown_device));
		toolbar.setSubtitle(deviceAddress);
		setSupportActionBar(toolbar);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		// Configure the view model.
		viewModel = new ViewModelProvider(this).get(BlinkyViewModel.class);
		viewModel.connect(device);

		// Set up views.
		//		binding.ledSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> viewModel.setLedState(isChecked));
		binding.btnKeepconnection.setOnClickListener(onClick);
		binding.btnRequestbond.setOnClickListener(onClick);
		binding.btnSynctimeage.setOnClickListener(onClick);
		binding.btnCompletebond.setOnClickListener(onClick);
		binding.btnValidatebond.setOnClickListener(onClick);
		binding.btnBreakbond.setOnClickListener(onClick);
		binding.btnFastsync.setOnClickListener(onClick);
		binding.btnEcgRealtime.setOnClickListener(onClick);
		binding.btnHrvRealtime.setOnClickListener(onClick);
		binding.btnFatigueAndCirculation.setOnClickListener(onClick);


		binding.infoNotSupported.actionRetry.setOnClickListener(v -> viewModel.reconnect());
		binding.infoTimeout.actionRetry.setOnClickListener(v -> viewModel.reconnect());

		viewModel.getConnectionState().observe(this, state -> {
			switch (state.getState()) {
				case CONNECTING:
					binding.progressContainer.setVisibility(View.VISIBLE);
					binding.infoNotSupported.container.setVisibility(View.GONE);
					binding.infoTimeout.container.setVisibility(View.GONE);
					binding.connectionState.setText(R.string.state_connecting);
					break;
				case INITIALIZING:
					binding.connectionState.setText(R.string.state_initializing);
					break;
				case READY:
					binding.progressContainer.setVisibility(View.GONE);
					binding.deviceContainer.setVisibility(View.VISIBLE);
					onConnectionStateChanged(true);
					//					viewModel.setLedState(true);
					break;
				case DISCONNECTED:
					if (state instanceof ConnectionState.Disconnected) {
						binding.deviceContainer.setVisibility(View.GONE);
						binding.progressContainer.setVisibility(View.GONE);
						final ConnectionState.Disconnected stateWithReason = (ConnectionState.Disconnected) state;
						if (stateWithReason.getReason() == ConnectionObserver.REASON_NOT_SUPPORTED) {
							binding.infoNotSupported.container.setVisibility(View.VISIBLE);
						} else {
							binding.infoTimeout.container.setVisibility(View.VISIBLE);
						}
					}
					// fallthrough
				case DISCONNECTING:
					onConnectionStateChanged(false);
					break;
			}
		});
		// label
		viewModel.getLedState().observe(this, isOn -> {
			binding.ledState.setText(isOn ? R.string.turn_on : R.string.turn_off);
			binding.ledSwitch.setChecked(isOn);
		});
		viewModel.getButtonState().observe(this,
				pressed -> {
			binding.buttonState.setText(pressed ?
							R.string.button_pressed : R.string.button_released);
			plotECG();
				});
	}

	private void onConnectionStateChanged(final boolean connected) {
		binding.ledSwitch.setEnabled(connected);
		if (!connected) {
			binding.ledSwitch.setChecked(false);
			binding.buttonState.setText(R.string.button_unknown);
		}
	}

	class OnClick implements View.OnClickListener{

		Command c;
		@RequiresApi(api = Build.VERSION_CODES.O)
		@Override
		public void onClick(View view) {

			switch (view.getId()) {
				case R.id.btn_keepconnection:
					c = Command.KEEP_CONNECTION;
					break;
				case R.id.btn_requestbond:
					c = Command.REQUEST_BOND;
					break;
				case R.id.btn_synctimeage:
					c = Command.SYNC_TIME_AGE;
					break;
				case R.id.btn_completebond:
					c = Command.COMPLETE_BOND;
					break;
				case R.id.btn_validatebond:
					c = Command.VALIDATE_BOND;
					break;
				case R.id.btn_breakbond:
					c = Command.BREAK_BOND;
					break;
				case R.id.btn_fastsync:
					c = Command.SYNC_FAST;
					break;
				case R.id.btn_ecg_realtime:
					c = Command.MEASURE_ECG_REALTIME;
					break;
				case R.id.btn_hrv_realtime:
					c = Command.MEASURE_HRV_REALTIME;
					break;
				case R.id.btn_fatigue_and_circulation:
					c = Command.MEASURE_FATIGUE_CIRCULATION;
					break;
				default:
					c = Command.KEEP_CONNECTION;
					Log.d("onClick", "onClick: default");
					break;
			}
			viewModel.sendCMD(c);
		}
	}

	@RequiresApi(api = Build.VERSION_CODES.O)
	private void plotECG(){

		//TODO: CatmullRomInterpolator
		Object obj = viewModel.readData();

		binding.chart.setBackgroundColor(getResources().getColor(R.color.ap_gray, null));
		//		binding.chart.setViewPortOffsets(110, 30, 110, 60);
		binding.chart.getDescription().setEnabled(false);
		binding.chart.setTouchEnabled(true);
		binding.chart.setScaleEnabled(true);
		binding.chart.setDrawGridBackground(true);
		binding.chart.setDrawBorders(true);
		//		binding.chart.setMaxHighlightDistance(300);
		binding.chart.animateY(500);
		binding.chart.setNoDataTextColor(getResources().getColor(R.color.ap_gray, null));
		binding.chart.setNoDataText("no data");

		Paint paint = binding.chart.getPaint(PAINT_INFO);
		paint.setTextSize(Utils.convertDpToPixel(20f));
		binding.chart.setPaint(paint, PAINT_INFO);

		XAxis x = binding.chart.getXAxis();
		x.setEnabled(true);
		x.setLabelCount(10, true);
		x.setDrawAxisLine(false);
		x.setDrawGridLines(true);
		x.setTextColor(Color.BLACK);
		x.setTextSize(16f);
		x.setPosition(XAxis.XAxisPosition.BOTTOM);
		x.setAxisLineColor(Color.BLACK);
		x.setValueFormatter(new ValueFormatter() {
			@Override
			public String getAxisLabel(float value, AxisBase axis) {
				//				SimpleDateFormat sdf = new SimpleDateFormat("MM/dd HH:mm", Locale.ROOT);
				return Float.toString(value);
			}
		});

		//		int limitHigh = 100, limitLow = 50;
		//		LimitLine high = new LimitLine((float) limitHigh, "Tachycardia");
		//		high.setLabelPosition(LimitLine.LimitLabelPosition.RIGHT_TOP);
		//		high.enableDashedLine(10f, 10f, 0f);
		//		high.setTextSize(16f);
		//		high.setLineColor(Color.RED);
		//		high.setLineWidth(2f);
		//		high.setTextColor(Color.BLACK);
		//
		//		LimitLine low = new LimitLine((float) limitLow, "Bradycardia");
		//		low.setLabelPosition(LimitLine.LimitLabelPosition.RIGHT_BOTTOM);
		//		low.enableDashedLine(10f, 10f, 0f);
		//		low.setTextSize(16f);
		//		low.setLineColor(Color.RED);
		//		low.setLineWidth(2f);
		//		low.setTextColor(Color.BLACK);

		YAxis left = binding.chart.getAxisLeft();
		left.setPosition(YAxis.YAxisLabelPosition.OUTSIDE_CHART);
		left.setDrawAxisLine(false);
		left.setDrawGridLines(false);
		left.setDrawLabels(true);
		left.setTextSize(16f);
		//		left.addLimitLine(high);
		//		left.addLimitLine(low);
		//TODO: change to data's mountain and valley
		left.setAxisMinimum(Float.parseFloat(Collections.min(((ArrayList)(obj))).toString()));
		left.setAxisMaximum(Float.parseFloat(Collections.max(((ArrayList)(obj))).toString()));

		binding.chart.getAxisRight().setEnabled(false);
		binding.chart.getLegend().setEnabled(false);

		boolean isEmpty;
		if(obj == null){
			binding.chart.setBackgroundColor(getResources().getColor(R.color.ap_gray, null));
			binding.chart.setData(null);
			binding.chart.notifyDataSetChanged();
			binding.chart.invalidate();
			isEmpty = true;
			return;
		}
		isEmpty = false;
		binding.chart.setBackgroundColor(getResources().getColor(R.color.white, null));
		LineDataSet set1;
		ArrayList<Entry> hr = new ArrayList<>();
		float j = 0;
		for(Object item : ((ArrayList)(obj)).toArray()) {
			//			isOverMin = isOverMin || item.getHr() < limitLow;
			//			isOverMax = isOverMax || item.getHr() > limitHigh;

			//			float f = ;
			//			Entry e = ;
			hr.add(new Entry(j, ((float)(short)(item))));
			j+=0.004;
		}



		if (binding.chart.getData() != null && binding.chart.getData().getDataSetCount() > 0) {
			set1 = (LineDataSet) binding.chart.getData().getDataSetByIndex(0);
			set1.setValues(hr);
			binding.chart.getData().notifyDataChanged();
			binding.chart.notifyDataSetChanged();
		}
		else {
			set1 = new LineDataSet(hr, "Heart Rate [bpm]");
			set1.setDrawFilled(false);
			set1.setDrawCircles(false);
			set1.setDrawCircleHole(false);
			set1.setDrawValues(false);
			set1.setAxisDependency(YAxis.AxisDependency.LEFT);
			set1.setColor(getResources().getColor(R.color.nordicRed, null));
			set1.setCircleColor(getResources().getColor(R.color.nordicRed, null));
			set1.setLineWidth(2f);
			LineData lineData = new LineData(set1);
			binding.chart.setData(lineData);
		}
		binding.chart.invalidate();
	}
}
