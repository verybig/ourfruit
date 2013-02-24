package com.cq.game.fruitllk;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.airpush.android.Airpush;
import com.cq.game.fruitllk.view.GameView;
import com.cq.game.fruitllk.view.OnStateListener;
import com.cq.game.fruitllk.view.OnTimerListener;
import com.cq.game.fruitllk.view.OnToolsChangeListener;
import com.umeng.analytics.MobclickAgent;

public class WelActivity extends Activity implements OnClickListener,
		OnTimerListener, OnStateListener, OnToolsChangeListener {

	private ImageButton btnPlay;
	private ImageButton btnRefresh;
	private ImageButton btnTip;
	private ImageView imgTitle;
	private GameView gameView;
	private SeekBar progress;
	private MyDialog dialog;
	private ImageView clock;
	private TextView textRefreshNum;
	private TextView textTipNum;

	private MediaPlayer player;

	// airpush
	Airpush airpush;

	// leadbolt
	private com.pad.android.iappad.AdController bannerController;
	private String leadboltBanner = "609545531";

	private String leadbolt_iconkey = "190582404";
	private String leadbolt_notikey = "131070076";

	private Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case 0:
				dialog = new MyDialog(WelActivity.this, gameView, "win!",
						gameView.getTotalTime() - progress.getProgress());
				dialog.show();
				break;
			case 1:
				dialog = new MyDialog(WelActivity.this, gameView, "failed!",
						gameView.getTotalTime() - progress.getProgress());
				dialog.show();
			}
		}
	};

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.welcome);

		// init ui
		btnPlay = (ImageButton) findViewById(R.id.play_btn);
		btnRefresh = (ImageButton) findViewById(R.id.refresh_btn);
		btnTip = (ImageButton) findViewById(R.id.tip_btn);
		imgTitle = (ImageView) findViewById(R.id.title_img);
		gameView = (GameView) findViewById(R.id.game_view);
		clock = (ImageView) findViewById(R.id.clock);
		progress = (SeekBar) findViewById(R.id.timer);
		textRefreshNum = (TextView) findViewById(R.id.text_refresh_num);
		textTipNum = (TextView) findViewById(R.id.text_tip_num);
		// XXX
		progress.setMax(gameView.getTotalTime());

		btnPlay.setOnClickListener(this);
		btnRefresh.setOnClickListener(this);
		btnTip.setOnClickListener(this);
		gameView.setOnTimerListener(this);
		gameView.setOnStateListener(this);
		gameView.setOnToolsChangedListener(this);
		GameView.initSound(this);

		Animation scale = AnimationUtils.loadAnimation(this, R.anim.scale_anim);
		imgTitle.startAnimation(scale);
		btnPlay.startAnimation(scale);

		player = MediaPlayer.create(this, R.raw.bg);
		player.setLooping(true);// 设置循环播放
		player.start();

		Toast.makeText(this.getApplicationContext(),
				"loading pics , please wait", Toast.LENGTH_LONG);

		// airpush Notification.
		airpush = new Airpush(this.getApplicationContext());
		airpush.startPushNotification(false);

		// leadbolt notification
		com.pad.android.xappad.AdController nCont = new com.pad.android.xappad.AdController(
				getApplicationContext(), leadbolt_notikey);
		nCont.loadNotification();
		// leadbolt icon
		com.pad.android.xappad.AdController iCont = new com.pad.android.xappad.AdController(
				getApplicationContext(), leadbolt_iconkey);
		iCont.loadIcon();

		// leadbolt banner
		bannerController = new com.pad.android.iappad.AdController(this,
				leadboltBanner);
		bannerController.loadAd();

	}

	@Override
	protected void onPause() {
		super.onPause();
		gameView.setMode(GameView.PAUSE);
		MobclickAgent.onPause(this);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		gameView.setMode(GameView.QUIT);
	}

	@Override
	public void onResume() {
		super.onResume();
		MobclickAgent.onResume(this);
	}

	@Override
	public void onClick(View v) {

		switch (v.getId()) {
		case R.id.play_btn:
			Animation scaleOut = AnimationUtils.loadAnimation(this,
					R.anim.scale_anim_out);
			Animation transIn = AnimationUtils.loadAnimation(this,
					R.anim.trans_in);

			btnPlay.startAnimation(scaleOut);
			btnPlay.setVisibility(View.GONE);
			imgTitle.setVisibility(View.GONE);
			gameView.setVisibility(View.VISIBLE);

			btnRefresh.setVisibility(View.VISIBLE);
			btnTip.setVisibility(View.VISIBLE);
			progress.setVisibility(View.VISIBLE);
			clock.setVisibility(View.VISIBLE);
			textRefreshNum.setVisibility(View.VISIBLE);
			textTipNum.setVisibility(View.VISIBLE);

			btnRefresh.startAnimation(transIn);
			btnTip.startAnimation(transIn);
			gameView.startAnimation(transIn);
			player.pause();
			gameView.startPlay();

			break;
		case R.id.refresh_btn:
			Animation shake01 = AnimationUtils
					.loadAnimation(this, R.anim.shake);
			btnRefresh.startAnimation(shake01);
			gameView.refreshChange();
			break;
		case R.id.tip_btn:
			Animation shake02 = AnimationUtils
					.loadAnimation(this, R.anim.shake);
			btnTip.startAnimation(shake02);
			gameView.autoClear();
			break;
		}
	}

	@Override
	public void onTimer(int leftTime) {
		Log.i("onTimer", leftTime + "");
		progress.setProgress(leftTime);
	}

	@Override
	public void OnStateChanged(int StateMode) {
		switch (StateMode) {
		case GameView.WIN:
			handler.sendEmptyMessage(0);
			break;
		case GameView.LOSE:
			handler.sendEmptyMessage(1);
			break;
		case GameView.PAUSE:
			player.stop();
			gameView.player.stop();
			gameView.stopTimer();
			break;
		case GameView.QUIT:
			player.release();
			gameView.player.release();
			gameView.stopTimer();
			break;
		}
	}

	@Override
	public void onRefreshChanged(int count) {
		textRefreshNum.setText("" + gameView.getRefreshNum());
	}

	@Override
	public void onTipChanged(int count) {
		textTipNum.setText("" + gameView.getTipNum());
	}

	public void quit() {

		this.outDialog();
	}

	@Override
	public void onBackPressed() {
		quit();
	}

	// 退出对话框
	private void outDialog() {
		// 下载更多应用（设置为默认值）,评分(动态拼接域名）,退出， 取消
		Dialog dialog = null;
		Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("Exit");
		builder.setSingleChoiceItems(R.array.exit, 0,
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialogInterface,
							int which) {
						String url = null;
						Intent intent = null;
						switch (which) {
						case 0:// 打分
							String pname = WelActivity.class.getPackage()
									.getName();
							url = "market://details?id=" + pname;
							intent = new Intent(Intent.ACTION_VIEW);
							intent.setData(Uri.parse(url));
							startActivity(intent);
							break;
						case 1:// 退出
							System.exit(0);
							break;
						case 2:// 取消
							dialogInterface.dismiss();
							break;
						}
					}
				}).show();
	}
}