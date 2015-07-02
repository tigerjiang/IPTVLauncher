package com.android.launcher.home;

import android.app.Application;

import com.hisense.network.utils.EpgDataInfoLoader.HiLauncherLoader;

public class Launcher extends Application {
	public HiLauncherLoader mModel;
	
	@Override
	public void onCreate() {
		super.onCreate();
		mModel = new HiLauncherLoader(this);
	}

	/**
	 * There's no guarantee that this function is ever called.
	 */
	@Override
	public void onTerminate() {
		super.onTerminate();
	}

	public HiLauncherLoader setLauncher(Home launcher) {
		mModel.initialize(launcher);
		return mModel;
	}

	HiLauncherLoader getModel() {
		return mModel;
	}
}

