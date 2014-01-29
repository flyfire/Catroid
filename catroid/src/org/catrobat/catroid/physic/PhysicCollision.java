/**
 *  Catroid: An on-device visual programming system for Android devices
 *  Copyright (C) 2010-2013 The Catrobat Team
 *  (<http://developer.catrobat.org/credits>)
 *  
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as
 *  published by the Free Software Foundation, either version 3 of the
 *  License, or (at your option) any later version.
 *  
 *  An additional term exception under section 7 of the GNU Affero
 *  General Public License, version 3, is available at
 *  http://developer.catrobat.org/license_additional_term
 *  
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU Affero General Public License for more details.
 *  
 *  You should have received a copy of the GNU Affero General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.catrobat.catroid.physic;

import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.Contact;
import com.badlogic.gdx.physics.box2d.ContactImpulse;
import com.badlogic.gdx.physics.box2d.ContactListener;
import com.badlogic.gdx.physics.box2d.Manifold;

import org.catrobat.catroid.content.Sprite;

public class PhysicCollision implements ContactListener {
	@SuppressWarnings("unused")
	private static final String TAG = PhysicCollision.class.getSimpleName();
	PhysicsWorld mPhysicWorld;

	public PhysicCollision(PhysicsWorld physicWorld) {
		mPhysicWorld = physicWorld;
	}

	@Override
	public void beginContact(Contact contact) {
		// Log.d(TAG, "beginContact");

		Body a = contact.getFixtureA().getBody();
		Body b = contact.getFixtureB().getBody();

		if (a.getUserData() instanceof Sprite) {
			mPhysicWorld.bounced((Sprite) a.getUserData());
			//Log.d(TAG, "SPRITE A");
		}
		if (b.getUserData() instanceof Sprite) {
			mPhysicWorld.bounced((Sprite) b.getUserData());
			//Log.d(TAG, "SPRITE B");
		}
	}

	@Override
	public void endContact(Contact contact) {
		//Log.d(TAG, "endContact");
	}

	@Override
	public void preSolve(Contact contact, Manifold oldManifold) {
		//Log.d(TAG, "preSolve");
	}

	@Override
	public void postSolve(Contact contact, ContactImpulse impulse) {
		//Log.d(TAG, "postSolve");
	}

}