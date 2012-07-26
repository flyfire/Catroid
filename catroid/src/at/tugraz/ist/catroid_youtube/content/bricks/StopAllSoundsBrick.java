/**
 *  Catroid: An on-device graphical programming language for Android devices
 *  Copyright (C) 2010-2011 The Catroid Team
 *  (<http://code.google.com/p/catroid/wiki/Credits>)
 *  
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as
 *  published by the Free Software Foundation, either version 3 of the
 *  License, or (at your option) any later version.
 *  
 *  An additional term exception under section 7 of the GNU Affero
 *  General Public License, version 3, is available at
 *  http://www.catroid.org/catroid_license_additional_term
 *  
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Affero General Public License for more details.
 *   
 *  You should have received a copy of the GNU Affero General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package at.tugraz.ist.catroid_youtube.content.bricks;

import android.content.Context;
import android.view.View;
import android.widget.BaseAdapter;
import at.tugraz.ist.catroid_youtube.ProjectManager;
import at.tugraz.ist.catroid_youtube.R;
import at.tugraz.ist.catroid_youtube.common.SoundInfo;
import at.tugraz.ist.catroid_youtube.content.Sprite;
import at.tugraz.ist.catroid_youtube.io.SoundManager;
import at.tugraz.ist.catroid_youtube.stage.StageRecorder;

import com.thoughtworks.xstream.annotations.XStreamOmitField;

public class StopAllSoundsBrick implements Brick {
	private static final long serialVersionUID = 1L;

	private Sprite sprite;

	@XStreamOmitField
	private transient View view;

	public StopAllSoundsBrick(Sprite sprite) {
		this.sprite = sprite;
	}

	public int getRequiredResources() {
		return NO_RESOURCES;
	}

	public void execute() {
		SoundManager.getInstance().pause();
		ProjectManager projectManager = ProjectManager.getInstance();
		StageRecorder recorder = StageRecorder.getInstance();
		for (Sprite sprite : projectManager.getCurrentProject().getSpriteList()) {
			for (SoundInfo soundInfo : sprite.getSoundList()) {
				recorder.recordSound(soundInfo);
			}
		}
	}

	public Sprite getSprite() {
		return this.sprite;
	}

	public View getView(Context context, int brickId, BaseAdapter adapter) {
		if (view == null) {
			view = View.inflate(context, R.layout.brick_stop_all_sounds, null);
		}
		return view;
	}

	@Override
	public Brick clone() {
		return new StopAllSoundsBrick(getSprite());
	}

	public View getPrototypeView(Context context) {
		return View.inflate(context, R.layout.brick_stop_all_sounds, null);
	}
}